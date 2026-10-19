package ai.incorptech.cmiap.presentation.controller;

import ai.incorptech.cmiap.infrastructure.ai.modules.publicNotices.PublicNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class PublicNoticeController {

    private final PublicNoticeService publicNoticeService;

    //    Editais
    @PostMapping("/publicNotice/ask")
    public String ask(@RequestParam String prompt) {
        return publicNoticeService.chat(prompt);
    }
}
