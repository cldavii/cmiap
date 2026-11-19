package ai.incorp.cmiap.agent.reports;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agents/reports")
@RequiredArgsConstructor
public class ReportKpiController {

    private final ReportKpiService service;

    @PostMapping("/kpis")
    public ReportResponseDTO report(@Valid @RequestBody ReportRequestDTO request) {
        return service.report(request);
    }
}
