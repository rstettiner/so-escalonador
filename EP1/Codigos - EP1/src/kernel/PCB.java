package kernel;

import java.util.ArrayList;
import java.util.Comparator;

import operacoes.Carrega;
import operacoes.Operacao;
import operacoes.Soma;
import operacoes.OperacaoES;

public class PCB {
	public enum Estado {NOVO, PRONTO, EXECUTANDO, ESPERANDO, TERMINADO};
	public int idProcesso; // primeiro processo criado deve ter id = 0
	public Estado estado = Estado.NOVO;
	public int[] registradores = new int[5];
	public int contadorDePrograma = 0;
	public Operacao[] codigo;
	public int chuteBurst = 5;
	public int burst = 0;
	public int burstRestante = 0;
	public int tempoDeEspera = 0; //quanto ciclos permaneceu na fila de prontos
	public int tempoDeResposta = 0; //ciclos que ficou esperando na fila prontos até ser executado
	public int tempoDeRetorno = 0; //tempo de espera + tempo de es + tempo de cpu
	public ArrayList<OperacaoES> operacoesES = new ArrayList<OperacaoES>();
	public ArrayList<Operacao> operacoesCPU = new ArrayList<Operacao>();

	public Integer getId(){
		return this.idProcesso;
	}

	public PCB(Operacao[] _codigo) {
		this.codigo = _codigo;
		for(Operacao operacao: _codigo) {
			if(operacao instanceof OperacaoES) {
				OperacaoES aux = (OperacaoES) operacao;
				this.operacoesES.add(aux);
			} else {
				Operacao aux = operacao;
				this.operacoesCPU.add(aux);
			}
		}
	}

	public void setId(int _idProcesso) {
		this.idProcesso = _idProcesso;
	}

	public void setState(Estado _estado) {
		this.estado = _estado;
	}

	public static class shortestJobFirst implements Comparator<PCB> {

		@Override
		public int compare(PCB o1, PCB o2) {
			Integer compare = o1.chuteBurst - o2.chuteBurst;
			return (compare == 0 ? o1.idProcesso - o2.idProcesso : compare);
		}
	}

	public static class firstComeFirstServed implements Comparator<PCB> {

		@Override
		public int compare(PCB o1, PCB o2) {
			return o1.idProcesso - o2.idProcesso;
		}
	}

	public static class shortestRemainingTimeFirst implements Comparator<PCB> {

		@Override
		public int compare(PCB o1, PCB o2) {
			Integer compare = o1.burstRestante - o2.burstRestante;
			return (compare == 0 ? o1.idProcesso - o2.idProcesso : compare);
		}
	}

}
