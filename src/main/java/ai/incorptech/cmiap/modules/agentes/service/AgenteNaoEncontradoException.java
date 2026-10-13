package ai.incorptech.cmiap.modules.agentes.service;

public class AgenteNaoEncontradoException extends RuntimeException {

	public AgenteNaoEncontradoException(String id) {
		super("Agente nao encontrado: " + id);
	}
}
