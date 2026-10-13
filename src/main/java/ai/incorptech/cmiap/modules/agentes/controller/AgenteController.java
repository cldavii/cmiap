package ai.incorptech.cmiap.modules.agentes.controller;

import ai.incorptech.cmiap.controllers.dto.ChatReply;
import ai.incorptech.cmiap.modules.agentes.controller.dto.MensagemParaAgente;
import ai.incorptech.cmiap.modules.agentes.model.FichaAgente;
import ai.incorptech.cmiap.modules.agentes.service.AgenteNaoEncontradoException;
import ai.incorptech.cmiap.modules.agentes.service.AgenteService;
import ai.incorptech.cmiap.modules.agentes.service.CatalogoDeAgentes;
import ai.incorptech.cmiap.modules.agentes.service.PedidoInvalidoException;
import ai.incorptech.cmiap.modules.agentes.service.RespostaDoModeloException;
import dev.langchain4j.exception.LangChain4jException;
import dev.langchain4j.exception.RateLimitException;
import dev.langchain4j.exception.TimeoutException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agentes")
public class AgenteController {

	private static final Logger log = LoggerFactory.getLogger(AgenteController.class);

	private final CatalogoDeAgentes catalogo;
	private final AgenteService agenteService;

	public AgenteController(CatalogoDeAgentes catalogo, AgenteService agenteService) {
		this.catalogo = catalogo;
		this.agenteService = agenteService;
	}

	@GetMapping
	public List<FichaAgente> listar() {
		return catalogo.listar();
	}

	@GetMapping("/{id}")
	public FichaAgente buscar(@PathVariable String id) {
		return catalogo.buscar(id);
	}

	@PostMapping("/{id}/chat")
	public ChatReply conversar(@PathVariable String id, @RequestBody(required = false) MensagemParaAgente corpo) {
		if (corpo == null) {
			throw new PedidoInvalidoException("O corpo da requisicao e obrigatorio, com o campo 'message'.");
		}
		String resposta = agenteService.conversar(id, corpo.message(), corpo.parametros());
		return new ChatReply(resposta);
	}

	// Cada erro vira um codigo HTTP diferente, para o front saber o que dizer ao usuario.

	@ExceptionHandler(AgenteNaoEncontradoException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	ProblemDetail agenteNaoEncontrado(AgenteNaoEncontradoException erro) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, erro.getMessage());
	}

	@ExceptionHandler(PedidoInvalidoException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	ProblemDetail pedidoInvalido(PedidoInvalidoException erro) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, erro.getMessage());
	}

	@ExceptionHandler(RateLimitException.class)
	@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
	ProblemDetail cotaEsgotada() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,
				"A cota gratuita do modelo de IA acabou. Tente de novo mais tarde.");
	}

	@ExceptionHandler(TimeoutException.class)
	@ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
	ProblemDetail tempoEsgotado() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.GATEWAY_TIMEOUT,
				"O modelo de IA demorou demais para responder. Tente de novo.");
	}

	@ExceptionHandler(RespostaDoModeloException.class)
	@ResponseStatus(HttpStatus.BAD_GATEWAY)
	ProblemDetail respostaVazia(RespostaDoModeloException erro) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, erro.getMessage());
	}

	// Qualquer outra falha do modelo (chave invalida, servico fora do ar). A mensagem
	// tecnica fica no log do servidor e nao vai para o usuario.
	@ExceptionHandler(LangChain4jException.class)
	@ResponseStatus(HttpStatus.BAD_GATEWAY)
	ProblemDetail falhaNoModelo(LangChain4jException erro) {
		log.error("Falha ao chamar o modelo de IA", erro);
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
				"Nao foi possivel falar com o modelo de IA agora. Tente de novo mais tarde.");
	}
}
