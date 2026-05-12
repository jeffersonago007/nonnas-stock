package com.nonnas.nfeimporter.application;

import com.nonnas.catalog.application.ports.FornecedorRepository;
import com.nonnas.catalog.application.ports.InsumoRepository;
import com.nonnas.catalog.domain.Cnpj;
import com.nonnas.catalog.domain.Fornecedor;
import com.nonnas.catalog.domain.Insumo;
import com.nonnas.nfeimporter.domain.ItemLido;
import com.nonnas.nfeimporter.domain.NotaFiscalLida;
import com.nonnas.nfeimporter.parser.XmlNfeParser;
import com.nonnas.operations.application.ports.FornecedorInsumoDeParaRepository;
import com.nonnas.sharedkernel.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Parseia o XML da NF-e e enriquece cada item com sugestão de match
 * contra o catálogo:
 * <ul>
 *   <li>{@code MATCH_DEPARA}: já há aprendizado {@code (fornecedor, cProd)} → usar esse insumo.</li>
 *   <li>{@code COLISAO_CODIGO}: o cProd colide com código de algum insumo existente,
 *       mas sem de-para — operador decide se vincula ou cria novo.</li>
 *   <li>{@code LIVRE}: nenhum match, criar insumo novo é o caminho.</li>
 * </ul>
 * A decisão final fica com o operador na tela de lançamento.
 */
@Service
public class PreviewNotaFiscalUseCase {

    private final XmlNfeParser parser;
    private final FornecedorRepository fornecedorRepo;
    private final InsumoRepository insumoRepo;
    private final FornecedorInsumoDeParaRepository deParaRepo;

    public PreviewNotaFiscalUseCase(XmlNfeParser parser,
                                    FornecedorRepository fornecedorRepo,
                                    InsumoRepository insumoRepo,
                                    FornecedorInsumoDeParaRepository deParaRepo) {
        this.parser = parser;
        this.fornecedorRepo = fornecedorRepo;
        this.insumoRepo = insumoRepo;
        this.deParaRepo = deParaRepo;
    }

    @Transactional(readOnly = true)
    public Preview execute(InputStream xmlStream) {
        NotaFiscalLida nf;
        try {
            nf = parser.parse(xmlStream);
        } catch (RuntimeException e) {
            throw new ValidationException("Falha ao ler XML: " + e.getMessage());
        }

        Optional<Fornecedor> fornecedor = fornecedorRepo.findByCnpj(Cnpj.of(nf.emitente().cnpj()));

        List<ItemEnriquecido> itens = new ArrayList<>(nf.itens().size());
        for (ItemLido item : nf.itens()) {
            itens.add(enriquecer(item, fornecedor.orElse(null)));
        }
        return new Preview(nf, itens);
    }

    private ItemEnriquecido enriquecer(ItemLido item, Fornecedor fornecedor) {
        // 1. De-para já mapeado → sugestão forte
        if (fornecedor != null && item.codigoFornecedor() != null && !item.codigoFornecedor().isBlank()) {
            var dp = deParaRepo.findByFornecedorAndCodigo(fornecedor.id().value(), item.codigoFornecedor());
            if (dp.isPresent()) {
                Insumo sugerido = insumoRepo.findById(
                        com.nonnas.catalog.domain.InsumoId.of(dp.get().insumoId())).orElse(null);
                if (sugerido != null) {
                    return new ItemEnriquecido(item, MatchStatus.MATCH_DEPARA,
                            sugerido.id().value(), sugerido.codigo(), sugerido.nome());
                }
            }
        }

        // 2. cProd colide com código global de outro insumo → avisar
        if (item.codigoFornecedor() != null && !item.codigoFornecedor().isBlank()) {
            Optional<Insumo> colide = insumoRepo.findByCodigo(item.codigoFornecedor());
            if (colide.isPresent()) {
                Insumo c = colide.get();
                return new ItemEnriquecido(item, MatchStatus.COLISAO_CODIGO,
                        c.id().value(), c.codigo(), c.nome());
            }
        }

        // 3. Sem match — criar novo é o caminho
        return new ItemEnriquecido(item, MatchStatus.LIVRE, null, null, null);
    }

    public enum MatchStatus {
        MATCH_DEPARA,    // de-para (fornecedor, cProd) existente — pré-selecionar
        COLISAO_CODIGO,  // cProd colide com insumo global — operador decide
        LIVRE            // nenhum match — criar novo é o default
    }

    public record Preview(NotaFiscalLida nf, List<ItemEnriquecido> itens) {}

    public record ItemEnriquecido(
            ItemLido item,
            MatchStatus matchStatus,
            UUID insumoSugeridoId,
            String insumoSugeridoCodigo,
            String insumoSugeridoNome
    ) {}
}
