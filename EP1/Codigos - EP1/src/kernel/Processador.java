package kernel;
import operacoes.Carrega;
import operacoes.Operacao;
import operacoes.Soma;
import operacoes.OperacaoES;

public class Processador {

	public int[] registradores = new int[5];

	public void executa(Operacao op) {
		if (op instanceof Carrega) {
			Carrega c = (Carrega) op;
			registradores[c.registrador] = c.valor;
		} else if (op instanceof Soma) {
			Soma s = (Soma) op;
			int a = registradores[s.registradorParcela1];
			int b = registradores[s.registradorParcela2]; 
			registradores[s.registradorTotal] = a + b;
		} else if(op instanceof OperacaoES) {
			OperacaoES es = (OperacaoES) op;
			try {
				Thread.sleep(1500);
			} catch(Exception e){
				throw new RuntimeException("A execução foi interrompida em algum momento...");
			}
		} else {
			throw new RuntimeException("Operacão Inválida");
		}
	}
}
