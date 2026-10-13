package ai.incorptech.cmiap.modules.agentes.service;

import ai.incorptech.cmiap.modules.agentes.model.FichaAgente;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Le as fichas dos agentes (arquivos JSON em src/main/resources/agentes/) uma vez,
 * quando a aplicacao sobe, e guarda tudo em memoria.
 *
 * Se alguma ficha estiver errada, a aplicacao nem sobe. E melhor descobrir o erro
 * na hora de subir do que com um agente funcionando sem as proprias regras.
 *
 * Hoje as fichas ficam em arquivos. Quando o projeto tiver banco de dados, so esta
 * classe muda: o resto da aplicacao continua pedindo as fichas por aqui.
 */
@Component
public class CatalogoDeAgentes {

	// "*.json" pega todo arquivo .json da pasta agentes/. Cada arquivo e um agente.
	private static final String PASTA_DAS_FICHAS = "classpath:agentes/*.json";
	private static final String PASTA_DAS_FONTES = "agentes/fontes/";

	// Recusa campos que nao existem na FichaAgente. Sem isso, um nome de campo
	// digitado errado seria ignorado em silencio e a lista sumiria do agente.
	private final ObjectMapper leitorDeJson = JsonMapper.builder()
			.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.build();

	// LinkedHashMap mantem os agentes na ordem em que foram lidos.
	private final Map<String, FichaAgente> fichasPorId = new LinkedHashMap<>();

	public CatalogoDeAgentes() {
		this(PASTA_DAS_FICHAS);
	}

	// Construtor usado pelos testes, para ler fichas de outra pasta.
	CatalogoDeAgentes(String pastaDasFichas) {
		try {
			Resource[] arquivos = new PathMatchingResourcePatternResolver().getResources(pastaDasFichas);
			for (Resource arquivo : arquivos) {
				FichaAgente ficha = lerFicha(arquivo);
				validarFicha(ficha, arquivo.getFilename());
				fichasPorId.put(ficha.id(), ficha);
			}
		} catch (IOException erro) {
			throw new IllegalStateException("Nao foi possivel ler as fichas dos agentes.", erro);
		}

		if (fichasPorId.isEmpty()) {
			throw new IllegalStateException("Nenhuma ficha de agente encontrada em " + pastaDasFichas);
		}
	}

	// Transforma um arquivo JSON em um objeto FichaAgente.
	private FichaAgente lerFicha(Resource arquivo) throws IOException {
		try (InputStream conteudo = arquivo.getInputStream()) {
			return leitorDeJson.readValue(conteudo, FichaAgente.class);
		} catch (RuntimeException erro) {
			throw new IllegalStateException("Ficha com erro no arquivo " + arquivo.getFilename() + ": " + erro.getMessage(), erro);
		}
	}

	private void validarFicha(FichaAgente ficha, String nomeDoArquivo) {
		String onde = "Ficha " + nomeDoArquivo + ": ";

		if (ficha.id() == null || ficha.id().isBlank()) {
			throw new IllegalStateException(onde + "falta o campo id.");
		}
		// O id precisa bater com o nome do arquivo. Isso evita copiar uma ficha
		// para criar outra e esquecer de trocar o id.
		if (!nomeDoArquivo.equals(ficha.id() + ".json")) {
			throw new IllegalStateException(onde + "o id '" + ficha.id() + "' nao bate com o nome do arquivo.");
		}
		if (fichasPorId.containsKey(ficha.id())) {
			throw new IllegalStateException(onde + "id repetido: " + ficha.id());
		}

		exigirTexto(ficha.nome(), onde + "falta o campo nome.");
		exigirTexto(ficha.objetivo(), onde + "falta o campo objetivo.");
		exigirLista(ficha.escopo(), onde + "a lista escopo esta vazia.");
		exigirLista(ficha.foraDeEscopo(), onde + "a lista foraDeEscopo esta vazia.");
		exigirLista(ficha.regrasDeDecisao(), onde + "a lista regrasDeDecisao esta vazia.");
		exigirLista(ficha.formatoDeSaida(), onde + "a lista formatoDeSaida esta vazia.");

		if (ficha.autonomia() == null) {
			throw new IllegalStateException(onde + "falta o campo autonomia.");
		}
		exigirLista(ficha.autonomia().nunca(), onde + "a lista autonomia.nunca esta vazia.");

		// Confere ja na subida se toda fonte citada existe.
		if (ficha.fontes() != null) {
			for (String fonte : ficha.fontes()) {
				lerFonte(fonte);
			}
		}
	}

	private void exigirTexto(String valor, String mensagemDeErro) {
		if (valor == null || valor.isBlank()) {
			throw new IllegalStateException(mensagemDeErro);
		}
	}

	private void exigirLista(List<String> lista, String mensagemDeErro) {
		if (lista == null || lista.isEmpty()) {
			throw new IllegalStateException(mensagemDeErro);
		}
	}

	public List<FichaAgente> listar() {
		return new ArrayList<>(fichasPorId.values());
	}

	/**
	 * Devolve a ficha do agente. Se nao existir agente com esse id, lanca
	 * AgenteNaoEncontradoException, que o controller transforma em 404.
	 */
	public FichaAgente buscar(String id) {
		FichaAgente ficha = fichasPorId.get(id);
		if (ficha == null) {
			throw new AgenteNaoEncontradoException(id);
		}
		return ficha;
	}

	/**
	 * Le o texto de um arquivo de referencia citado na ficha, como as diretrizes de marca.
	 */
	public String lerFonte(String nomeDoArquivo) {
		ClassPathResource arquivo = new ClassPathResource(PASTA_DAS_FONTES + nomeDoArquivo);
		try {
			return arquivo.getContentAsString(StandardCharsets.UTF_8);
		} catch (IOException erro) {
			throw new IllegalStateException("Fonte de referencia nao encontrada: " + nomeDoArquivo, erro);
		}
	}
}
