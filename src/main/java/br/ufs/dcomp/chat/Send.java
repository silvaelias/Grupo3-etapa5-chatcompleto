package br.ufs.dcomp.chat;

import com.rabbitmq.client.*;
import com.google.protobuf.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Send {
    private static String emissor;
    private static String data;
    private static String hora;
    private static String chave;
    private static String path;
    
    public Send(String emissor, String data, String hora, String chave, String wae) {
        this.emissor = emissor;
        this.data = data;
        this.hora = hora;
        this.chave = chave;
        this.path = wae;
    }
    
    public static String getEmissor() {
        return emissor;
    }
    
    public static String getData() {
        return data;
    }
    
    public static String getHora() {
        return hora;
    }
    
    public static String getChave() {
        return chave;
    }
 
    public static String getPath() {
        return path;
    }
    
    private static Runnable envio = new Runnable() {
        @Override
        public void run() {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setUsername("admin"); 
            factory.setPassword("cl0ud$"); 
            factory.setVirtualHost("/");
            factory.setHost("networkbalancerrabbitmq-1d605ec203675c51.elb.us-east-1.amazonaws.com"); 
            
            try {    
                Connection connection = factory.newConnection();
                Channel canal = connection.createChannel();
                
                String[] splitWae = getPath().split("\\p{Punct}");
                int i = splitWae.length;
                String arq = splitWae[i-2].concat(".").concat(splitWae[i-1]);
                String chaveArquivo = getChave().substring(1).concat("-files");
                
                File file = new File(getPath());
                int len = (int)file.length();
                byte[] arquivo = new byte[len];
                FileInputStream input  = new FileInputStream(file);
                input.read(arquivo);
                input.close();
                
                MensagemProto.Mensagem.Builder mensagem = MensagemProto.Mensagem.newBuilder();
                    
                mensagem.setEmissor(getEmissor());
                mensagem.setData(getData());
                mensagem.setHora(getHora());
                if(getChave().startsWith("#"))
                    mensagem.setGrupo(getChave());
                else
                    mensagem.setGrupo("");

                mensagem.setCorpo(ByteString.copyFrom(arquivo));
                mensagem.setNome(arq);
                
                MensagemProto.Mensagem wrapper = mensagem.build();
                byte[] buffer = wrapper.toByteArray();
                    
                if (getChave().startsWith("@")){
                    canal.basicPublish("", chaveArquivo, null, buffer);
                    System.out.println("Arquivo \"" + getPath() + "\" foi enviado para " + getChave());
                    System.out.print(getChave() + ">> ");
                }
                else {
                    canal.basicPublish(chaveArquivo, "", null, buffer);
                    System.out.println("Arquivo \"" + getPath() + "\" foi enviado para " + getChave());
                    System.out.print(getChave() + ">> ");
                }
            }
            catch (Exception e){
                System.out.println(e.getMessage() + "\nFalha. Tente outra vez.");
                System.out.print(getChave() + ">> ");
            }
        }
    };
    
    public static void main(String[] args){
        new Thread(envio).start();
    }
}

