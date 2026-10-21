package ai.incorptech.cmiap.modules.agentes.service;

/**
 * Erro em algo que o usuario enviou (mensagem vazia, parametro faltando, texto grande demais).
 * O controller transforma em 400.
 */
public class PedidoInvalidoException extends RuntimeException {

	public PedidoInvalidoException(String mensagem) {
		super(mensagem);
	}
}
