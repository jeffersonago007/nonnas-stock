package com.nonnas.nfeimporter.parser;

import com.nonnas.nfeimporter.domain.EmitenteLido;
import com.nonnas.nfeimporter.domain.ItemLido;
import com.nonnas.nfeimporter.domain.NotaFiscalLida;
import com.nonnas.sharedkernel.ValidationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser de NF-e modelo 55 (XML SEFAZ). Usa DOM + lookup por nome local
 * ignorando namespace para lidar com variações de geradores (com/sem
 * wrapper {@code nfeProc}, com/sem prefixo de namespace).
 *
 * <p>Não valida assinatura digital nem chama SEFAZ — escopo é só extrair
 * os dados estruturados. Validação de existência da nota junto à Receita
 * fica para uma fase futura.
 *
 * <p>Configurado com proteções OWASP contra XXE: doctype desabilitado,
 * external entities desabilitadas.
 */
public class XmlNfeParser {

    public NotaFiscalLida parse(InputStream xml) {
        if (xml == null) {
            throw new ValidationException("XML da nota fiscal é obrigatório");
        }
        return parse(new InputSource(xml));
    }

    public NotaFiscalLida parse(Reader xml) {
        if (xml == null) {
            throw new ValidationException("XML da nota fiscal é obrigatório");
        }
        return parse(new InputSource(xml));
    }

    private NotaFiscalLida parse(InputSource source) {
        Document doc = readDocument(source);
        Element infNFe = requireElement(doc.getDocumentElement(), "infNFe",
                "Elemento <infNFe> não encontrado — XML não parece uma NF-e modelo 55");

        String chave = extrairChave(infNFe);
        Element ide = requireChild(infNFe, "ide", "Elemento <ide> obrigatório");
        Element emit = requireChild(infNFe, "emit", "Elemento <emit> obrigatório");
        Element total = requireChild(infNFe, "total", "Elemento <total> obrigatório");

        String numero = textOrThrow(ide, "nNF", "Número da NF (nNF) obrigatório");
        String serie = textOrEmpty(ide, "serie");
        OffsetDateTime dataEmissao = parseDataEmissao(ide);
        BigDecimal valorTotal = parseValorTotal(total);

        EmitenteLido emitente = parseEmitente(emit);
        List<ItemLido> itens = parseItens(infNFe);

        return new NotaFiscalLida(chave, numero, serie, dataEmissao, valorTotal, emitente, itens);
    }

    private Document readDocument(InputSource source) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            // OWASP XXE protection
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(source);
        } catch (ParserConfigurationException ex) {
            throw new IllegalStateException("Falha ao configurar parser XML", ex);
        } catch (Exception ex) {
            throw new ValidationException("XML inválido: " + ex.getMessage());
        }
    }

    private String extrairChave(Element infNFe) {
        String idAttr = infNFe.getAttribute("Id");
        if (idAttr == null || idAttr.isBlank()) {
            throw new ValidationException("Atributo Id em <infNFe> obrigatório (chave de acesso)");
        }
        // O valor vem como "NFe35190821..." — chave de 44 dígitos prefixada.
        String semPrefixo = idAttr.startsWith("NFe") ? idAttr.substring(3) : idAttr;
        if (semPrefixo.length() != 44 || !semPrefixo.chars().allMatch(Character::isDigit)) {
            throw new ValidationException("Chave de acesso inválida (esperado 44 dígitos): " + semPrefixo);
        }
        return semPrefixo;
    }

    private OffsetDateTime parseDataEmissao(Element ide) {
        // NF-e 4.00 usa <dhEmi>. Versões antigas (3.10) usavam <dEmi> (date-only).
        String dhEmi = textOrEmpty(ide, "dhEmi");
        if (!dhEmi.isBlank()) {
            try {
                return OffsetDateTime.parse(dhEmi);
            } catch (DateTimeParseException ex) {
                throw new ValidationException("Data de emissão inválida: " + dhEmi);
            }
        }
        String dEmi = textOrEmpty(ide, "dEmi");
        if (!dEmi.isBlank()) {
            // Date-only — assume meia-noite UTC. Aceitação para retro-compat.
            try {
                return OffsetDateTime.parse(dEmi + "T00:00:00Z");
            } catch (DateTimeParseException ex) {
                throw new ValidationException("Data de emissão inválida: " + dEmi);
            }
        }
        throw new ValidationException("Data de emissão (dhEmi ou dEmi) obrigatória");
    }

    private BigDecimal parseValorTotal(Element total) {
        Element icmsTot = childOrNull(total, "ICMSTot");
        Element fonte = icmsTot != null ? icmsTot : total;
        String vNF = textOrEmpty(fonte, "vNF");
        if (vNF.isBlank()) {
            throw new ValidationException("Valor total (vNF) obrigatório");
        }
        return new BigDecimal(vNF);
    }

    private EmitenteLido parseEmitente(Element emit) {
        String cnpj = textOrEmpty(emit, "CNPJ");
        if (cnpj.isBlank()) {
            // Pode ser CPF (raro em NF-e modelo 55, mas existe). Sem suporte por ora.
            throw new ValidationException("Emitente sem CNPJ — apenas pessoa jurídica suportada");
        }
        String razao = textOrThrow(emit, "xNome", "Razão social do emitente (xNome) obrigatória");
        String fantasia = textOrEmpty(emit, "xFant");
        String ie = textOrEmpty(emit, "IE");
        return new EmitenteLido(cnpj, razao, fantasia.isBlank() ? null : fantasia, ie.isBlank() ? null : ie);
    }

    private List<ItemLido> parseItens(Element infNFe) {
        NodeList dets = infNFe.getElementsByTagNameNS("*", "det");
        if (dets.getLength() == 0) {
            throw new ValidationException("NF-e sem itens (<det>)");
        }
        List<ItemLido> itens = new ArrayList<>(dets.getLength());
        for (int i = 0; i < dets.getLength(); i++) {
            itens.add(parseItem((Element) dets.item(i)));
        }
        return List.copyOf(itens);
    }

    private ItemLido parseItem(Element det) {
        int numero = parseInt(det.getAttribute("nItem"), 1);
        Element prod = requireChild(det, "prod", "Item sem <prod>");

        String cProd = textOrThrow(prod, "cProd", "Item sem código (cProd)");
        String xProd = textOrThrow(prod, "xProd", "Item sem descrição (xProd)");
        String ncm = textOrEmpty(prod, "NCM");
        String uCom = textOrThrow(prod, "uCom", "Item sem unidade comercial (uCom)");
        BigDecimal qCom = new BigDecimal(textOrThrow(prod, "qCom", "Item sem quantidade (qCom)"));
        BigDecimal vUnCom = new BigDecimal(textOrThrow(prod, "vUnCom", "Item sem valor unitário (vUnCom)"));
        BigDecimal vProd = new BigDecimal(textOrThrow(prod, "vProd", "Item sem valor total (vProd)"));

        return new ItemLido(numero, cProd, xProd, ncm.isBlank() ? null : ncm, uCom, qCom, vUnCom, vProd);
    }

    // ---------- helpers ----------

    private static Element requireElement(Element root, String localName, String message) {
        if (root == null) {
            throw new ValidationException("XML vazio");
        }
        NodeList list = root.getElementsByTagNameNS("*", localName);
        if (list.getLength() == 0) {
            throw new ValidationException(message);
        }
        return (Element) list.item(0);
    }

    private static Element requireChild(Element parent, String localName, String message) {
        Element child = childOrNull(parent, localName);
        if (child == null) {
            throw new ValidationException(message);
        }
        return child;
    }

    private static Element childOrNull(Element parent, String localName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && localName.equals(node.getLocalName())) {
                return (Element) node;
            }
        }
        return null;
    }

    private static String textOrEmpty(Element parent, String localName) {
        Element child = childOrNull(parent, localName);
        if (child == null) return "";
        String text = child.getTextContent();
        return text == null ? "" : text.trim();
    }

    private static String textOrThrow(Element parent, String localName, String message) {
        String text = textOrEmpty(parent, localName);
        if (text.isBlank()) {
            throw new ValidationException(message);
        }
        return text;
    }

    private static int parseInt(String value, int fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
