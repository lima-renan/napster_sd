package p2p;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;


public class Servidor {

    public static void main(String[] args){
        try {

            InetAddress addr = InetAddress.getByName("127.0.0.1"); // Define o IP do servidor
            DatagramSocket serverSocket;
            serverSocket = new DatagramSocket(10098,addr); //porta default do servidor para comunicação com os peers
            HashMap<String, String> recebidas = new HashMap<>(); // Cria um HashMap para controlar as mensagens recebidas

            //O Servidor permanece funcionando
            while(true){
                System.out.println(serverSocket.getLocalAddress().getHostAddress());
                Mensagem.setRecebidas(serverSocket, recebidas); //Trata as mensagens recebidas e envia o ACK para o cliente

            }

        } catch (IOException e) {
            //Imprime o erro
            e.printStackTrace();
        }

    }

}
