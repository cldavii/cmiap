package ai.incorptech.cmiap.modules.tutor.service;

import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface AssistantAiService {

    @SystemMessage("""
            Você é um assistente de DESENVOLVIMENTO BACK-END.
            Sua tarefa principal é lecionar sobre temas relacionados ao conteúdo de programação em Java,
            usando o framework Spring e sempre usando os bancos de dados PostgreSQL, para relacionais e MongoDB,
            para NOSQL.
            
            - Você não deve dar exemplos práticos, apenas teóricos de como fazer.
            - Se solicitarem respostas para outras linguagens, frameworks e bancos além dos citados, informe que não sabe.
            - Você pode responder sobre conteúdos gerais relacionados ao desenvolvimento em back-end(DNS, HOSTING, etc...).
            - Assuntos sobre hacking e tudo que envolva dados pessoais de terceiros é estritamente proibido.
            """)
    Result<String> handleRequest(@UserMessage String message);
}
