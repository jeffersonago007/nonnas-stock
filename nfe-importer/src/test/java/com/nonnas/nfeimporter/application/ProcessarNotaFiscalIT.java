package com.nonnas.nfeimporter.application;

import com.nonnas.catalog.application.ports.FornecedorRepository;
import com.nonnas.catalog.application.ports.InsumoRepository;
import com.nonnas.catalog.application.ports.UnidadeMedidaRepository;
import com.nonnas.catalog.domain.Cnpj;
import com.nonnas.catalog.domain.Insumo;
import com.nonnas.catalog.domain.UnidadeMedida;
import com.nonnas.nfeimporter.testsupport.AbstractNfeImporterIntegrationTest;
import com.nonnas.operations.application.ports.FornecedorInsumoDeParaRepository;
import com.nonnas.operations.application.ports.NotaFiscalRepository;
import com.nonnas.operations.domain.NotaFiscal;
import com.nonnas.sharedkernel.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessarNotaFiscalIT extends AbstractNfeImporterIntegrationTest {

    private static final String CNPJ_FORNECEDOR = "11444777000161";
    private static final String CNPJ_REENTRADA = "11222333000181";
    private static final String CNPJ_APRENDIZADO = "06990590000123";
    // 44 dígitos exatos (chk_notas_fiscais_chave_44digitos).
    private static final String CHAVE_1 = "35260511444777000161550010000010011234567890";
    private static final String CHAVE_REENTRADA = "35260511222333000181550010000010021234567890";
    private static final String CHAVE_APRENDIZADO_1 = "35260506990590000123550010000010031234567890";
    private static final String CHAVE_APRENDIZADO_2 = "35260506990590000123550010000010041234567890";

    @Autowired
    private ProcessarNotaFiscalUseCase processar;

    @Autowired
    private FornecedorRepository fornecedorRepo;

    @Autowired
    private InsumoRepository insumoRepo;

    @Autowired
    private UnidadeMedidaRepository unidadeRepo;

    @Autowired
    private NotaFiscalRepository notaRepo;

    @Autowired
    private FornecedorInsumoDeParaRepository deParaRepo;

    @Test
    void lancaNotaCriandoFornecedorEInsumoNovos() {
        UUID kgId = unidadePorCodigo("KG").id().value();
        UUID filialId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();

        var cmd = new ProcessarNotaFiscalUseCase.Comando(
                filialId, usuarioId,
                ProcessarNotaFiscalUseCase.FornecedorEntrada.novo(
                        CNPJ_FORNECEDOR, "Atacado Pizzaria SA"),
                "1001", "1",
                CHAVE_1,
                Instant.parse("2026-05-10T13:00:00Z"),
                new BigDecimal("1200.00"),
                "Primeira nota do fornecedor",
                List.of(new ProcessarNotaFiscalUseCase.ItemEntrada(
                        ProcessarNotaFiscalUseCase.InsumoEntrada.novo(
                                "MUS-CX-5KG", "Mussarela em caixa 5kg", kgId),
                        "MUS-CX-5KG",
                        "MUSSARELA EM CAIXA 5KG",
                        new BigDecimal("10.0000"),
                        kgId,
                        new BigDecimal("120.0000"),
                        new BigDecimal("1200.00"),
                        null, null)));

        NotaFiscal nf = processar.execute(cmd);

        // 1) Fornecedor criado com razão em UPPERCASE.
        var fornecedor = fornecedorRepo.findByCnpj(Cnpj.of(CNPJ_FORNECEDOR)).orElseThrow();
        assertThat(fornecedor.razaoSocial()).isEqualTo("ATACADO PIZZARIA SA");

        // 2) Insumo criado com defaults (categoria A classificar + lote+validade true).
        Insumo insumo = insumoRepo.findByCodigo("MUS-CX-5KG").orElseThrow();
        assertThat(insumo.nome()).isEqualTo("MUSSARELA EM CAIXA 5KG");
        assertThat(insumo.categoriaId().value())
                .isEqualTo(ProcessarNotaFiscalUseCase.CATEGORIA_A_CLASSIFICAR_ID);
        assertThat(insumo.controlaLote()).isTrue();
        assertThat(insumo.controlaValidade()).isTrue();

        // 3) Nota persistida com 1 item, ligada à movimentação de entrada.
        NotaFiscal salva = notaRepo.findById(nf.id()).orElseThrow();
        assertThat(salva.fornecedorId()).isEqualTo(fornecedor.id().value());
        assertThat(salva.itens()).hasSize(1);
        assertThat(salva.movimentacaoEntradaId()).isNotNull();

        // 4) De-para foi aprendido para esse (fornecedor, cProd).
        var depara = deParaRepo.findByFornecedorAndCodigo(fornecedor.id().value(), "MUS-CX-5KG");
        assertThat(depara).isPresent();
        assertThat(depara.get().insumoId()).isEqualTo(insumo.id().value());
    }

    @Test
    void rejeitaReentradaPorChaveJaLancada() {
        UUID kgId = unidadePorCodigo("KG").id().value();
        UUID filialId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();

        processar.execute(comandoBasico(filialId, usuarioId, CHAVE_REENTRADA, kgId,
                CNPJ_REENTRADA, "Distribuidor Reentrada", "REE-001"));

        // Segunda chamada com a mesma chave → 409 / BusinessRule.
        assertThatThrownBy(() -> processar.execute(comandoBasico(
                filialId, usuarioId, CHAVE_REENTRADA, kgId,
                CNPJ_REENTRADA, "Distribuidor Reentrada", "REE-001")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("já lançada");
    }

    @Test
    void notaComCodigoQueColideComInsumoGlobalDeOutroForncedorCriaNovoInsumoSemTocarOAntigo() {
        UUID kgId = unidadePorCodigo("KG").id().value();
        UUID filialId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        // CNPJs reais (válidos) de fornecedores diferentes; chaves de NF-e inéditas neste test.
        String cnpjA = "60892858001101";
        String cnpjB = "54910325000130";
        String chaveA = "35260560892858001101550010000010081234567890";
        String chaveB = "35260554910325000130550010000010091234567890";

        // Cadastra um insumo com codigo "001" via uma 1ª nota do fornecedor A.
        processar.execute(comandoBasico(filialId, usuarioId,
                chaveA, kgId, cnpjA, "Fornecedor Mussarela SA", "001"));
        Insumo mussarela = insumoRepo.findByCodigo("001").orElseThrow();
        UUID mussarelaId = mussarela.id().value();

        // Fornecedor DIFERENTE manda nota com cProd "001" mas xProd "Produto de Teste".
        // Comportamento desejado: cria um insumo NOVO (código sufixado), NÃO toca na mussarela.
        var cmd = new ProcessarNotaFiscalUseCase.Comando(
                filialId, usuarioId,
                ProcessarNotaFiscalUseCase.FornecedorEntrada.novo(cnpjB, "Outro Fornecedor LTDA"),
                "2002", "1", chaveB,
                Instant.parse("2026-05-12T10:00:00Z"),
                new BigDecimal("50.00"), null,
                List.of(new ProcessarNotaFiscalUseCase.ItemEntrada(
                        ProcessarNotaFiscalUseCase.InsumoEntrada.novo(
                                "001", "PRODUTO DE TESTE", kgId),
                        "001",
                        "PRODUTO DE TESTE",
                        new BigDecimal("1.0000"),
                        kgId,
                        new BigDecimal("50.0000"),
                        new BigDecimal("50.00"),
                        null, null)));

        processar.execute(cmd);

        // Mussarela continua intacta — mesmo id, mesmo nome.
        Insumo mussarelaApos = insumoRepo.findById(mussarela.id()).orElseThrow();
        assertThat(mussarelaApos.nome()).isEqualTo(mussarela.nome());
        assertThat(mussarelaApos.id().value()).isEqualTo(mussarelaId);

        // Novo insumo foi criado com código sufixado pelo CNPJ do fornecedor, NÃO "001".
        // Procura via de-para pra recuperar o insumoId associado.
        UUID fornecedorOutroId = fornecedorRepo.findByCnpj(Cnpj.of(cnpjB))
                .orElseThrow().id().value();
        var depara = deParaRepo.findByFornecedorAndCodigo(fornecedorOutroId, "001").orElseThrow();
        Insumo novoInsumo = insumoRepo.findById(
                com.nonnas.catalog.domain.InsumoId.of(depara.insumoId())).orElseThrow();
        assertThat(novoInsumo.id().value()).isNotEqualTo(mussarelaId);
        assertThat(novoInsumo.codigo()).startsWith("001-");
        assertThat(novoInsumo.nome()).isEqualTo("PRODUTO DE TESTE");
    }

    @Test
    void segundaNotaDoMesmoFornecedorComMesmoCodigoReusaInsumoExistente() {
        UUID kgId = unidadePorCodigo("KG").id().value();
        UUID filialId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();

        // 1ª nota cria o insumo.
        processar.execute(comandoBasico(filialId, usuarioId, CHAVE_APRENDIZADO_1, kgId,
                CNPJ_APRENDIZADO, "Atacado Aprendizado", "FAR-001"));
        UUID insumoId = insumoRepo.findByCodigo("FAR-001").orElseThrow().id().value();

        // 2ª nota traz o mesmo cProd — orquestrador encontra pelo código e reaproveita.
        processar.execute(comandoBasico(filialId, usuarioId, CHAVE_APRENDIZADO_2, kgId,
                CNPJ_APRENDIZADO, "Atacado Aprendizado", "FAR-001"));

        Optional<Insumo> insumo = insumoRepo.findByCodigo("FAR-001");
        assertThat(insumo).isPresent();
        assertThat(insumo.get().id().value()).isEqualTo(insumoId);

        // De-para continua presente.
        var fornecedorId = fornecedorRepo.findByCnpj(Cnpj.of(CNPJ_APRENDIZADO))
                .orElseThrow().id().value();
        assertThat(deParaRepo.findByFornecedorAndCodigo(fornecedorId, "FAR-001")).isPresent();
    }

    private ProcessarNotaFiscalUseCase.Comando comandoBasico(
            UUID filialId, UUID usuarioId, String chave, UUID unidadeId,
            String cnpj, String razao, String codigoInsumo) {
        return new ProcessarNotaFiscalUseCase.Comando(
                filialId, usuarioId,
                ProcessarNotaFiscalUseCase.FornecedorEntrada.novo(cnpj, razao),
                "999", "1", chave,
                Instant.parse("2026-05-10T13:00:00Z"),
                new BigDecimal("100.00"), null,
                List.of(new ProcessarNotaFiscalUseCase.ItemEntrada(
                        ProcessarNotaFiscalUseCase.InsumoEntrada.novo(
                                codigoInsumo, "Item Teste " + codigoInsumo, unidadeId),
                        codigoInsumo,
                        "Item Teste " + codigoInsumo,
                        new BigDecimal("1.0000"),
                        unidadeId,
                        new BigDecimal("100.0000"),
                        new BigDecimal("100.00"),
                        null, null)));
    }

    private UnidadeMedida unidadePorCodigo(String codigo) {
        return unidadeRepo.findByCodigo(codigo)
                .orElseThrow(() -> new IllegalStateException("Unidade " + codigo + " ausente do seed"));
    }
}
