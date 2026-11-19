package ai.incorp.cmiap.agent.bids;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(wiringMode = AiServiceWiringMode.EXPLICIT, chatModel = "googleAiGeminiChatModel")
public interface BidsAgent {

    @SystemMessage(fromResource = "/agents/bids.json")
    String analyze(@UserMessage String briefing);
}

