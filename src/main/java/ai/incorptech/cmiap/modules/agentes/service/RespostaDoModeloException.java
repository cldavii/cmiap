package ai.incorptech.cmiap.modules.agentes.service;

/**
 * O modelo de IA respondeu, mas sem texto (por exemplo, quando bloqueia a resposta).
 * O controller transforma em 502.
 */
public class RespostaDoModeloException extends RuntimeException {

	public RespostaDoModeloException(String mensagem) {
		super(mensagem);
	}
}
