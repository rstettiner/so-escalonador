package kernel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import kernel.PCB.Estado;
import operacoes.Carrega;
import operacoes.Operacao;
import operacoes.OperacaoES;
import operacoes.Soma;

public class SeuSO extends SO {

	List<PCB> processos = new ArrayList<PCB>();
	List<PCB> prontos = new ArrayList<PCB>();
	List<PCB> esperando = new ArrayList<PCB>();
	List<PCB> terminados = new ArrayList<PCB>();
	PCB pcbExecutando = null;
	int trocasDeContexto = 0;
	int chuteBurst = 5;
	Escalonador escalonadorEscolhido;

	@Override
	// ATENCÃO: cria o processo mas o mesmo 
	// só estará "pronto" no próximo ciclo
	protected void criaProcesso(Operacao[] codigo) {
		// TODO Auto-generated method stub
		PCB processo = new PCB(codigo);
		processo.setId(this.idProcessoNovo());
		processo.setState(Estado.NOVO);
		processo.setChuteBurst(chuteBurst);
		processos.add(processo);
	}

	public PCB buscaId(Integer id) {
		for(PCB _pcb : this.processos) {
			if(_pcb.idProcesso == id) {
				return _pcb;
			}
		}
		return null;
	}

	public Integer returnIndex(ArrayList<Integer> ids, Integer ourId){
		for(int i = 0; i< ids.size();i++) {
			if(ids.get(i) == ourId) {
				return i;
			}
		}
		return -1;
	}

	@Override
	protected void trocaContexto(PCB pcbAtual, PCB pcbProximo) {
		// TODO Auto-generated method stub
		if(pcbAtual == null){
			//primeiro processo sendo executado
			this.pcbExecutando = pcbProximo;
			pcbProximo.setState(Estado.EXECUTANDO);
			this.trocasDeContexto++;
		} else {
			if (this.escalonadorEscolhido == Escalonador.FIRST_COME_FIRST_SERVED || escalonadorEscolhido == Escalonador.SHORTEST_JOB_FIRST) {
				if(this.terminados.contains(pcbAtual)) {
					pcbProximo.setState(Estado.EXECUTANDO);
					this.pcbExecutando = pcbProximo;
					pcbAtual.setState(Estado.TERMINADO);
					this.trocasDeContexto++;
				}
			} else {
				// preemptivos
				pcbProximo.setState(Estado.EXECUTANDO);
				this.pcbExecutando = pcbProximo;
				if (!this.terminados.contains(pcbAtual)) {
					//preemptivo vai pro esperando ou pro pronto?
					pcbAtual.setState(Estado.ESPERANDO);
				} else{
					pcbAtual.setState(Estado.TERMINADO);
				}
				this.trocasDeContexto++;
			}
		}
	}

	@Override
	// Assuma que 0 <= idDispositivo <= 4
	protected OperacaoES proximaOperacaoES(int idDispositivo) {
		// TODO Auto-generated method stub
		PCB pcbAtual = pcbExecutando;
		for(OperacaoES operacao : pcbAtual.operacoesES) {
			if(operacao.ciclos <= 0){
				pcbAtual.operacoesES.remove(operacao);
			} else{
				return operacao;
			}
		}
		return null;
	}

	@Override
	protected Operacao proximaOperacaoCPU() {
		// TODO Auto-generated method stub
		PCB pcbAtual = pcbExecutando;
		if(pcbAtual.operacoesCPU.size() > 0){
			Operacao ops = pcbAtual.operacoesCPU.get(0);
			pcbAtual.operacoesCPU.remove(ops);
			return ops;
		}
		return null;
	}

	@Override
	protected void executaCicloKernel() {
		// TODO Auto-generated method stub
		if(this.escalonadorEscolhido.equals(Escalonador.SHORTEST_JOB_FIRST)){
			//ordena a lista total de processos

			if(this.processos.size() > 0){
				//saber quem é quem
				this.prontos = this.processos.stream().filter((x) -> x.estado.equals(Estado.PRONTO) || ((x.estado.equals(Estado.NOVO) && x.contadorDePrograma > 0) || (x.estado.equals(Estado.ESPERANDO) && x.operacoesES.size() == 0))).collect(Collectors.toList());
				this.esperando = this.processos.stream().filter((x) -> x.estado.equals(Estado.ESPERANDO) || (x.operacoesES.size() > 0 && (x.estado.equals(Estado.PRONTO) || x.estado.equals(Estado.EXECUTANDO)))).collect(Collectors.toList());
				this.prontos.removeAll(esperando);
				this.terminados = this.processos.stream().filter((x) -> x.estado.equals(Estado.TERMINADO) || (x.operacoesES.size() <= 0 && x.operacoesCPU.size() <= 0)).collect(Collectors.toList());
				this.esperando.removeAll(terminados);
				this.prontos.removeAll(terminados);
				this.terminados.forEach((x) -> x.setState(Estado.TERMINADO));
				this.esperando.forEach((x) -> x.setState(Estado.ESPERANDO));
				this.prontos.forEach((x) -> x.setState(Estado.PRONTO));

				//quem vai ser executado
				ArrayList<PCB> podeSerExecutado = new ArrayList<>();
				podeSerExecutado.addAll(this.prontos);
				podeSerExecutado.addAll(this.esperando);

				Collections.sort(podeSerExecutado, new PCB.shortestJobFirst());
				if(podeSerExecutado.size() > 0){
					PCB atual = pcbExecutando;
					//mudar as propriedades do processo sendo executado
					this.trocaContexto(atual, podeSerExecutado.get(0));
				}
			}
		} else if(this.escalonadorEscolhido.equals(Escalonador.FIRST_COME_FIRST_SERVED)){
			if(this.processos.size() > 0){
				//saber quem é quem
				this.prontos = this.processos.stream().filter((x) -> x.estado.equals(Estado.PRONTO) || ((x.estado.equals(Estado.NOVO) && x.contadorDePrograma > 0) || (x.estado.equals(Estado.ESPERANDO) && x.operacoesES.size() == 0))).collect(Collectors.toList());
				this.prontos.forEach((x) -> x.setState(Estado.PRONTO));
				this.esperando = this.processos.stream().filter((x) -> x.estado.equals(Estado.ESPERANDO) || (x.operacoesES.size() > 0 && (x.estado.equals(Estado.PRONTO) || x.estado.equals(Estado.EXECUTANDO)))).collect(Collectors.toList());
				this.prontos.removeAll(esperando);
				this.esperando.forEach((x) -> x.setState(Estado.ESPERANDO));
				this.terminados = this.processos.stream().filter((x) -> x.estado.equals(Estado.TERMINADO) || (x.operacoesES.size() <= 0 && x.operacoesCPU.size() <= 0)).collect(Collectors.toList());
				this.esperando.removeAll(terminados);
				this.terminados.forEach((x) -> x.setState(Estado.TERMINADO));

				//quem vai ser executado
				ArrayList<PCB> podeSerExecutado = new ArrayList<>();
				podeSerExecutado.addAll(this.prontos);
				podeSerExecutado.addAll(this.esperando);

				Collections.sort(podeSerExecutado, new PCB.firstComeFirstServed());
				if(podeSerExecutado.size() > 0){
					PCB atual = pcbExecutando;
					//mudar as propriedades do processo sendo executado
					this.trocaContexto(atual, podeSerExecutado.get(0));
				}
			}
		} else if(this.escalonadorEscolhido.equals(Escalonador.SHORTEST_REMANING_TIME_FIRST)){
			if(this.processos.size() > 0){
				//saber quem é quem
				this.prontos = this.processos.stream().filter((x) -> x.estado.equals(Estado.PRONTO) || ((x.estado.equals(Estado.NOVO) && x.contadorDePrograma > 0) || (x.estado.equals(Estado.ESPERANDO) && x.operacoesES.size() == 0))).collect(Collectors.toList());
				this.prontos.forEach((x) -> x.setState(Estado.PRONTO));
				this.esperando = this.processos.stream().filter((x) -> x.estado.equals(Estado.ESPERANDO) || (x.operacoesES.size() > 0 && (x.estado.equals(Estado.PRONTO) || x.estado.equals(Estado.EXECUTANDO)))).collect(Collectors.toList());
				this.prontos.removeAll(esperando);
				this.esperando.forEach((x) -> x.setState(Estado.ESPERANDO));
				this.terminados = this.processos.stream().filter((x) -> x.estado.equals(Estado.TERMINADO) || (x.operacoesES.size() <= 0 && x.operacoesCPU.size() <= 0)).collect(Collectors.toList());
				this.esperando.removeAll(terminados);
				this.terminados.forEach((x) -> x.setState(Estado.TERMINADO));

				//quem vai ser executado
				ArrayList<PCB> podeSerExecutado = new ArrayList<>();
				podeSerExecutado.addAll(this.prontos);
				podeSerExecutado.addAll(this.esperando);

				Collections.sort(podeSerExecutado, new PCB.shortestRemainingTimeFirst());
				if(podeSerExecutado.size() > 0){
					PCB atual = pcbExecutando;
					//mudar as propriedades do processo sendo executado
					this.trocaContexto(atual, podeSerExecutado.get(0));
				}
			}
		} else if(this.escalonadorEscolhido.equals(Escalonador.ROUND_ROBIN_QUANTUM_5)){
			if(this.processos.size() > 0){
				//saber quem é quem
				this.prontos = this.processos.stream().filter((x) -> x.estado.equals(Estado.PRONTO) || ((x.estado.equals(Estado.NOVO) && x.contadorDePrograma > 0) || (x.estado.equals(Estado.ESPERANDO) && x.operacoesES.size() == 0))).collect(Collectors.toList());
				this.prontos.forEach((x) -> x.setState(Estado.PRONTO));
				this.esperando = this.processos.stream().filter((x) -> x.estado.equals(Estado.ESPERANDO) || (x.operacoesES.size() > 0 && (x.estado.equals(Estado.PRONTO) || x.estado.equals(Estado.EXECUTANDO)))).collect(Collectors.toList());
				this.prontos.removeAll(esperando);
				this.esperando.forEach((x) -> x.setState(Estado.ESPERANDO));
				this.terminados = this.processos.stream().filter((x) -> x.estado.equals(Estado.TERMINADO) || (x.operacoesES.size() <= 0 && x.operacoesCPU.size() <= 0)).collect(Collectors.toList());
				this.esperando.removeAll(terminados);
				this.terminados.forEach((x) -> x.setState(Estado.TERMINADO));

				//quem vai ser executado
				ArrayList<PCB> podeSerExecutado = new ArrayList<>();
				podeSerExecutado.addAll(this.prontos);
				podeSerExecutado.addAll(this.esperando);

				//round robin não tem
				//Collections.sort(podeSerExecutado, new PCB.shortestJobFirst());
				if(podeSerExecutado.size() > 0){
					PCB atual = pcbExecutando;
					//mudar as propriedades do processo sendo executado
					this.trocaContexto(atual, podeSerExecutado.get(0));
				}
			}
		} else{
			throw new RuntimeException("O escalonador não tem um tipo especificado...");
		}
		for (PCB processo : this.processos) {
			processo.contadorDePrograma += 1;
			processo.burst += 1;
			if(processo.estado.equals(Estado.NOVO) || processo.estado.equals(Estado.PRONTO)) processo.tempoDeResposta += 1;
			processo.burstRestante = (processo.chuteBurst - processo.burst < 0 ? 0 : processo.chuteBurst - processo.burst);
			processo.tempoDeRetorno = processo.tempoDeResposta + processo.tempoDeEspera;
		}
		//setta nova média de chute pro burst
		Integer nextBurst = 0;
		for (PCB processo : this.terminados) {
			nextBurst += processo.burst;
		}
		if(this.terminados.size() > 0){
			this.chuteBurst = nextBurst / this.terminados.size();
		}
	}

	@Override
	protected boolean temTarefasPendentes() {
		// TODO Auto-generated method stub
		return this.terminados.size() != this.processos.size();
	}

	@Override
	protected Integer idProcessoNovo() {
		// TODO Auto-generated method stub
		return this.processos.size();
	}

	@Override
	protected List<Integer> idProcessosProntos() {
		// TODO Auto-generated method stub
		return this.prontos.stream().map(PCB::getId).collect(Collectors.toList());
	}

	@Override
	protected Integer idProcessoExecutando() {
		// TODO Auto-generated method stub
		return this.pcbExecutando.idProcesso;
	}

	@Override
	protected List<Integer> idProcessosEsperando() {
		// TODO Auto-generated method stub
		return this.esperando.stream().map(PCB::getId).collect(Collectors.toList());
	}

	@Override
	protected List<Integer> idProcessosTerminados() {
		// TODO Auto-generated method stub
		return this.terminados.stream().map(PCB::getId).collect(Collectors.toList());
	}

	@Override
	protected int tempoEsperaMedio() {
		// TODO Auto-generated method stub
		int total = 0;
		for(PCB processo : processos) {
			total += processo.tempoDeEspera;
		}
		return total/processos.size();
	}

	@Override
	protected int tempoRespostaMedio() {
		// TODO Auto-generated method stub
		int total = 0;
		for(PCB processo : processos) {
			total += processo.tempoDeResposta;
		}
		return total/processos.size();
	}

	@Override
	protected int tempoRetornoMedio() {
		// TODO Auto-generated method stub
		int total = 0;
		for(PCB processo : processos) {
			total += processo.tempoDeRetorno;
		}
		return total/processos.size();
	}

	@Override
	protected int trocasContexto() {
		// TODO Auto-generated method stub
		return this.trocasDeContexto;
	}

	@Override
	public void defineEscalonador(Escalonador e) {
		// TODO Auto-generated method stub
		this.escalonadorEscolhido = e;
	}
}
