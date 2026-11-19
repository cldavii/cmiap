package ai.incorp.cmiap.agent.reports;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(wiringMode = AiServiceWiringMode.EXPLICIT, chatModel = "googleAiGeminiChatModel")
public interface ReportNarrativeAgent {

    @SystemMessage(fromResource = "/agents/reports.json")
    String summarize(@UserMessage String metrics);
}
