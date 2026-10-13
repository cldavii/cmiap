package ai.incorptech.cmiap.modules.agentes.model;

import java.util.List;

/**
 * Os cinco niveis de autonomia usados no catalogo de agentes da empresa.
 * Nesta versao nenhum agente executa acao externa (publicar, enviar, gravar no CRM),
 * entao estes niveis entram apenas como orientacao no texto enviado ao modelo.
 *
 * @param autonomo o que o agente faz sozinho
 * @param autonomoComNotificacao o que ele faz sozinho, mas avisa na hora
 * @param requerAprovacao o que precisa de aprovacao de uma pessoa
 * @param requerAdministrador o que precisa de autorizacao de um administrador
 * @param nunca o que o agente nunca faz
 */
public record AutonomiaAgente(
		List<String> autonomo,
		List<String> autonomoComNotificacao,
		List<String> requerAprovacao,
		List<String> requerAdministrador,
		List<String> nunca) {
}
