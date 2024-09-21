
//grupo 03:
// Isaias Elias da Silva
// Jardel Santos Nascimento
// Sergio Santana dos Santos


package br.ufs.dcomp.chat;

import com.rabbitmq.client.*;
import com.google.protobuf.*;
import java.util.*;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Chat {

    public static void main(String[] argv) throws Exception {
        String queueName;
        String queueNameFile;
        
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername("admin"); 
        factory.setPassword("password"); 
        factory.setVirtualHost("/");
        factory.setHost("192.168.1.252"); 
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        Channel channelFile = connection.createChannel();
        
        MensagemProto.Mensagem.Builder mensagem = MensagemProto.Mensagem.newBuilder();
        
        System.out.print("User: ");
        Scanner s = new Scanner(System.in);
        queueName = s.nextLine();
        queueNameFile = queueName.concat("-files");
 
        channel.queueDeclare(queueName, false, false, false, null);
        channelFile.queueDeclare(queueNameFile, false, false, false, null);
        
        Consumer consumer = new DefaultConsumer(channel) {
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                MensagemProto.Mensagem message = MensagemProto.Mensagem.parseFrom(body);
                String emissor = message.getEmissor();
                String data = message.getData();
                String hora = message.getHora();
                String grupo = message.getGrupo();
                String cont = message.getCorpo().toStringUtf8();
                System.out.println("(" + data + " às " + hora +") " + emissor + (grupo.isEmpty() ? "" : grupo) + " diz: " + cont);
            }
        };

        channel.basicConsume(queueName, true, consumer);

        DateFormat dateTime = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        String queueKey = "", groupName, groupNameFile;
        System.out.print(">> ");
        
        boolean loop = true;
        while (loop) {
            String msg = s.nextLine();
            
            if (msg.startsWith("!listGroups")) {
                // Chama RESTClient para listar grupos
                String path = "/api/queues/wpocetio/" + queueName + "/bindings";
                RESTClient restClient = new RESTClient(path);
                String jsonResponse = restClient.getData();
                RESTClient.readData(jsonResponse);
                System.out.print(queueKey + ">> ");
            }

            if (msg.startsWith("!listUsers")) {
                // Chama RESTClient para listar usuários de um grupo
                String group = msg.substring(11);
                String path = "/api/exchanges/wpocetio/" + group + "/bindings/source";
                RESTClient restClient = new RESTClient(path);
                String jsonResponse = restClient.getData();
                RESTClient.readData(jsonResponse);
                System.out.print(queueKey + ">> ");
            }

            if (msg.equals("!quit")) {
                loop = false;
            }
            // Processa outras mensagens ou comandos
        }
        System.exit(0);
    }
}
