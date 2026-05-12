package com.nonnas.nfeimporter.application;

import com.nonnas.catalog.application.fornecedor.CriarFornecedorUseCase;
import com.nonnas.catalog.application.insumo.CriarInsumoUseCase;
import com.nonnas.catalog.application.ports.FornecedorRepository;
import com.nonnas.catalog.application.ports.InsumoRepository;
import com.nonnas.catalog.domain.Cnpj;
import com.nonnas.catalog.domain.Fornecedor;
import com.nonnas.catalog.domain.FornecedorId;
import com.nonnas.catalog.domain.Insumo;
import com.nonnas.catalog.domain.InsumoId;
import com.nonnas.operations.application.notafiscal.LancarNotaFiscalUseCase;
import com.nonnas.operations.application.ports.FornecedorInsumoDeParaRepository;
import com.nonnas.operations.domain.NotaFiscal;
import com.nonnas.sharedkernel.NotFoundException;
import com.nonnas.sharedkernel.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orquestrador do lançamento de nota fiscal: aceita um payload onde
 * fornecedor e insumos podem vir como id existente OU como descritor para
 * criação. Resolve cada referência consultando catalog (criando quando
 * necessário) e delega o lançamento + entrada de estoque para o
 * {@link LancarNotaFiscalUseCase} de operations.
 *
 * <p>Insumos novos nascem com categoria fixa "A classificar"
 * ({@link #CATEGORIA_A_CLASSIFICAR_ID}, seed em catalog/V015) e
 * {@code controla_lote=true}/{@code controla_validade=true} (decisões 2a/3a do T20).
 */
@Service
public class ProcessarNotaFiscalUseCase {

    /** UUID determinístico semeado por {@code catalog/V015__seed_categoria_a_classificar.sql}. */
    public static final UUID CATEGORIA_A_CLASSIFICAR_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final FornecedorRepository fornecedorRepo;
    private final CriarFornecedorUseCase criarFornecedor;
    private final InsumoRepository insumoRepo;
    private final CriarInsumoUseCase criarInsumo;
    private final FornecedorInsumoDeParaRepository deParaRepo;
    private final LancarNotaFiscalUseCase lancar;

    public ProcessarNotaFiscalUseCase(FornecedorRepository fornecedorRepo,
                                      CriarFornecedorUseCase criarFornecedor,
                                      InsumoRepository insumoRepo,
                                      CriarInsumoUseCase criarInsumo,
                                      FornecedorInsumoDeParaRepository deParaRepo,
                                      LancarNotaFiscalUseCase lancar) {
        this.fornecedorRepo = fornecedorRepo;
        this.criarFornecedor = criarFornecedor;
        this.insumoRepo = insumoRepo;
        this.criarInsumo = criarInsumo;
        this.deParaRepo = deParaRepo;
        this.lancar = lancar;
    }

    @Transactional
    public NotaFiscal execute(Comando cmd) {
        if (cmd == null || cmd.itens == null || cmd.itens.isEmpty()) {
            throw new ValidationException("Nota fiscal deve ter ao menos um item");
        }

        UUID fornecedorId = resolverFornecedor(cmd.fornecedor);

        List<LancarNotaFiscalUseCase.Item> itensResolvidos = new ArrayList<>(cmd.itens.size());
        for (ItemEntrada item : cmd.itens) {
            Insumo insumo = resolverInsumoEntidade(item.insumo, fornecedorId, item.codigoFornecedor);
            BigDecimal valorTotal = item.valorTotal != null
                    ? item.valorTotal
                    : item.valorUnitario.multiply(item.quantidade);
            itensResolvidos.add(new LancarNotaFiscalUseCase.Item(
                    insumo.id().value(), item.codigoFornecedor, item.descricaoOrigem,
                    item.quantidade, item.unidadeMedidaId,
                    item.valorUnitario, valorTotal,
                    item.lote, item.dataValidade,
                    insumo.controlaValidade()));
        }

        var lancamento = new LancarNotaFiscalUseCase.Comando(
                cmd.filialId, cmd.usuarioId, fornecedorId,
                cmd.numero, cmd.serie, cmd.chaveNfe, cmd.dataEmissao,
                cmd.valorTotal, cmd.observacao, itensResolvidos);
        return lancar.execute(lancamento);
    }

    private UUID resolverFornecedor(FornecedorEntrada f) {
        if (f == null) {
            throw new ValidationException("Fornecedor obrigatório");
        }
        if (f.id != null) {
            FornecedorId fid = FornecedorId.of(f.id);
            if (fornecedorRepo.findById(fid).isEmpty()) {
                throw new NotFoundException("Fornecedor", f.id);
            }
            return f.id;
        }
        if (f.cnpj == null || f.razaoSocial == null || f.razaoSocial.isBlank()) {
            throw new ValidationException("Fornecedor: id ou (cnpj + razaoSocial) obrigatório");
        }
        Optional<Fornecedor> existente = fornecedorRepo.findByCnpj(Cnpj.of(f.cnpj));
        if (existente.isPresent()) {
            return existente.get().id().value();
        }
        Fornecedor novo = criarFornecedor.execute(f.razaoSocial, f.cnpj);
        return novo.id().value();
    }

    /**
     * Resolve o insumo para o item da nota fiscal. Ordem de precedência:
     * <ol>
     *   <li>{@code i.id} explícito (operador vinculou manualmente);</li>
     *   <li>de-para {@code (fornecedorId, cProd)} — aprendizado de cargas anteriores;</li>
     *   <li>criação de insumo novo com código único (sufixa com CNPJ se cProd colide com insumo existente).</li>
     * </ol>
     * <p>Importante: <strong>nunca</strong> reusar insumo só porque o {@code i.codigo}
     * (que é o cProd local do fornecedor) bate com o código global de outro insumo —
     * fornecedores diferentes podem usar o mesmo cProd para produtos distintos.
     */
    private Insumo resolverInsumoEntidade(InsumoEntrada i, UUID fornecedorId, String cProd) {
        if (i == null) {
            throw new ValidationException("Insumo obrigatório no item");
        }
        if (i.id != null) {
            InsumoId iid = InsumoId.of(i.id);
            return insumoRepo.findById(iid)
                    .orElseThrow(() -> new NotFoundException("Insumo", i.id));
        }
        String chaveFornecedor = cProd != null && !cProd.isBlank() ? cProd
                : (i.codigo != null && !i.codigo.isBlank() ? i.codigo : null);
        if (chaveFornecedor != null) {
            var depara = deParaRepo.findByFornecedorAndCodigo(fornecedorId, chaveFornecedor);
            if (depara.isPresent()) {
                UUID insumoId = depara.get().insumoId();
                return insumoRepo.findById(InsumoId.of(insumoId))
                        .orElseThrow(() -> new NotFoundException("Insumo (via de-para)", insumoId));
            }
        }
        if (i.codigo == null || i.codigo.isBlank() || i.nome == null || i.nome.isBlank()
                || i.unidadeBaseId == null) {
            throw new ValidationException(
                    "Insumo: id ou (codigo + nome + unidadeBaseId) obrigatório");
        }
        String codigoFinal = codigoUnicoParaInsumoNovo(i.codigo, fornecedorId);
        return criarInsumo.execute(codigoFinal, i.nome,
                CATEGORIA_A_CLASSIFICAR_ID, i.unidadeBaseId,
                true, true);
    }

    private String codigoUnicoParaInsumoNovo(String cProdSugerido, UUID fornecedorId) {
        if (insumoRepo.findByCodigo(cProdSugerido).isEmpty()) {
            return cProdSugerido;
        }
        // Colisão: o cProd que o operador trouxe já é código global de outro insumo
        // (provavelmente de um fornecedor diferente). Sufixa pra garantir unicidade
        // sem perder a referência ao fornecedor original.
        String cnpjShort = fornecedorRepo.findById(FornecedorId.of(fornecedorId))
                .map(f -> f.cnpj().value().substring(0, 8))
                .orElse("UNK");
        String candidato = cProdSugerido + "-" + cnpjShort;
        if (insumoRepo.findByCodigo(candidato).isPresent()) {
            candidato = candidato + "-" + System.currentTimeMillis();
        }
        return candidato;
    }

    public record Comando(
            UUID filialId,
            UUID usuarioId,
            FornecedorEntrada fornecedor,
            String numero,
            String serie,
            String chaveNfe,
            Instant dataEmissao,
            BigDecimal valorTotal,
            String observacao,
            List<ItemEntrada> itens
    ) {}

    /** Fornecedor resolvido por id existente OU descritor para criação. */
    public record FornecedorEntrada(UUID id, String cnpj, String razaoSocial) {
        public static FornecedorEntrada existente(UUID id) {
            return new FornecedorEntrada(id, null, null);
        }
        public static FornecedorEntrada novo(String cnpj, String razaoSocial) {
            return new FornecedorEntrada(null, cnpj, razaoSocial);
        }
    }

    /** Insumo resolvido por id existente OU descritor para criação. */
    public record InsumoEntrada(UUID id, String codigo, String nome, UUID unidadeBaseId) {
        public static InsumoEntrada existente(UUID id) {
            return new InsumoEntrada(id, null, null, null);
        }
        public static InsumoEntrada novo(String codigo, String nome, UUID unidadeBaseId) {
            return new InsumoEntrada(null, codigo, nome, unidadeBaseId);
        }
    }

    public record ItemEntrada(
            InsumoEntrada insumo,
            String codigoFornecedor,
            String descricaoOrigem,
            BigDecimal quantidade,
            UUID unidadeMedidaId,
            BigDecimal valorUnitario,
            BigDecimal valorTotal,
            String lote,
            LocalDate dataValidade
    ) {}
}
