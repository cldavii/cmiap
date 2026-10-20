package ai.incorptech.cmiap.presentation.controller;

import ai.incorptech.cmiap.application.modules.contentGenerator.ContentGeneratorService;
import ai.incorptech.cmiap.infrastructure.ai.memory.AgentChatMemoryProvider;
import ai.incorptech.cmiap.presentation.controller.dto.content.ContentGenerationRequestDTO;
import ai.incorptech.cmiap.presentation.controller.dto.content.ContentGenerationResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/ai/content")
@RequiredArgsConstructor
public class ContentGenerationController {

    private final ContentGeneratorService contentGeneratorService;
    private final AgentChatMemoryProvider contentGeneratorChatMemoryProvider;

    @PostMapping("/generate")
    public ContentGenerationResponseDTO generate(
            @RequestBody @Valid ContentGenerationRequestDTO request,
            Authentication authentication) {
        UUID conversationId = request.conversationId() != null
                ? request.conversationId()
                : UUID.randomUUID();
        String memoryId = contentGeneratorChatMemoryProvider.memoryIdFor(
                authentication.getName(), conversationId);
        String content = contentGeneratorService.chat(memoryId, request.toPrompt());
        return new ContentGenerationResponseDTO(conversationId, content);
    }

    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<Void> clearConversation(
            @PathVariable UUID conversationId,
            Authentication authentication) {
        contentGeneratorChatMemoryProvider.clearConversation(
                authentication.getName(), conversationId);
        return ResponseEntity.noContent().build();
    }
}