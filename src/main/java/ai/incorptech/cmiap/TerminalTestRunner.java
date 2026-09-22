package ai.incorptech.cmiap;

import ai.incorptech.cmiap.modules.editais.service.EditalAiService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * ATENÇÃO: ESTE ARQUIVO FOI CRIADO APENAS PARA TESTES INICIAIS NO TERMINAL.
 * ELE DEVE SER APAGADO AO FINAL DO PROJETO QUANDO AS ROTAS WEB ESTIVEREM DEFINITIVAS E INTEGRADAS.
 */
@Component
public class TerminalTestRunner implements CommandLineRunner {

    private final EditalAiService editalAiService;

    public TerminalTestRunner(EditalAiService editalAiService) {
        this.editalAiService = editalAiService;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("======================================================");
        System.out.println("🤖 INICIANDO TESTE NO TERMINAL - AGENTE DE EDITAIS 🤖");
        System.out.println("======================================================");
        
        try {
            String resposta = editalAiService.handleRequest("Quais são as regras mais comuns exigidas na qualificação técnica de um edital de TI?").content();
            System.out.println("RESPOSTA DA IA:");
            System.out.println(resposta);
        } catch (Exception e) {
            System.err.println("Erro ao se comunicar com a IA (Verifique a API Key): " + e.getMessage());
        }
        
        System.out.println("======================================================");
        System.out.println("TESTE FINALIZADO.");
        System.out.println("======================================================");
    }
}
