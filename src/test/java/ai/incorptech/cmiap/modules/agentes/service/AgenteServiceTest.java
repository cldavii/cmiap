package ai.incorptech.cmiap.modules.agentes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.incorptech.cmiap.modules.agentes.model.FichaAgente;
import ai.incorptech.cmiap.modules.agentes.model.ParametroAgente;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.FinishReason;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AgenteServiceTest {

	private final CatalogoDeAgentes catalogo = new CatalogoDeAgentes();

	private final ChatModel chatModel = mock(ChatModel.class);

	private final AgenteService agenteService = new AgenteService(catalogo, chatModel);

	@BeforeEach
	void modeloRespondeSempreOMesmoTexto() {
		modeloResponde(chatModel, "Resposta do modelo.", FinishReason.STOP);
	}

	@Test
	void devolveOTextoDoModeloComOAvisoDeDadosFicticios() {
		String resposta = agenteService.conversar("editais-e-licitacoes", "Analise este edital.", new HashMap<>());

		assertThat(resposta).isEqualTo("Resposta do modelo." + AgenteService.AVISO_DADOS_FICTICIOS);
	}

	@Test
	void mandaAsRegrasDaFichaComoSistemaEOPedidoComoUsuario() {
		agenteService.conversar("editais-e-licitacoes", "Analise este edital.", new HashMap<>());

		assertThat(instrucoesEnviadas()).contains("Editais e Licitações");
		assertThat(instrucoesEnviadas()).contains("Nunca preencher valor de proposta");
		assertThat(mensagemDoUsuarioEnviada()).contains("<mensagem>\nAnalise este edital.\n</mensagem>");
	}

	@Test
	void incluiOConteudoDasFontesNasInstrucoes() {
		agenteService.conversar("producao-de-conteudo", "Escreva um post.", parametros("produto", "Sabiá Agenda"));

		assertThat(instrucoesEnviadas()).contains("Sabiá Software");
		assertThat(instrucoesEnviadas()).contains("FICTÍCIO");
	}

	// Se o valor do parametro fosse para as instrucoes de sistema, o usuario poderia
	// escrever ali algo que o modelo trataria como regra (injecao de prompt).
	@Test
	void osValoresDosParametrosVaoNaMensagemDoUsuarioENaoNasInstrucoes() {
		agenteService.conversar("prospeccao-de-leads", "Monte uma cadencia.", parametros("icp", "Conselhos regionais do Nordeste"));

		assertThat(instrucoesEnviadas()).doesNotContain("Conselhos regionais do Nordeste");
		assertThat(mensagemDoUsuarioEnviada()).contains("- icp: Conselhos regionais do Nordeste");
	}

	@Test
	void trocaQuebraDeLinhaDoParametroPorEspaco() {
		agenteService.conversar("prospeccao-de-leads", "Monte uma cadencia.", parametros("icp", "linha 1\n- regra falsa"));

		assertThat(mensagemDoUsuarioEnviada()).contains("- icp: linha 1 - regra falsa");
	}

	@Test
	void marcaComoNaoInformadoOParametroOpcionalQueFaltou() {
		agenteService.conversar("prospeccao-de-leads", "Monte uma cadencia.", parametros("icp", "Conselhos regionais"));

		assertThat(mensagemDoUsuarioEnviada()).contains("- limiteDeCadencia: não informado");
	}

	@Test
	void recusaQuandoFaltaParametroObrigatorio() {
		assertThatThrownBy(() -> agenteService.conversar("prospeccao-de-leads", "Monte uma cadencia.", new HashMap<>()))
				.isInstanceOf(PedidoInvalidoException.class)
				.hasMessageContaining("icp");

		verify(chatModel, never()).chat(any(ChatRequest.class));
	}

	@Test
	void recusaParametroQueNaoExisteNaFicha() {
		Map<String, String> informados = parametros("icp", "Conselhos regionais");
		informados.put("limiteCadencia", "4 toques");

		assertThatThrownBy(() -> agenteService.conversar("prospeccao-de-leads", "Monte uma cadencia.", informados))
				.isInstanceOf(PedidoInvalidoException.class)
				.hasMessageContaining("limiteCadencia");

		verify(chatModel, never()).chat(any(ChatRequest.class));
	}

	@Test
	void recusaMensagemGrandeDemais() {
		String mensagemEnorme = "a".repeat(AgenteService.LIMITE_DA_MENSAGEM + 1);

		assertThatThrownBy(() -> agenteService.conversar("editais-e-licitacoes", mensagemEnorme, new HashMap<>()))
				.isInstanceOf(PedidoInvalidoException.class);

		verify(chatModel, never()).chat(any(ChatRequest.class));
	}

	@Test
	void recusaParametroGrandeDemais() {
		String valorEnorme = "a".repeat(AgenteService.LIMITE_DO_PARAMETRO + 1);

		assertThatThrownBy(() -> agenteService.conversar("prospeccao-de-leads", "Oi", parametros("icp", valorEnorme)))
				.isInstanceOf(PedidoInvalidoException.class)
				.hasMessageContaining("icp");

		verify(chatModel, never()).chat(any(ChatRequest.class));
	}

	@Test
	void recusaMensagemEmBranco() {
		assertThatThrownBy(() -> agenteService.conversar("editais-e-licitacoes", "   ", new HashMap<>()))
				.isInstanceOf(PedidoInvalidoException.class);

		verify(chatModel, never()).chat(any(ChatRequest.class));
	}

	@Test
	void avisaQuandoOAgenteNaoExiste() {
		assertThatThrownBy(() -> agenteService.conversar("agente-que-nao-existe", "Oi", new HashMap<>()))
				.isInstanceOf(AgenteNaoEncontradoException.class);

		verify(chatModel, never()).chat(any(ChatRequest.class));
	}

	@Test
	void respostaSemTextoViraErro() {
		modeloResponde(chatModel, "", FinishReason.CONTENT_FILTER);

		assertThatThrownBy(() -> agenteService.conversar("editais-e-licitacoes", "Analise este edital.", new HashMap<>()))
				.isInstanceOf(RespostaDoModeloException.class);
	}

	@Test
	void respostaCortadaPeloLimiteGanhaUmAviso() {
		modeloResponde(chatModel, "Ficha do edital: objeto...", FinishReason.LENGTH);

		String resposta = agenteService.conversar("editais-e-licitacoes", "Analise este edital.", new HashMap<>());

		assertThat(resposta).contains(AgenteService.AVISO_RESPOSTA_CORTADA);
	}

	// As proibicoes ("Voce nunca faz") sao a parte mais importante de cada ficha.
	// Este teste confere, para todos os agentes do catalogo, que elas chegam ao modelo.
	@Test
	void todoAgenteMandaASuaListaNuncaParaOModelo() {
		for (FichaAgente ficha : catalogo.listar()) {
			ChatModel modeloDoTeste = mock(ChatModel.class);
			modeloResponde(modeloDoTeste, "ok", FinishReason.STOP);
			AgenteService servico = new AgenteService(catalogo, modeloDoTeste);

			servico.conversar(ficha.id(), "Oi", parametrosObrigatoriosPreenchidos(ficha));

			ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
			verify(modeloDoTeste).chat(captor.capture());
			String instrucoes = ((SystemMessage) captor.getValue().messages().get(0)).text();

			assertThat(instrucoes).as(ficha.id()).contains("Você nunca faz");
			for (String proibicao : ficha.autonomia().nunca()) {
				assertThat(instrucoes).as(ficha.id()).contains(proibicao);
			}
		}
	}

	private void modeloResponde(ChatModel modelo, String texto, FinishReason motivoDoFim) {
		ChatResponse resposta = ChatResponse.builder()
				.aiMessage(AiMessage.from(texto))
				.finishReason(motivoDoFim)
				.build();
		when(modelo.chat(any(ChatRequest.class))).thenReturn(resposta);
	}

	private Map<String, String> parametros(String nome, String valor) {
		Map<String, String> parametros = new HashMap<>();
		parametros.put(nome, valor);
		return parametros;
	}

	private Map<String, String> parametrosObrigatoriosPreenchidos(FichaAgente ficha) {
		Map<String, String> parametros = new HashMap<>();
		for (ParametroAgente parametro : ficha.parametros()) {
			if (parametro.obrigatorio()) {
				parametros.put(parametro.nome(), "valor de teste");
			}
		}
		return parametros;
	}

	// A primeira mensagem do pedido e a de sistema, com as regras montadas a partir da ficha.
	private String instrucoesEnviadas() {
		return ((SystemMessage) pedidoEnviadoAoModelo().messages().get(0)).text();
	}

	// A segunda mensagem e a do usuario, com os parametros e o pedido.
	private String mensagemDoUsuarioEnviada() {
		return ((UserMessage) pedidoEnviadoAoModelo().messages().get(1)).singleText();
	}

	// Captura o pedido que o servico mandou para o modelo falso do teste.
	private ChatRequest pedidoEnviadoAoModelo() {
		ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
		verify(chatModel).chat(captor.capture());
		return captor.getValue();
	}
}
