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
            HashMap<InetAddress, Integer> ip_port_peers = new HashMap<InetAddress, Integer>(); // Cria um HashMap vazio para registrar os Ips e portas dos peers

            // O Servidor permanece funcionando
            while(true){
               Mensagem.sendACK(serverSocket,ip_port_peers);

            }

        } catch (IOException e) {
            //Imprime o erro
            e.printStackTrace();
        }

    }

}
