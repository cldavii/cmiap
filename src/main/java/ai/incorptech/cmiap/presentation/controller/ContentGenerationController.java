package ai.incorptech.cmiap.presentation.controller;

import ai.incorptech.cmiap.infrastructure.ai.modules.contentGenerator.ContentGeneratorService;
import ai.incorptech.cmiap.presentation.controller.dto.content.ContentGenerationRequestDTO;
import ai.incorptech.cmiap.presentation.controller.dto.content.ContentGenerationResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/content")
@RequiredArgsConstructor
public class ContentGenerationController {

    private final ContentGeneratorService contentGeneratorService;

    @PostMapping("/generate")
    public ContentGenerationResponseDTO generate(@RequestBody @Valid ContentGenerationRequestDTO request) {
        return new ContentGenerationResponseDTO(contentGeneratorService.chat(request.toPrompt()));
    }
}