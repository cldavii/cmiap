package ai.incorptech.cmiap.services;

import dev.langchain4j.model.chat.ChatModel;
import org.springframework.stereotype.Service;

/**
 * Ponto unico de conversa com o agente. Concentra aqui a validacao de entrada para que
 * nenhuma chamada em branco chegue a consumir cota da API.
 */
@Service
public class ChatService {

	private final ChatModel chatModel;

	public ChatService(ChatModel chatModel) {
		this.chatModel = chatModel;
	}

	public String reply(String prompt) {
		if (prompt == null || prompt.isBlank()) {
			throw new IllegalArgumentException("O prompt nao pode ser vazio.");
		}
		return chatModel.chat(prompt);
	}
}
