package ai.incorptech.cmiap.modules.agentes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.incorptech.cmiap.modules.agentes.model.FichaAgente;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class CatalogoDeAgentesTest {

	private final CatalogoDeAgentes catalogo = new CatalogoDeAgentes();

	@Test
	void carregaOAgenteDeEditais() {
		List<String> ids = new ArrayList<>();
		for (FichaAgente ficha : catalogo.listar()) {
			ids.add(ficha.id());
		}

		assertThat(ids).contains("editais-e-licitacoes");
	}

	@Test
	void encontraUmAgentePeloId() {
		FichaAgente ficha = catalogo.buscar("editais-e-licitacoes");

		assertThat(ficha.nome()).isEqualTo("Editais e Licitações");
	}

	@Test
	void avisaQuandoOAgenteNaoExiste() {
		assertThatThrownBy(() -> catalogo.buscar("agente-que-nao-existe"))
				.isInstanceOf(AgenteNaoEncontradoException.class);
	}

	// Os documentos reais de marca da empresa sao confidenciais e nao podem ser enviados
	// ao modelo de IA. Por isso as fontes daqui sao ficticias, e este teste garante que
	// nem elas nem as fichas citam a empresa.
	// O "\b" (fronteira de palavra) evita o falso alarme em palavras como "Incorporar".
	@Test
	void nenhumaFichaNemFonteCitaAEmpresaReal() {
		Pattern nomeDaEmpresa = Pattern.compile("\\bincorp\\b", Pattern.CASE_INSENSITIVE);

		for (FichaAgente ficha : catalogo.listar()) {
			assertThat(nomeDaEmpresa.matcher(ficha.toString()).find()).as(ficha.id()).isFalse();

			for (String fonte : ficha.fontes()) {
				assertThat(nomeDaEmpresa.matcher(catalogo.lerFonte(fonte)).find()).as(fonte).isFalse();
			}
		}
	}

	// Os testes abaixo leem fichas erradas de propósito, em src/test/resources/fichas-de-teste/.

	@Test
	void recusaFichaComCampoDigitadoErrado() {
		assertThatThrownBy(() -> new CatalogoDeAgentes("classpath:fichas-de-teste/campo-desconhecido/*.json"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("campo-desconhecido.json");
	}

	@Test
	void recusaFichaComIdDiferenteDoNomeDoArquivo() {
		assertThatThrownBy(() -> new CatalogoDeAgentes("classpath:fichas-de-teste/id-diferente-do-arquivo/*.json"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("nao bate com o nome do arquivo");
	}

	@Test
	void recusaFichaSemAListaNunca() {
		assertThatThrownBy(() -> new CatalogoDeAgentes("classpath:fichas-de-teste/sem-lista-nunca/*.json"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("autonomia.nunca");
	}

	@Test
	void recusaFichaQueCitaFonteInexistente() {
		assertThatThrownBy(() -> new CatalogoDeAgentes("classpath:fichas-de-teste/fonte-inexistente/*.json"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("arquivo-que-nao-existe.md");
	}

	@Test
	void recusaPastaSemNenhumaFicha() {
		assertThatThrownBy(() -> new CatalogoDeAgentes("classpath:fichas-de-teste/*.json"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Nenhuma ficha");
	}
}
