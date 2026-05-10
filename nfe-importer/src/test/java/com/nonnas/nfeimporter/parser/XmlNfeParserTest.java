package com.nonnas.nfeimporter.parser;

import com.nonnas.nfeimporter.domain.NotaFiscalLida;
import com.nonnas.sharedkernel.ValidationException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class XmlNfeParserTest {

    private final XmlNfeParser parser = new XmlNfeParser();

    @Test
    void parseiaNfeSimplesComUmItem() {
        NotaFiscalLida nf = parser.parse(fixture("nfe-simples.xml"));

        assertThat(nf.chaveAcesso()).isEqualTo("35260512345678000195550010000001231234567890");
        assertThat(nf.numero()).isEqualTo("123");
        assertThat(nf.serie()).isEqualTo("1");
        assertThat(nf.dataEmissao()).isEqualTo(OffsetDateTime.parse("2026-05-10T08:00:00-03:00"));
        assertThat(nf.valorTotal()).isEqualByComparingTo("1348.50");

        assertThat(nf.emitente().cnpj()).isEqualTo("12345678000195");
        assertThat(nf.emitente().razaoSocial()).isEqualTo("Distribuidora de Carnes Premium LTDA");
        assertThat(nf.emitente().nomeFantasia()).isEqualTo("Carnes Premium");
        assertThat(nf.emitente().inscricaoEstadual()).isEqualTo("123456789012");

        assertThat(nf.itens()).hasSize(1);
        var item = nf.itens().get(0);
        assertThat(item.numero()).isEqualTo(1);
        assertThat(item.codigoFornecedor()).isEqualTo("BOV-001");
        assertThat(item.descricao()).isEqualTo("Picanha bovina premium");
        assertThat(item.ncm()).isEqualTo("02013000");
        assertThat(item.unidadeComercial()).isEqualTo("KG");
        assertThat(item.quantidade()).isEqualByComparingTo("15.0000");
        assertThat(item.valorUnitario()).isEqualByComparingTo("89.9000");
        assertThat(item.valorTotal()).isEqualByComparingTo("1348.50");
    }

    @Test
    void parseiaNfeComMultiplosItensPreservandoOrdem() {
        NotaFiscalLida nf = parser.parse(fixture("nfe-multi-itens.xml"));

        assertThat(nf.chaveAcesso()).isEqualTo("35260598765432000180550010000004561234567891");
        assertThat(nf.emitente().cnpj()).isEqualTo("98765432000180");
        // Sem xFant — deve vir null, não string vazia
        assertThat(nf.emitente().nomeFantasia()).isNull();
        assertThat(nf.itens()).hasSize(3);

        assertThat(nf.itens().get(0).codigoFornecedor()).isEqualTo("MUS-CX-5KG");
        assertThat(nf.itens().get(0).quantidade()).isEqualByComparingTo("10.0000");
        assertThat(nf.itens().get(0).unidadeComercial()).isEqualTo("CX");

        assertThat(nf.itens().get(1).codigoFornecedor()).isEqualTo("FAR-25KG");
        assertThat(nf.itens().get(1).valorUnitario()).isEqualByComparingTo("85.5000");

        assertThat(nf.itens().get(2).codigoFornecedor()).isEqualTo("OLE-LT");
        assertThat(nf.itens().get(2).quantidade()).isEqualByComparingTo("48.0000");
    }

    @Test
    void parseiaNfeSemWrapperNfeProcEComCamposOpcionaisAusentes() {
        NotaFiscalLida nf = parser.parse(fixture("nfe-campos-minimos.xml"));

        assertThat(nf.chaveAcesso()).isEqualTo("35260511111111000111550010000007891234567892");
        assertThat(nf.serie()).isEqualTo("2");
        assertThat(nf.emitente().nomeFantasia()).isNull();
        assertThat(nf.emitente().inscricaoEstadual()).isNull();
        assertThat(nf.itens().get(0).ncm()).isNull();
    }

    @Test
    void rejeitaXmlSemElementoInfNFe() {
        InputStream xml = new ByteArrayInputStream(
                "<root><nada/></root>".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> parser.parse(xml))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("infNFe");
    }

    @Test
    void rejeitaXmlMalFormado() {
        InputStream xml = new ByteArrayInputStream(
                "<infNFe Id=\"x\"><ide>".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> parser.parse(xml))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("XML inválido");
    }

    @Test
    void rejeitaInfNFeSemAtributoIdComChaveDeAcesso() {
        String xml = """
                <NFe xmlns="http://www.portalfiscal.inf.br/nfe">
                  <infNFe versao="4.00">
                    <ide><nNF>1</nNF><serie>1</serie><dhEmi>2026-01-01T00:00:00-03:00</dhEmi></ide>
                    <emit><CNPJ>11111111000111</CNPJ><xNome>x</xNome></emit>
                    <det nItem="1"><prod><cProd>x</cProd><xProd>x</xProd><uCom>UN</uCom>
                      <qCom>1</qCom><vUnCom>1</vUnCom><vProd>1</vProd></prod></det>
                    <total><ICMSTot><vNF>1</vNF></ICMSTot></total>
                  </infNFe>
                </NFe>
                """;
        assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Id");
    }

    @Test
    void rejeitaXmlSemItens() {
        String xml = """
                <NFe xmlns="http://www.portalfiscal.inf.br/nfe">
                  <infNFe Id="NFe35260511111111000111550010000007891234567892" versao="4.00">
                    <ide><nNF>1</nNF><serie>1</serie><dhEmi>2026-01-01T00:00:00-03:00</dhEmi></ide>
                    <emit><CNPJ>11111111000111</CNPJ><xNome>x</xNome></emit>
                    <total><ICMSTot><vNF>1</vNF></ICMSTot></total>
                  </infNFe>
                </NFe>
                """;
        assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("itens");
    }

    @Test
    void naoExpandeEntidadesExternasXxe() {
        // OWASP: payload XXE clássico tentando ler arquivo do disco. Parser
        // deve recusar via disallow-doctype-decl.
        String xxe = """
                <?xml version="1.0"?>
                <!DOCTYPE foo [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <NFe xmlns="http://www.portalfiscal.inf.br/nfe">
                  <infNFe Id="NFe35260511111111000111550010000007891234567892">
                    <emit><CNPJ>11111111000111</CNPJ><xNome>&xxe;</xNome></emit>
                  </infNFe>
                </NFe>
                """;
        assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(xxe.getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(ValidationException.class);
    }

    private InputStream fixture(String name) {
        InputStream stream = getClass().getClassLoader()
                .getResourceAsStream("nfe-fixtures/" + name);
        if (stream == null) {
            throw new IllegalStateException("Fixture não encontrada: " + name);
        }
        return stream;
    }
}
