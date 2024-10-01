
//grupo 03:
// Isaias Elias da Silva
// Jardel Santos Nascimento
// Sergio Santana dos Santos


package br.ufs.dcomp.chat;

import com.rabbitmq.client.*;
import com.google.protobuf.*;
import com.google.gson.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Chat {

    public static void main(String[] argv) throws Exception {
        String queueName;
        String queueNameFile;
         
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername("admin"); 
        factory.setPassword("cl0ud$"); 
        factory.setVirtualHost("/");
        factory.setHost("networkbalancerrabbitmq-1d605ec203675c51.elb.us-east-1.amazonaws.com"); 
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        Channel channelFile = connection.createChannel();
        
        MensagemProto.Mensagem.Builder mensagem = MensagemProto.Mensagem.newBuilder();
        
        System.out.print("User: ");
        Scanner s = new Scanner(System.in);
        queueName = s.nextLine();
        queueNameFile = queueName.concat("-files");
 
                          //(queue-name,    durable,  exclusive, auto-delete, params); 
        channel.queueDeclare(queueName,     false,    false,     false,       null);
        channelFile.queueDeclare(queueNameFile, false,    false,     false,       null);
        
        Consumer consumer = new DefaultConsumer(channel) {
            
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)           
                throws IOException {
                    try {   
                        MensagemProto.Mensagem message = MensagemProto.Mensagem.parseFrom(body);
                        
                        String emissor = message.getEmissor();
                        String data = message.getData();
                        String hora = message.getHora();
                        String grupo = message.getGrupo();
                        String cont = message.getCorpo().toStringUtf8();
                        
                        if (grupo.isEmpty()) 
                            System.out.println("(" + data + " às " + hora +") " +  emissor + " diz: " + cont);
                        else 
                            System.out.println("(" + data + " às " + hora +") " +  emissor + grupo + " diz: " + cont);
                    }
                    catch(IOException e){
                        System.out.println(e.getMessage());
                    }
            }
        };
        
        Consumer consumerFile = new DefaultConsumer(channelFile) {
            
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)           
                throws IOException {
                    try {   
                        MensagemProto.Mensagem message = MensagemProto.Mensagem.parseFrom(body);
                
                        String emissor = message.getEmissor();
                        String data = message.getData();
                        String hora = message.getHora();
                        String grupo = message.getGrupo();
                        String tipoMime = message.getTipo();
                        String nome = message.getNome();
                        byte[] conteudo = message.getCorpo().toByteArray();
                        
                        File dir = new File("chat/downloads/");
                        dir.mkdirs();
                        
                        File file = new File(dir, nome);
                        FileOutputStream saida = new FileOutputStream(file);
                        BufferedOutputStream out = new BufferedOutputStream(saida);
                        out.write(conteudo);
                        out.flush();
                        out.close();
                
                        System.out.println("(" + data + " às " + hora +") Arquivo \"" + nome + "\" recebido de " + emissor);
                    }
                    catch(IOException e){
                        System.out.println(e.getMessage());
                    }
            }
        };
                      //(queue-name, autoAck, consumer);    
        channel.basicConsume(queueName, true, consumer);
        channelFile.basicConsume(queueNameFile, true, consumerFile);
        
        DateFormat dateTime = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        boolean loop = true;
        String msg, queueKey = "", groupName = "", groupNameFile = "";
        System.out.print(">> ");
        
        while (loop) {
            try {
                msg = s.nextLine();
                if (msg.equals("") || msg.isEmpty())
                    System.out.print(queueKey + ">> ");
                if (msg.startsWith("@") || msg.startsWith("#")) { // @username ou #groupname
                    queueKey = msg;
                    System.out.print(queueKey + ">> ");
                }
                if (msg.startsWith("!newGroup")) { // !newGroup "grupo"
                    groupName = msg.substring(10);
                    groupNameFile = groupName.concat("-files");
                    channel.exchangeDeclare(groupName, "fanout");
                    channel.queueBind(queueName, groupName, "");
                    channelFile.exchangeDeclare(groupNameFile, "fanout");
                    channelFile.queueBind(queueNameFile, groupNameFile, "");
                    System.out.print(queueKey + ">> ");
                }
                if (msg.startsWith("!addUser")) { // !addUser user grupo
                    channel.queueBind(msg.split("\\s")[1], msg.split("\\s")[2], ""); 
                    channelFile.queueBind(msg.split("\\s")[1].concat("-files"), msg.split("\\s")[2].concat("-files"), "");
                    System.out.print(queueKey + ">> ");
                }
                if (msg.startsWith("!delFromGroup")) { // !delFromGroup user grupo
                    channel.queueUnbind(msg.split("\\s")[1], msg.split("\\s")[2], ""); 
                    channelFile.queueUnbind(msg.split("\\s")[1].concat("-files"), msg.split("\\s")[2].concat("-files"), "");
                    System.out.print(queueKey + ">> ");
                }
                if (msg.startsWith("!removeGroup")) { // !removeGroup "grupo"
                    groupName = msg.substring(13);
                    groupNameFile = groupName.concat("-files");
                    channel.exchangeDelete(groupName);
                    channelFile.exchangeDelete(groupNameFile);
                    System.out.print(queueKey + ">> ");
                }
                if (msg.startsWith("!upload")) { // !upload "diretorio"
                    String url = msg.substring(8);
                    String cm = "";
                    if(url.startsWith("/"))
                        cm = url.substring(1);
                    else
                        cm = url;

                    Date data = new Date();
                    String dataS = dateTime.format(data);
                    String[] splitData = dataS.split("\\s");
                    
                    System.out.println("Enviando \"" + cm + "\" para " + queueKey + "...");
                    System.out.print(queueKey + ">> ");
                    Send envio = new Send(queueName, splitData[0], splitData[1], queueKey, cm);
                    envio.main(new String[]{});
                }
                if(msg.startsWith("!listGroups")){ // !listGroups
                    String user = queueKey.substring(1);
                 String path = "/api/queues///" + user + "/bindings";
                 RESTClient rest = new RESTClient(path);
                 rest.main(new String[]{});
                    System.out.print("\n" + queueKey + ">> ");
                }
                if(msg.startsWith("!listUsers")){ // !listUsers "grupo"
                    String group = msg.substring(11);
                    String path = "/api/exchanges///" + group + "/bindings/source";
                    RESTClient rest = new RESTClient(path);
                    rest.main(new String[]{});
                    System.out.print("\n" + queueKey + ">> ");
                }
                if(msg.equals("!quit")){
                    System.out.println("Obrigado! Até a próxima.");
                    loop = false;
                }
                if (!msg.substring(0,1).matches("\\p{Punct}")) {
                    if (queueKey.startsWith("@")) {
                        Date data = new Date();
                        String dataS = dateTime.format(data);
                        String[] splitData = dataS.split("\\s");
                    
                        mensagem.setEmissor(queueName);
                        mensagem.setData(splitData[0]);
                        mensagem.setHora(splitData[1]);
                        mensagem.setTipo("text/plain");
                        mensagem.setCorpo(ByteString.copyFromUtf8(msg));
                    
                        MensagemProto.Mensagem wrapper = mensagem.build();
                        byte[] buffer = wrapper.toByteArray();
                    
                        channel.basicPublish("", queueKey.substring(1), null, buffer);
                        System.out.print(queueKey + ">> ");
                    }
                    if (queueKey.startsWith("#")) {
                        Date data = new Date();
                        String dataS = dateTime.format(data);
                        String[] splitData = dataS.split("\\s");
                    
                        mensagem.setEmissor(queueName);
                        mensagem.setData(splitData[0]);
                        mensagem.setHora(splitData[1]);
                        mensagem.setGrupo(queueKey);
                        mensagem.setTipo("text/plain");
                        mensagem.setCorpo(ByteString.copyFromUtf8(msg));
                    
                        MensagemProto.Mensagem wrapper = mensagem.build();
                        byte[] buffer = wrapper.toByteArray();
                    
                        channel.basicPublish(queueKey.substring(1),"", null, buffer);
                        System.out.print(queueKey + ">> ");
                    }
                }
            }
            catch (IOException e) {
                System.out.println("Houve um erro, tente novamente!");
                if (queueKey.isEmpty())
                    System.out.print(">> ");
                else
                    System.out.print(queueKey + ">> ");
            }
        }
        System.exit(0);
    }
}
