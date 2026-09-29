package ai.incorptech.cmiap.modules.editais.controller;

import ai.incorptech.cmiap.modules.editais.service.EditalAiService;
import dev.langchain4j.service.Result;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/editais")
public class EditalController {

    private final EditalAiService editalAiService;

    public EditalController(EditalAiService editalAiService) {
        this.editalAiService = editalAiService;
    }

    @GetMapping
    public String getAll() {
        return "GET: Endpoint para listar editais (não implementado)";
    }

    @PostMapping
    public String create(@RequestBody String body) {
        return "POST: Endpoint para criar/processar edital. Exemplo de uso de agente:\n" 
                + editalAiService.handleRequest(body).content();
    }

    @PutMapping("/{id}")
    public String update(@PathVariable String id, @RequestBody String body) {
        return "PUT: Endpoint para atualizar edital " + id;
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable String id) {
        return "DELETE: Endpoint para deletar edital " + id;
    }
}
