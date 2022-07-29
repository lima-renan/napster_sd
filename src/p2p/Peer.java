package p2p;

import com.google.gson.Gson;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;





public class Peer {

    //Especificações do peer

    private String ip;
    private String port;
    private ArrayList<String> files;
    private String folder; //Endereço da pasta com os arquivos
    private Socket sock; //Socket que será utilizado para envio de mensagens TCP pelo peer
    public static volatile boolean running; //determina se o peer está em execução


    // Métodos setters e getters
    public String getIp(){ return this.ip;}

    public String getPort(){ return this.port;}

    public ArrayList<String> getFiles() {
        return this.files;
    }
    public String getFolder() {
        return this.folder;
    }
    public Socket getSock(){ return this.sock; }

    public void setIp(String ip) { this.ip = ip; }

    public void setPort(String port) { this.port = port; }

    public void setFiles (ArrayList<String>files) { this.files = files; }
    public void setFolder(String folder) { this.folder = folder; }
    public void setSock (Socket sock){ this.sock = sock;}


    public Peer (){

    }

    public Peer(String ip, String port) {
        this.setIp(ip);
        this.setPort(port);
    }

    public Peer(String ip, String port, String folder) {
        this.setIp(ip);
        this.setPort(port);
        this.setFolder(folder);
    }

    public Peer(String ip, String port, ArrayList<String> files, String folder) {
        this.setIp(ip);
        this.setPort(port);
        this.setFiles(files);
        this.setFolder(folder);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        running = true; //inicia o peer
        Peer peer = new Peer(); // Cria estrutura para receber especificações do peer e
        Mensagem.welcome(); // exibe mensagem de inicialização
        Mensagem.PeerConfig(peer); //
        DatagramSocket clientSocket = new DatagramSocket(Integer.parseInt(peer.getPort()),InetAddress.getByName(peer.getIp())); // Datagrama para conexão UDP com o Servidor
        ServerSocket peerTCP = new ServerSocket(Integer.parseInt(peer.getPort()),50,InetAddress.getByName(peer.getIp())); // inicializa o socket para recebimento de mensagens TCP
        byte[] recBuffer = new byte[1024]; // buffer de recebimento
        DatagramPacket recPkt = new DatagramPacket(recBuffer, recBuffer.length); // cria pacote de recebimento
        Thread reck = new PeerThreadReceiver(peer, clientSocket, recPkt); // Gera uma nova thread qd chega uma mensagem do servidor
        reck.setName("RECK");
        reck.start();
        Thread send = new PeerThreadSend(peer, clientSocket, "START"); // estabelelce a primeira conexão com o server
        send.join();
        Thread tcpCnt = new PeerThreadDownload(peerTCP, peer); //Thread para recebimento de solicitações de downloads de outros peers via TCP
        tcpCnt.setName("tcpConnect");
        tcpCnt.start();
        while (Peer.running) {
            Mensagem.menu(peer,clientSocket); //exibe o menu de opções: JOIN, SEARCH, DOWNLOAD E LEAVE - usa thread para enviar
        }


    }
}

    // Classe aninhada para comunicação com o Server e outros peers através de Threads
    class PeerThreadSend extends Thread {

        Peer peer; // Armazena as especificações do peer
        DatagramSocket clientSocket; // Armazena os dados do datagrama do peer
        String option; //opção de envio

        public PeerThreadSend(Peer peer, DatagramSocket clientSocket, String option) {
            // define as configurações da thread e inicia
            this.peer = peer;
            this.clientSocket = clientSocket;
            this.option = option;
            this.start();
        }

        public void run() {
            try {
                Mensagem.tryConect(peer, clientSocket, option);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

// Classe aninhada para comunicação com o Server e outros peers através de Threads
class PeerThreadReceiver extends Thread {

    Peer peer; // Armazena as especificações do peer
    DatagramSocket clientSocket; // Armazena os dados do datagrama do peer
    DatagramPacket recPacket; // Armazena os dados do pacote que será enviado pelo peer

    public PeerThreadReceiver(Peer peer, DatagramSocket clientSocket, DatagramPacket recPacket) {
        this.peer = peer;
        this.clientSocket = clientSocket;
        this.recPacket = recPacket;
    }

    public void run() {
        // A thread recebe mensagem ao servidor
        try {
            while(Peer.running) {
                Mensagem.ACKfromServer(peer, clientSocket, recPacket); //aguarda retorno do servidor
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}

// Thread para receber e tratar solicitações e retorno de download de outros peers
class PeerThreadDownload extends Thread {

    private ServerSocket peerSocket; // socket para recebimento de solicitações de Download
    private Peer peer; // detalhes do peer

    public PeerThreadDownload (ServerSocket peerSocket, Peer peer){
        this.peerSocket = peerSocket;
        this.peer = peer;
    }

    public void run() {
        //Ref.: https://www.baeldung.com/java-inputstream-server-socket
        try {
            while(Peer.running) {
                Socket node = peerSocket.accept();
                peer.setSock(node);
                int bufferSize = node.getReceiveBufferSize();
                DataInputStream in = new DataInputStream(new BufferedInputStream(node.getInputStream()));
                char dataType = in.readChar();
                int read;
                System.out.println(dataType);
                //System.out.println(in.readUTF());
                if (dataType == 's') { // se a mensagem enviada for uma string, deve-se validar se é DOWNLOAD_NEGADO ou uma solicitação de arquivo
                    int length = in.readInt();
                    byte[] msgByte = new byte[length]; //buffer que armazena os bytes da mensagem recebida
                    boolean end = false; //váriavel para controlar leitura dos bytes
                    StringBuilder dataString = new StringBuilder(length); //váriavel para gerar a string
                    int totalBytesRead = 0;
                    while (!end) {
                        int currentBytesRead = in.read(msgByte);
                        totalBytesRead = currentBytesRead + totalBytesRead;
                        if (totalBytesRead <= length) {
                            dataString
                                    .append(new String(msgByte, 0, currentBytesRead, StandardCharsets.UTF_8));
                        } else {
                            dataString
                                    .append(new String(msgByte, 0, length - totalBytesRead + currentBytesRead,
                                            StandardCharsets.UTF_8));
                        }
                        if (dataString.length() >= length) { //enquanto a quantidade de bytes lidos for menor que a de bytes declarados, continua
                            end = true;
                        }
                    }
                    Gson recgson = new Gson(); //instância para gerar a mensagem a partir string json do cliente
                    Mensagem msg = recgson.fromJson(dataString.toString(), Mensagem.class);  //gera a mensagem a partir da string json recebida do cliente
                    if (msg.getComment().equals("DOWNLOAD_NEGADO")) {
                        System.out.println("peer "+ msg.getIpPeer() + ":" + "[" + msg.getPortPeer() +"]" + "negou o download, pedindo agora para o peer [IP]:[porta]");
                    } else { //caso contrário verifica se o arquivo solicitado existe
                        File myFile = new File(peer.getFolder() +"/"+ msg.getComment()); // instancia arquivo a partir o nome recebido pelo outro peer
                        if (myFile.exists()) { //myFile.exists() && Math.random() < 0.5se o arquivo existir, sorteia para permitir o DOWNLOAD ou não
                            System.out.println("enviando arquivo: " + myFile.getName() + " da pasta " + myFile.getPath());
                            PeerThreadSendFileTCP sendFile = new PeerThreadSendFileTCP(peer,msg.getIpPeer(), Integer.parseInt(msg.getPortPeer()),myFile); //seta o ip e porta do peer remoto e envia a mensagem
                            sendFile.start(); // inicia a thread
                            sendFile.join(); // aguarda a conclusão
                        } else { //nega o download
                            PeerThreadSendStringTCP sendString = new PeerThreadSendStringTCP(peer,msg.getIpPeer(), Integer.parseInt(msg.getPortPeer()),"DOWNLOAD_NEGADO"); //seta o ip e porta do peer remoto e envia a mensagem
                            sendString.start(); // inicia a thread
                            sendString.join(); // aguarda a conclusão
                        }
                    }

                }else{ //if (dataType == 'f'){ // o peer remoto está enviando um arquivo
                    byte[] fileByte = new byte[bufferSize]; //buffer que armazena os bytes da mensagem recebida
                    String filename = in.readUTF();
                    System.out.println("Recebido!");
                    System.out.println(peer.getFolder()+ "/" + "teste");
                    OutputStream fos = new FileOutputStream(peer.getFolder()+ "/" + filename);
                    while ((read= in.read(fileByte)) >= 0){
                        fos.write(fileByte, 0, read);
                    }
                    fos.flush();
                    node.close();
                    if (node.getInputStream() != null) {
                        node.getInputStream().close();
                    }
                }
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

}

// Thread para enviar strings para outros peers
class PeerThreadSendStringTCP extends Thread {
    private Peer peer; // especificações do Peer
    private String ipSend; // ip do peer que irá receber
    private int portSend; // porta do peer que irá receber
    private String sendInfo; // dados que serão enviados


    public PeerThreadSendStringTCP (Peer peer,String ipSend, int portSend, String sendInfo){
        this.peer = peer;
        this.ipSend = ipSend;
        this.portSend = portSend;
        this.sendInfo = sendInfo;

    }

    public void run() {

        try {

            // Tenta criar uma conexão com o peer remoto com o ip e porta fornecidos
            Socket s = new Socket(ipSend,portSend);
            // cria a cadeia de saída (escrita) de informações do socket
            OutputStream os = s.getOutputStream();
            DataOutputStream writer = new DataOutputStream(os);
            Mensagem msgDownload = new Mensagem();
            msgDownload.setIpPortPeer(peer.getIp(),peer.getPort());
            msgDownload.setComment(sendInfo);
            sendInfo = Mensagem.preparaJson(msgDownload); // prepara o json para envio
            byte[] dataInBytes = sendInfo.getBytes(StandardCharsets.UTF_8); // codifica a string no padrão UTF-8
            writer.writeChar('s'); // especifica o tipo de mensagem
            writer.writeInt(dataInBytes.length); // agrega o tamanho da string a ser enviada
            writer.write(dataInBytes); // envia a string
            s.close();


        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

}

// Thread para enviar arquivos para outros peers
class PeerThreadSendFileTCP extends Thread {

    private Peer peer; // especificações do Peer
    private String ipSend; // ip do peer que irá receber
    private int portSend; // porta do peer que irá receber
    private File file; // dados que serão enviados

    public PeerThreadSendFileTCP (Peer peer, String ipSend, int portSend, File file){
        this.peer = peer;
        this.ipSend = ipSend;
        this.portSend = portSend;
        this.file = file;
    }

    public void run() {

        try {

            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);

            DataInputStream dis = new DataInputStream(bis);

            // Tenta criar uma conexão com o peer remoto com o ip e porta fornecidos
            Socket s = new Socket(ipSend, portSend);
            // cria a cadeia de saída (escrita) de informações do socket
            OutputStream os = s.getOutputStream();
            DataOutputStream writer = new DataOutputStream(os);
            writer.writeUTF(file.getName()); // adiciona o nome do arquivo
            writer.writeChar('f'); // especifica o tipo de mensagem
            byte[] dataBuffer = new byte[4096]; // cria vetor para armazenar blocos de bytes do arquivo
            int read;
            while((read = dis.read(dataBuffer)) >= 0){
                writer.write(dataBuffer, 0, read);
            }
            writer.close();
            if (writer != null) {
                writer.flush();
            }

        }
        catch(Exception e){
            e.printStackTrace();
        }

}

}