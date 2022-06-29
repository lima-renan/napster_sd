package p2p;

import java.io.IOException;
import java.net.DatagramSocket;
import java.util.HashMap;


public class Servidor {

    public static void main(String[] args){
        try {

            DatagramSocket serverSocket;
            serverSocket = new DatagramSocket(10098); //porta defualt para cominicação com os peers
            HashMap<String, String> recebidas = new HashMap<>(); // Cria um HashMap para controlar as mensagens recebidas

            //O Servidor permanece funcionando
            while(true){

                Mensagem.setRecebidas(serverSocket, recebidas); //Trata as mensagens recebidas e envia o ACK para o cliente

            }

        } catch (IOException e) {
            //Imprime o erro
            e.printStackTrace();
        }

    }

}
