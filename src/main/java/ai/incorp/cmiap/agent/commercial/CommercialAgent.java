package ai.incorp.cmiap.agent.commercial;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(wiringMode = AiServiceWiringMode.EXPLICIT, chatModel = "googleAiGeminiChatModel")
public interface CommercialAgent {

    @SystemMessage(fromResource = "/agents/commercial.json")
    String advise(@UserMessage String briefing);
}
