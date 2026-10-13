package ai.incorptech.cmiap.modules.agentes.controller.dto;

import java.util.Map;

/**
 * Corpo do POST /agentes/{id}/chat.
 * Exemplo: {"message": "Analise este edital...", "parametros": {"criteriosDeElegibilidade": "..."}}
 */
public record MensagemParaAgente(String message, Map<String, String> parametros) {
}
