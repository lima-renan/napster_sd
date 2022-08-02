package p2p;

import com.google.gson.Gson;


import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;



public class Peer {

    //Especificações do peer

    private String ip;
    private String port;
    private ArrayList<String> files;
    private String folder; //Endereço da pasta com os arquivos
    private String lastFile; //armazena o último arquivo baixado
    private String solicitedFile; // arquivo solicitado para o download
    private volatile boolean answer; // váriavel que indica se a mensagem foi recebida de outro peer

    private volatile boolean running; //determina se o peer está em execução

    private ArrayList<String> peersSearch; // armazena os IPs e portas dos peers que possuem o arquivo



    // Métodos setters e getters
    public String getIp(){ return this.ip;}

    public String getPort(){ return this.port;}

    public ArrayList<String> getFiles() {
        return this.files;
    }
    public String getFolder() {
        return this.folder;
    }
    public String getLastFile(){ return this.lastFile; }
    public String getSolicitedFile(){ return this.solicitedFile; }
    public Boolean getAnswer() { return this.answer; }

    public Boolean getRunning() { return this.running; }
    public ArrayList<String> getPeersSearch() {
        return this.peersSearch;
    }
    public void setIp(String ip) { this.ip = ip; }

    public void setPort(String port) { this.port = port; }

    public void setFiles (ArrayList<String>files) { this.files = files; }
    public void setFolder(String folder) { this.folder = folder; }

    public void setLastFile (String file){ this.lastFile = file; }
    public void setSolicitedFile (String solicitedFileile){ this.solicitedFile = solicitedFileile; }
    public void setAnswer (boolean answer){ this.answer = answer; }

    public void setRunning (boolean run){ this.running = run; }
    public void setPeersSearch (ArrayList<String>peersSearch) { this.peersSearch = peersSearch; }

    public Peer (){

    }


    public static void main(String[] args) throws IOException, InterruptedException {
        Peer peer = new Peer(); // Cria estrutura para receber especificações do peer e
        peer.setRunning(true); //inicia o peer
        Mensagem.welcome(); // exibe mensagem de inicialização
        Mensagem.PeerConfig(peer); //
        DatagramSocket clientSocket = new DatagramSocket(Integer.parseInt(peer.getPort()),InetAddress.getByName(peer.getIp())); // Datagrama para conexão UDP com o Servidor
        ServerSocket peerTCP = new ServerSocket(Integer.parseInt(peer.getPort()),50,InetAddress.getByName(peer.getIp())); // inicializa o socket para recebimento de mensagens TCP
        byte[] recBuffer = new byte[1024]; // buffer de recebimento
        DatagramPacket recPkt = new DatagramPacket(recBuffer, recBuffer.length); // cria pacote de recebimento
        Thread reck = new PeerThreadReceiver(peer, clientSocket, recPkt); // Gera uma nova thread qd chega uma mensagem do servidor
        reck.setName("RECK");
        reck.start();
        Thread send = new PeerThreadSend(peer, clientSocket, "START"); // estabelece a primeira conexão com o server
        send.join();
        Thread tcpCnt = new PeerThreadDownload(peerTCP, clientSocket, peer); //Thread para recebimento de solicitações de downloads de outros peers via TCP
        tcpCnt.setName("tcpConnect");
        tcpCnt.start();
        while (peer.getRunning()) {
            Mensagem.menu(peer,clientSocket); //exibe o menu de opções: JOIN, SEARCH, DOWNLOAD E LEAVE - usa thread para enviar
        }
        // quando receber a confirmação do LEAVE:
        reck.interrupt();
        tcpCnt.interrupt();
        clientSocket.close(); // fecha o socket com o servidor
        System.err.println("Peer desligado!"); //mensagem de warning sobre nova tentativa de conexão
        System.exit(0); //encerra o processo
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
            } catch (InterruptedException e) {
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
            while(peer.getRunning()) {
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
    private DatagramSocket clientSocket; // utilizado para enviar update ao servidor
    private Peer peer; // detalhes do peer

    public PeerThreadDownload (ServerSocket peerSocket, DatagramSocket clientSocket, Peer peer){
        this.peerSocket = peerSocket;
        this.clientSocket = clientSocket;
        this.peer = peer;
    }

    public void run() {
        //Ref.: https://www.baeldung.com/java-inputstream-server-socket
        try {
            while(peer.getRunning()) {
                Socket node = peerSocket.accept();
                peer.setAnswer(true); // indica nas especificações que a mensagem foi recebida de outro peer
                DataInputStream clientData = new DataInputStream(new BufferedInputStream(node.getInputStream()));
                if(clientData.readChar() == 'f'){ // se for um arquivo
                    String filename = clientData.readUTF(); // armazena o nome do arquivo
                    long fileSize = clientData.readLong(); // lê o tamanho do arquivo
                    FileOutputStream fos = new FileOutputStream(peer.getFolder() + "/" + filename); // cria uma cópia do arquivo recebido no diretório do peer
                    byte[] fileByte = new byte[12*1024]; //buffer que armazena os bytes da mensagem recebida
                    int read; // variável para auxiliar na leitura dos bytes
                    while (fileSize > 0 && (read = clientData.read(fileByte, 0, (int)Math.min(fileByte.length, fileSize))) != -1) {
                        fos.write(fileByte, 0, read);
                        fileSize -= read; // lê até atingir o tamanho do arquivo
                    }
                    fos.close(); // fecha o arquivo
                    peer.setLastFile(filename); // adiciona nas esepcificações do peer, o nome do arquivo baixado
                    Thread send = new PeerThreadSend(peer, clientSocket, "UPDATE"); // envia um UPDATE para o server
                    send.join(); // aguarda a conclusão do processo
                    System.out.println("Arquivo " + filename + " baixado com sucesso na pasta " + peer.getFolder());

                } else {// se a mensagem enviada for uma string, deve-se validar se é DOWNLOAD_NEGADO ou uma solicitação de arquivo
                    int length = clientData.readInt();
                    byte[] msgByte = new byte[length]; //buffer que armazena os bytes da mensagem recebida
                    boolean end = false; //váriavel para controlar leitura dos bytes
                    StringBuilder dataString = new StringBuilder(length); //váriavel para gerar a string
                    int totalBytesRead = 0; // váriavel para auxiliar na leitura dos bytes
                    while (!end) { // transforma os bytes em string
                        int currentBytesRead = clientData.read(msgByte);
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
                        ArrayList<String> ipsSearch = peer.getPeersSearch();
                        if(!ipsSearch.isEmpty() && ipsSearch.size() > 1){
                            List<String> filteredList = ipsSearch.stream()
                                    .filter(ip -> !ip.equals(msg.getIpPeer()+":"+msg.getPortPeer()))
                                    .collect(Collectors.toList());

                            String[] ans = filteredList.get(0).split(":");
                            System.out.println("peer "+ msg.getIpPeer() + ":" + msg.getPortPeer() + " negou o download, pedindo agora para o peer " + ans[0] + ":" + ans[1]);
                            PeerThreadSendStringTCP sendString = new PeerThreadSendStringTCP(peer, ans[0], Integer.parseInt(ans[1]), peer.getSolicitedFile()); //seta o ip e porta do peer remoto e envia a mensagem
                            sendString.start(); // inicia a thread
                            sendString.join(); // aguarda a conclusão
                        }else{ // caso só um peer tenha o arquivo
                            System.out.println("peer "+ msg.getIpPeer() + ":" + "[" + msg.getPortPeer() +"]" + " negou o download, pedindo agora para o peer " + msg.getIpPeer() + ":" + msg.getPortPeer());
                            long timer = System.currentTimeMillis();
                            while((System.currentTimeMillis() - timer < 2000)){ // aguarda 2s para enviar uma nova solicitação
                            }
                            PeerThreadSendStringTCP sendString = new PeerThreadSendStringTCP(peer, msg.getIpPeer(), Integer.parseInt(msg.getPortPeer()), peer.getSolicitedFile()); //seta o ip e porta do peer remoto e envia a mensagem
                            sendString.start(); // inicia a thread
                            sendString.join(); // aguarda a conclusão

                        }

                    } else { //verifica se o arquivo solicitado existe
                        File myFile = new File(peer.getFolder() +"/"+ msg.getComment()); // instancia arquivo a partir o nome recebido pelo outro peer
                        if (myFile.exists() && Math.random() < 0.2) { //se o arquivo existir, sorteia para permitir o DOWNLOAD ou não
                            PeerThreadSendFileTCP sendFile = new PeerThreadSendFileTCP(peer,msg.getIpPeer(), Integer.parseInt(msg.getPortPeer()),myFile); //seta o ip e porta do peer remoto e envia a mensagem
                            sendFile.start(); // inicia a thread
                            sendFile.join(); // aguarda a conclusão
                        } else { //nega o download
                            PeerThreadSendStringTCP sendString = new PeerThreadSendStringTCP(peer,msg.getIpPeer(), Integer.parseInt(msg.getPortPeer()),"DOWNLOAD_NEGADO"); //seta o ip e porta do peer remoto e envia a mensagem
                            sendString.start(); // inicia a thread
                            sendString.join(); // aguarda a conclusão
                        }
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
            System.err.println("Peer solicitado não pode ser conectado, tente novamente!");
            System.exit(1);
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
            // Tenta criar uma conexão com o peer remoto com o ip e porta fornecidos
            Socket s = new Socket(ipSend, portSend);

            //váriaveis para ler o arquivo no diretório do peer
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);
            DataInputStream dis = new DataInputStream(bis);

            // cria a cadeia de saída (escrita) de informações do socket
            DataOutputStream writer = new DataOutputStream(new BufferedOutputStream((s.getOutputStream())));

            writer.writeChar('f'); //especifica que é um arquivo
            writer.flush();
            writer.writeUTF(file.getName()); //envia o nome do arquivo
            writer.flush();
            writer.writeLong(file.length()); //envia o tamanho do arquivo
            writer.flush();
            byte[] dataBuffer = new byte[12*1024]; // cria vetor para armazenar blocos de bytes do arquivo
            int read = dis.read(dataBuffer, 0,dataBuffer.length);
            while(read != -1){
                writer.write(dataBuffer, 0, read);
                read = dis.read(dataBuffer, 0,dataBuffer.length);
            }
            writer.flush();
            s.close();

        }
        catch(Exception e){
            e.printStackTrace();
            System.err.println("Peer solicitado não pode ser conectado, tente novamente!");
            System.exit(1);
        }

}

}