package ai.incorptech.cmiap.modules.agentes.model;

import java.util.List;

/**
 * Ficha de configuracao de um agente. Cada agente e um arquivo JSON em
 * src/main/resources/agentes/, e nao uma classe Java: para criar um agente novo
 * basta escrever uma ficha nova, sem mexer no codigo.
 *
 * Os campos seguem as secoes do catalogo de agentes da empresa (objetivo, escopo,
 * fora de escopo, autonomia, regras de decisao e formato de saida).
 *
 * @param id identificador usado na URL, por exemplo "editais-e-licitacoes"
 * @param nome nome mostrado para o usuario
 * @param objetivo para que o agente existe, em uma frase
 * @param escopo o que o agente faz
 * @param foraDeEscopo o que o agente nao faz
 * @param autonomia o que ele faz sozinho, o que precisa de aprovacao e o que nunca faz
 * @param regrasDeDecisao regras que o agente segue em situacoes especificas
 * @param formatoDeSaida o que o agente entrega como resposta
 * @param parametros informacoes que o usuario precisa passar para o agente
 * @param fontes arquivos de referencia em src/main/resources/agentes/fontes/
 */
public record FichaAgente(
		String id,
		String nome,
		String objetivo,
		List<String> escopo,
		List<String> foraDeEscopo,
		AutonomiaAgente autonomia,
		List<String> regrasDeDecisao,
		List<String> formatoDeSaida,
		List<ParametroAgente> parametros,
		List<String> fontes) {
}
