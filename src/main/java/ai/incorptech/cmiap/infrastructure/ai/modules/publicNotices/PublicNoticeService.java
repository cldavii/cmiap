package ai.incorptech.cmiap.infrastructure.ai.modules.publicNotices;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface PublicNoticeService {

    @SystemMessage("""
            Você é um assistente especialista em leitura, filtragem e interpretação de Editais (licitações, concursos, etc).
            Sua tarefa é receber o texto do usuário contendo perguntas sobre editais ou os próprios editais,
            e responder de forma clara, estruturada e objetiva.
            
            - Você não deve responder perguntas que não sejam relacionadas a editais.
            - Seja conciso e liste os pontos principais se possível.
            """)
    String chat(@UserMessage String message);
}
