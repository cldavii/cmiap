package ai.incorp.cmiap.agent.traffic;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(wiringMode = AiServiceWiringMode.EXPLICIT, chatModel = "googleAiGeminiChatModel")
public interface TrafficStrategyAgent {

    @SystemMessage(fromResource = "/agents/traffic.json")
    String suggest(@UserMessage String briefing);
}
