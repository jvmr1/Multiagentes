package multiagentes;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import java.util.*;

public class Concessionaria extends Agent {

    private Integer Hora = 0;
    private Integer Dia = 1;
    private AID[] Agentes;
    private Hashtable Contas;
    private Hashtable ContasAux;
    private float demandaMin;
    private float demandaMax;
    private float demandaVar;
    private float demandaSum = 0;
    private float demandaMed;
    private float tarifa;
    private int sinal;
    private int caso;

    @Override
    protected void setup() {
        System.out.println("Rede iniciada");

        Contas = new Hashtable();
        ContasAux = new Hashtable();

        Object[] args = getArguments();
        caso = Integer.parseInt((String) args[0]);

        addBehaviour(new TickerBehaviour(this, 10) {
            @Override
            protected void onTick() {
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("rede");
                template.addServices(sd);
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    Agentes = new AID[result.length];
                    for (int i = 0; i < result.length; ++i) {
                        Agentes[i] = result[i].getName();
                    }
                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }

                myAgent.addBehaviour(new VerificaDemandaAtual());
                if (caso == 1) {
                    myAgent.addBehaviour(new VerificaDemandaFutura());
                }

            }
        });
    }

    @Override
    protected void takeDown() {
        System.out.println("Rede encerrada");
    }

    private class VerificaDemandaAtual extends Behaviour {

        private float demandaTotal = 0;
        private int repliesCnt = 0;
        private MessageTemplate mt;
        private int step = 0;

        @Override
        public void action() {
            switch (step) {
                case 0:
                    ACLMessage dem = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < Agentes.length; ++i) {
                        dem.addReceiver(Agentes[i]);
                    }
                    dem.setContent(Integer.toString(Hora));
                    dem.setConversationId("verifica-demanda");
                    dem.setReplyWith("cfp" + System.currentTimeMillis());
                    myAgent.send(dem);

                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("verifica-demanda"),
                            MessageTemplate.MatchInReplyTo(dem.getReplyWith()));
                    step = 1;

                case 1:
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        float consumo = 0;

                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            consumo = Float.parseFloat(reply.getContent());
                            demandaTotal = demandaTotal + consumo;
                            
                            String nome_sender = reply.getSender().getName();
                            ContasAux.put(nome_sender, consumo);

                        }

                        repliesCnt++;
                        if (repliesCnt >= Agentes.length) {
                            System.out.println(demandaTotal);
                            demandaSum = demandaSum + demandaTotal;

                            //calcular a tarifa com base na demanda total
                            //1. descobrir consumo minimo e maximo
                            if (Dia == 1 && Hora == 0) {
                                demandaMin = demandaTotal;
                                demandaMax = demandaTotal;
                            } else if (demandaTotal < demandaMin) {
                                demandaMin = demandaTotal;
                            } else if (demandaTotal > demandaMax) {
                                demandaMax = demandaTotal;
                            }
                            demandaVar = demandaMax - demandaMin;

                            if (demandaTotal >= demandaMin && demandaTotal < (demandaMin + 1.0 / 3.0 * demandaVar)) {
                                tarifa = (float) 0.4436;
                            } else if (demandaTotal >= (demandaMin + 1.0 / 3.0 * demandaVar) && demandaTotal < (demandaMin + 2.0 / 3.0 * demandaVar)) {
                                tarifa = (float) 0.77734;
                            } else if (demandaTotal >= (demandaMin + 2.0 / 3.0 * demandaVar) && demandaTotal <= (demandaMin + demandaVar)) {
                                tarifa = (float) 1.23864;
                            }
                            
                            
                            Set<String> nomes = ContasAux.keySet();

                            for (String nome : nomes) {

                                float consumo_aux = (float) ContasAux.get(nome);

                                float aux;
                                if (Contas.containsKey(nome)) {
                                    aux = (float) Contas.get(nome);
                                } else {
                                    aux = 0;
                                }
                                Contas.put(nome, (aux + consumo_aux * tarifa));
                            }

                            Hora = Hora + 1;
                            if (Hora > 23) {
                                Hora = 0;
                                Dia = Dia + 1;
                            }
                            if (Dia > 30) {
                                // envia ordem de finalizar para todos os agentes
                                ACLMessage fim = new ACLMessage(ACLMessage.CFP);
                                for (int i = 0; i < Agentes.length; ++i) {
                                    fim.addReceiver(Agentes[i]);
                                    String nome_agente = Agentes[i].getName();
                                    float valor = (float) Contas.get(nome_agente);
                                    System.out.println(nome_agente + " tem que pagar: R$" + valor);
                                }
                                fim.setContent("fim");
                                fim.setConversationId("finaliza-rede");
                                fim.setReplyWith("fim" + System.currentTimeMillis());
                                myAgent.send(fim);

                                mt = MessageTemplate.and(MessageTemplate.MatchConversationId("finaliza-rede"),
                                        MessageTemplate.MatchInReplyTo(fim.getReplyWith()));

                                step = 3;
                                doDelete();
                                demandaMed = (float) (demandaSum / (30.0 * 24.0));
                                System.out.println("Demanda Media = " + demandaMed);

                                break;
                            }
                        }
                    } else {
                        block();
                    }
                    break;
            }
        }

        @Override
        public boolean done() {
            if (step == 3) {
                System.out.println("Verificacao da demanda encerrada");
            }

            return (step == 0);
        }
    }

    private class VerificaDemandaFutura extends Behaviour {

        private float demandaTotal = 0;
        private int repliesCnt = 0;
        private MessageTemplate mt;
        private int step = 0;

        @Override
        public void action() {
            switch (step) {
                case 0:
                    ACLMessage dem = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < Agentes.length; ++i) {
                        dem.addReceiver(Agentes[i]);
                    }
                    dem.setContent(Integer.toString(Hora));
                    dem.setConversationId("verifica-demanda-futura");
                    dem.setReplyWith("cfp" + System.currentTimeMillis());
                    myAgent.send(dem);

                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("verifica-demanda-futura"),
                            MessageTemplate.MatchInReplyTo(dem.getReplyWith()));
                    step = 1;

                case 1:
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        float consumo = 0;

                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            consumo = Float.parseFloat(reply.getContent());
                            demandaTotal = demandaTotal + consumo;
                        }

                        repliesCnt++;
                        if (repliesCnt >= Agentes.length) {

                            if (demandaTotal >= demandaMin && demandaTotal < (demandaMin + 1.0 / 3.0 * demandaVar)) {

                                //mandar voltar a consumir as cargas moveis
                                ACLMessage sng = new ACLMessage(ACLMessage.CFP);
                                for (int i = 0; i < Agentes.length; ++i) {
                                    sng.addReceiver(Agentes[i]);
                                }
                                sinal = 0;
                                sng.setContent(Integer.toString(sinal));
                                sng.setConversationId("reduzir-consumo");
                                sng.setReplyWith("sng" + System.currentTimeMillis());
                                myAgent.send(sng);

                                mt = MessageTemplate.and(MessageTemplate.MatchConversationId("reduzir-consumo"),
                                        MessageTemplate.MatchInReplyTo(sng.getReplyWith()));

                            } else if (demandaTotal >= (demandaMin + 2.0 / 3.0 * demandaVar) && demandaTotal <= (demandaMin + demandaVar)) {
                                //mandar parar de consumir as cargas moveis
                                ACLMessage sng = new ACLMessage(ACLMessage.CFP);
                                for (int i = 0; i < Agentes.length; ++i) {
                                    sng.addReceiver(Agentes[i]);
                                }
                                sinal = 1;
                                sng.setContent(Integer.toString(sinal));
                                sng.setConversationId("reduzir-consumo");
                                sng.setReplyWith("sng" + System.currentTimeMillis());
                                myAgent.send(sng);

                                mt = MessageTemplate.and(MessageTemplate.MatchConversationId("reduzir-consumo"),
                                        MessageTemplate.MatchInReplyTo(sng.getReplyWith()));
                            }

                        }
                    } else {
                        block();
                    }
                    break;
            }
        }

        @Override
        public boolean done() {
            if (step == 3) {
                System.out.println("Verificacao da demanda encerrada");
            }

            return (step == 0);
        }
    }

}
