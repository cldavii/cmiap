package ai.incorptech.cmiap.presentation.controller;

import ai.incorptech.cmiap.infrastructure.ai.modules.publicNotices.PublicNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai/pn")
@RequiredArgsConstructor
public class PublicNoticeController {

    private final PublicNoticeService publicNoticeService;

    //    Editais
    @PostMapping("/generate")
    public String ask(@RequestParam String prompt) {
        return publicNoticeService.chat(prompt);
    }
}
