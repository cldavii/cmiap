package ai.incorptech.cmiap.modules.agentes.model;

/**
 * Uma informacao que o usuario passa para o agente, como o perfil de cliente ideal
 * ou a capacidade de posts por semana. O catalogo da empresa pede que o agente nunca
 * invente esses valores.
 *
 * @param nome nome do parametro, usado no JSON da requisicao
 * @param descricao o que o usuario deve informar
 * @param obrigatorio se for true, a conversa e recusada quando o parametro falta
 */
public record ParametroAgente(String nome, String descricao, boolean obrigatorio) {
}
