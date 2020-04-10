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

public class Agente extends Agent {

    float conta = 0;
    String conversa;
    int reduzir = 0;

    float[] carga_fixa = new float[24];
    float[] carga_movel = new float[24];

    protected void setup() {
        System.out.println(getAID().getName() + " entrou na rede.");

        Object[] args = getArguments();
        for (int i = 0; i < 24; i++) {
            float arg = Float.parseFloat((String) args[i]);
            carga_fixa[i] = arg;
        }
        System.out.println(Arrays.toString(carga_fixa));

        for (int i = 24; i < 48; i++) {
            float arg = Float.parseFloat((String) args[i]);
            carga_movel[i - 24] = arg;
        }
        System.out.println(Arrays.toString(carga_movel));

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("rede");
        sd.setName("Smartgrid");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new DemandaOfertaServidor());
    }

    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println(getAID().getName() + " saiu da rede.");
    }

    private class DemandaOfertaServidor extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {

                conversa = msg.getConversationId();
                String Mensagem = msg.getContent();

                if (conversa == "finaliza-rede") {
                    //doDelete();
                    System.out.println(getAID().getName() + " saiu da rede.");
                } else if (conversa == "verifica-demanda") {
                    Integer i = Integer.parseInt(Mensagem);
                    ACLMessage resposta = msg.createReply();

                    Float consumo;
                    consumo = 0f;
                    if (reduzir == 1) {
                        //consumir apenas as cargas fixas e shiftar vetor de cargas moveis
                        consumo = (Float) carga_fixa[i];
                        float aux = carga_movel[carga_movel.length - 1];
                        for (int j = carga_movel.length - 1; j > 0; j--) {
                            carga_movel[j] = carga_movel[j - 1];
                        }
                        carga_movel[0] = aux;
                    } else if (reduzir == 0) {
                        consumo = (Float) carga_fixa[i] + (Float) carga_movel[i];
                    }

                    if (consumo != null) {
                        resposta.setPerformative(ACLMessage.PROPOSE);
                        resposta.setContent(String.valueOf(consumo.floatValue()));
                    } else {
                        resposta.setPerformative(ACLMessage.REFUSE);
                        resposta.setContent("Nao disponivel");
                    }

                    myAgent.send(resposta);
                    
                } else if (conversa == "verifica-demanda-futura") {
                    Integer i = Integer.parseInt(Mensagem);
                    ACLMessage resposta = msg.createReply();

                    if (i==23){
                        i=-1;
                    }
                    
                    Float consumo;
                    consumo = 0f;

                        consumo = (Float) carga_fixa[i+1] + (Float) carga_movel[i+1];

                    if (consumo != null) {
                        resposta.setPerformative(ACLMessage.PROPOSE);
                        resposta.setContent(String.valueOf(consumo.floatValue()));
                    } else {
                        resposta.setPerformative(ACLMessage.REFUSE);
                        resposta.setContent("Nao disponivel");
                    }

                    myAgent.send(resposta);
                    
                    
                } else if (conversa == "reduzir-consumo") {

                    Integer i = Integer.parseInt(Mensagem);
                    if (i == 1) {
                        reduzir = 1;
                    } else if (i == 0) {
                        reduzir = 0;
                    }
                }
            } else {
                block();
            }
        }
    }
}