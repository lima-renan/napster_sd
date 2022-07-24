package p2p;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Servidor {

    public static void main(String[] args) {
        try {

            InetAddress addr = InetAddress.getByName("127.0.0.1"); // Define o IP do servidor
            DatagramSocket serverSocket;
            serverSocket = new DatagramSocket(10098, addr); //porta padrão do servidor para comunicação com os peers
            List<AbstractMap.SimpleEntry<String, String>> ip_port_peers = new CopyOnWriteArrayList<>(); //Cria uma lista vazia para registrar os Ips e portas dos peers.
            ConcurrentHashMap<String, ArrayList<String>> files_peers = new ConcurrentHashMap<>(); // Cria um ConcurrentHashMap vazio para registrar os arquivos dos peers, a chave é o nome do arquivo, para cada arquivo há um array com uma lista de IPs que representam os peers que possuem

            // O Servidor permanece funcionando
            while (true) {
                // configura o buffer para receber os datagramas
                byte[] recBuffer = new byte[1024];
                DatagramPacket recPkt = new DatagramPacket(recBuffer, recBuffer.length);
                serverSocket.receive(recPkt); //BLOCKING - Recebe o datagrama
                new ServerThread(serverSocket, recPkt, ip_port_peers, files_peers); // A cada datagrama recebido, uma thread é gerada para o atendimento
            }

        } catch (IOException e) {
            //Imprime o erro
            e.printStackTrace();
        }

    }
}

    // Classe aninhada para atendimendo dos peers através de Threads
    class ServerThread extends Thread {

        DatagramSocket sock; // Armazena o socket do servidor
        DatagramPacket recPkt; // Armazena o packet que será enviado ao cliente
        List<AbstractMap.SimpleEntry<String, String>> ip_port_peers; // Armazena os ips e portas dos peers
        ConcurrentHashMap<String, ArrayList<String>> files_peers; // Armazena os arquivos dos peers

        public ServerThread(DatagramSocket sock,DatagramPacket recPkt,  List<AbstractMap.SimpleEntry<String, String>> ip_port_peers, ConcurrentHashMap<String, ArrayList<String>> files_peers) {
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


