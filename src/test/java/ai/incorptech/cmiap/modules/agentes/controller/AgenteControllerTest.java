package ai.incorptech.cmiap.modules.agentes.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ai.incorptech.cmiap.modules.agentes.service.AgenteNaoEncontradoException;
import ai.incorptech.cmiap.modules.agentes.service.AgenteService;
import ai.incorptech.cmiap.modules.agentes.service.CatalogoDeAgentes;
import ai.incorptech.cmiap.modules.agentes.service.PedidoInvalidoException;
import ai.incorptech.cmiap.modules.agentes.service.RespostaDoModeloException;
import dev.langchain4j.exception.LangChain4jException;
import dev.langchain4j.exception.RateLimitException;
import dev.langchain4j.exception.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(AgenteController.class)
@Import(CatalogoDeAgentes.class)
class AgenteControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CatalogoDeAgentes catalogo;

	@MockitoBean
	private AgenteService agenteService;

	@Test
	void listaTodosOsAgentesDoCatalogo() throws Exception {
		mockMvc.perform(get("/agentes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(catalogo.listar().size()));
	}

	@Test
	void mostraAFichaDeUmAgente() throws Exception {
		mockMvc.perform(get("/agentes/editais-e-licitacoes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Editais e Licitações"));
	}

	@Test
	void devolve404AoPedirAFichaDeUmAgenteQueNaoExiste() throws Exception {
		mockMvc.perform(get("/agentes/agente-que-nao-existe"))
				.andExpect(status().isNotFound());
	}

	@Test
	void devolve200ComARespostaDoAgente() throws Exception {
		given(agenteService.conversar(eq("editais-e-licitacoes"), eq("Analise este edital."), any()))
				.willReturn("Ficha do edital pronta.");

		conversarComEditais()
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reply").value("Ficha do edital pronta."));
	}

	@Test
	void devolve404QuandoOAgenteNaoExiste() throws Exception {
		given(agenteService.conversar(eq("agente-que-nao-existe"), anyString(), any()))
				.willThrow(new AgenteNaoEncontradoException("agente-que-nao-existe"));

		mockMvc.perform(post("/agentes/agente-que-nao-existe/chat")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"message\":\"Oi\"}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void devolve400ComOMotivoQuandoOPedidoEInvalido() throws Exception {
		given(agenteService.conversar(anyString(), anyString(), any()))
				.willThrow(new PedidoInvalidoException("Falta o parametro obrigatorio: icp"));

		conversarComEditais()
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Falta o parametro obrigatorio: icp"));
	}

	@Test
	void devolve400QuandoNaoHaCorpo() throws Exception {
		mockMvc.perform(post("/agentes/editais-e-licitacoes/chat")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());

		verify(agenteService, never()).conversar(anyString(), anyString(), any());
	}

	@Test
	void devolve429QuandoACotaDoModeloAcaba() throws Exception {
		given(agenteService.conversar(anyString(), anyString(), any()))
				.willThrow(new RateLimitException("quota exceeded"));

		conversarComEditais()
				.andExpect(status().isTooManyRequests());
	}

	@Test
	void devolve504QuandoOModeloDemora() throws Exception {
		given(agenteService.conversar(anyString(), anyString(), any()))
				.willThrow(new TimeoutException("read timed out"));

		conversarComEditais()
				.andExpect(status().isGatewayTimeout());
	}

	@Test
	void devolve502QuandoOModeloNaoDevolveTexto() throws Exception {
		given(agenteService.conversar(anyString(), anyString(), any()))
				.willThrow(new RespostaDoModeloException("O modelo nao devolveu texto."));

		conversarComEditais()
				.andExpect(status().isBadGateway());
	}

	@Test
	void devolve502SemExporOErroTecnicoDoModelo() throws Exception {
		given(agenteService.conversar(anyString(), anyString(), any()))
				.willThrow(new LangChain4jException("API key not valid"));

		conversarComEditais()
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.detail").value("Nao foi possivel falar com o modelo de IA agora. Tente de novo mais tarde."));
	}

	private ResultActions conversarComEditais() throws Exception {
		return mockMvc.perform(post("/agentes/editais-e-licitacoes/chat")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"message\":\"Analise este edital.\"}"));
	}
}
