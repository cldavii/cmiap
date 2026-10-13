package ai.incorptech.cmiap.controllers;

import ai.incorptech.cmiap.controllers.dto.ChatPrompt;
import ai.incorptech.cmiap.controllers.dto.ChatReply;
import ai.incorptech.cmiap.services.ChatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class ChatController {

	private final ChatService chatService;

	public ChatController(ChatService chatService) {
		this.chatService = chatService;
	}

	@PostMapping("/chat")
	public ChatReply chat(@RequestBody(required = false) ChatPrompt prompt) {
		if (prompt == null || prompt.message() == null || prompt.message().isBlank()) {
			throw new IllegalArgumentException("O campo 'message' e obrigatorio e nao pode ser vazio.");
		}
		return new ChatReply(chatService.reply(prompt.message()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	ProblemDetail promptInvalido(IllegalArgumentException erro) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, erro.getMessage());
	}
}
