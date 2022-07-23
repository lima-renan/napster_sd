package p2p;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;





public class Peer {

    //Especificações do peer

    private String ip;
    private String port;
    private ArrayList<String> files;
    private String folder; //Endereço da apsta com os arquivos


    // Métodos setters e getters
    public String getIp(){ return this.ip;}

    public String getPort(){ return this.port;}

    public ArrayList<String> getFiles() {
        return this.files;
    }
    public String getFolder() {
        return this.folder;
    }

    public void setIp(String ip) { this.ip = ip; }

    public void setPort(String port) { this.port = port; }

    public void setFiles (ArrayList<String>files) { this.files = files; }
    public void setFolder(String folder) { this.folder = folder; }


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
        Peer peer = new Peer(); // Cria estrutura para receber especificações do peer
        DatagramSocket clientSocket = new DatagramSocket(); // Datagrama para conexão UDP com o Servidor
        byte[] recBuffer = new byte[1024]; // buffer de recebimento
        DatagramPacket recPkt = new DatagramPacket(recBuffer, recBuffer.length); // cria pacote de recebimento
        Mensagem.welcome(); // exibe mensagem de inicialização
        Thread send = new PeerThreadSend(peer, clientSocket); // exibe o menu e envia através de threads
        send.setName("SEND");
        Thread reck = new PeerThreadReceiver(peer, clientSocket, recPkt); // Gera uma nova thread qd chega uma mensagem do servidor
        reck.setName("RECK");
        send.start();
        reck.start();

    }
}

    // Classe aninhada para comunicação com o Server e outros peers através de Threads
    class PeerThreadSend extends Thread {

        Peer peer; // Armazena as especificações do peer
        DatagramSocket clientSocket; // Armazena os dados do datagrama do peer

        public PeerThreadSend(Peer peer, DatagramSocket clientSocket) {
            this.peer = peer;
            this.clientSocket = clientSocket;
        }

        public void run() {
            while (true) {
                Mensagem.menu(peer, clientSocket); // exibe o menu de opções: JOIN, SEARCH, DOWNLOAD E LEAVE - usa thread para enviar
                continue;
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
            while(true) {
                Mensagem.ACKfromServer(peer, clientSocket, recPacket); //aguarda retorno do servidor
                continue;
            }
        } catch (SocketException socketException) { //caso o socket seja fechado
            try {
                clientSocket = new DatagramSocket();
            } catch (SocketException e) {
                throw new RuntimeException(e);
            }
            new PeerThreadReceiver(peer,clientSocket, recPacket); // Gera uma nova thread qd chega uma mensagem do servidorreck.setName("RECK");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}

        /*
        Socket s = socketPeer();

        // cria a cadeia de saída (escrita) de informações do socket
        OutputStream os = s.getOutputStream();
        DataOutputStream writer = new DataOutputStream(os);

        //cria a cadeia de entrada (leitura) de informações do socket
        InputStreamReader is = new InputStreamReader(s.getInputStream());
        BufferedReader reader = new BufferedReader(is);

        // cria um buffer que lê informações do teclado
        BufferedReader inFromUser = new BufferedReader (new InputStreamReader(System.in));

        // leitura do teclado
        String texto = inFromUser.readLine(); //BLOCKING

        // escrita no socket (envio de informação ao host remoto)
        writer.writeBytes(texto + "\n");

        // leitura do socket (recebimento de informação do host remoto)
        String response = reader.readLine(); //BLOCKING
        System.out.println("DoServidor:" + response);

        //fechamento do canal (socket)
        s.close();



        HashMap<String, String> enviadas = new HashMap<>(); // Cria HashMap para armazenar mensagens enviadas

        HashMap<String, String> confirmadas = new HashMap<>(); // Cria HashMap para armazenar os ACKs

        int i = 1; // inteiro que será usado como id da msg

        DatagramSocket clientSocket = new DatagramSocket(); // Sistema Operacional assina uma porta

        InetAddress IPAddress = InetAddress.getByName("127.0.0.1"); // Define o IP do servidor

        // Enquanto o usuário não digitar sair ou Ctrl+C, o cliente continuará executando
        while(true){

            i =  Mensagem.vazioId(i, confirmadas); // Verifica qual é a primeira posição vazia para o id

            // Cria uma nova mensagem a partir do inteiro e da string  do input do usuário
            Mensagem msgudp = new Mensagem(String.format("%04d", i), Mensagem.capturaMensagem()); //id é passado como string de 4 dígitos

            // caso o usuário digite sair, o processo é encerrado
            if((msgudp.getMensagem()).equals("sair")){
                break;
            }


            // Prepara, envia o pacote e retorna o cabeçalho do pacote enviado. Em seguida, retorna o id da mensagem enviada
            Mensagem.setEnvio(msgudp, i, enviadas, clientSocket, IPAddress); // Exibe na ta tela a mensagem que será enviada, adiciona ao HashMap e envia ao servidor

            try { //inicializa o temporizador
                Mensagem.senderACK(enviadas, confirmadas, clientSocket); //recebe o ACK do receiver

            }catch(SocketTimeoutException e){ // Procedimento para quando houver timeout e não houver confirmação de recebimento

                Mensagem.setReenvio(msgudp, enviadas, clientSocket, IPAddress); // Prepara, reenvia o pacote e retorna o cabeçalho do pacote enviado

                Mensagem.senderACK(enviadas, confirmadas, clientSocket); // Recebe o ACK do receiver

                continue; // continua no loop
            }
        }
    }
    */

