package ai.incorp.cmiap.agent.seo;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agents/seo")
@RequiredArgsConstructor
public class SeoAnalysisController {

    private final SeoAnalysisService seoAnalysisService;

    @PostMapping("/analyze")
    public SeoAnalysisResult analyze(@Valid @RequestBody SeoAnalysisRequest request) {
        return seoAnalysisService.analyze(request);
    }
}
