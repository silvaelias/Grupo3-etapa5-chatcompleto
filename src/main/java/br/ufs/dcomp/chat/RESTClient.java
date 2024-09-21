package br.ufs.dcomp.chat;

import com.google.gson.*;
import java.io.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class RESTClient {

    private String path;

    public RESTClient(String path) {
        this.path = path;
    }

    public String getData() {
        String json = new String();
        
        try {
            // Configuração da autenticação
            String username = "admin";
            String password = "password";

            String usernameAndPassword = username + ":" + password;
            String authorizationHeaderName = "Authorization";
            String authorizationHeaderValue = "Basic " + java.util.Base64.getEncoder().encodeToString(usernameAndPassword.getBytes());
     
            // URL base do serviço REST
            String restResource = "https://192.168.1.252";

            // Configura o cliente HTTP para fazer a requisição
            Client client = ClientBuilder.newClient();
            Response resposta = client.target(restResource)
                .path(this.path)
                .request(MediaType.APPLICATION_JSON)
                .header(authorizationHeaderName, authorizationHeaderValue)
                .get(); // Executa o GET
            
            if (resposta.getStatus() == 200) {
                json = resposta.readEntity(String.class);
            } else {
                System.out.println("Falha ao obter dados. Status: " + resposta.getStatus());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    public static void readData(String json) {
        try {
            JsonArray saida = (new Gson()).fromJson(json, JsonArray.class);
            for (int i = 0; i < saida.size(); i++) {
                System.out.println(saida.get(i).getAsJsonObject().toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
