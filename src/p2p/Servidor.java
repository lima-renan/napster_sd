package p2p;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;


public class Servidor {

    public static void main(String[] args) {
        try {

            InetAddress addr = InetAddress.getByName("127.0.0.1"); // Define o IP do servidor
            DatagramSocket serverSocket;
            serverSocket = new DatagramSocket(10098, addr); //porta padrão do servidor para comunicação com os peers
            HashMap<String, Integer> ip_port_peers = new HashMap<>(); //Cria um HashMap vazio para registrar os Ips e portas dos peers, onde a chave é o IP.
            HashMap<String, ArrayList<String>> files_peers = new HashMap<>(); // Cria um HashMap vazio para registrar os arquivos dos peers, a chave é o nome do arquivo, para cada arquivo há um array com uma lista de IPs que representam os peers que possuem

            // O Servidor permanece funcionando
            while (true) {
                // configura o buffer para receber os datagramas
                byte[] recBuffer = new byte[1024];
                DatagramPacket recPkt = new DatagramPacket(recBuffer, recBuffer.length);
                serverSocket.receive(recPkt); //BLOCKING - Recebe o datagrama
                new ClientThread(serverSocket, recPkt, ip_port_peers, files_peers); // A cada datagrama recebido, uma thread é gerada para o atendimento
            }

        } catch (IOException e) {
            //Imprime o erro
            e.printStackTrace();
        }

    }
}

    // Classe aninhada para atendimendo dos peers através de Threads
    class ClientThread extends Thread {

        DatagramSocket sock;
        DatagramPacket recPkt;
        HashMap<String, Integer> ip_port_peers;
        HashMap<String, ArrayList<String>> files_peers;

        public ClientThread(DatagramSocket sock,DatagramPacket recPkt,  HashMap<String, Integer> ip_port_peers, HashMap<String, ArrayList<String>> files_peers) {
            this.sock = sock;
            this.recPkt = recPkt;
            this.ip_port_peers = ip_port_peers;
            this.files_peers = files_peers;
            this.start();
        }

        public void run() {
            // As threads prestam atendimento aos peers, em caso de erro é exibida uma exceção
            try{
                Mensagem.sendACK(sock,recPkt, ip_port_peers, files_peers);
            }
            catch(SocketException e){
                System.out.println("Socket: " + e.getMessage());
            }
            catch(IOException e){
                System.out.println("IO: " + e.getMessage());
            }

        }
    }


