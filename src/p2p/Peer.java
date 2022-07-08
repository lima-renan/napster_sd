package p2p;

import java.io.IOException; // biblioteca para execções
import java.net.DatagramSocket; // biblioteca para UDP
import java.net.InetAddress; // biblioteca para endereço IP
import java.net.Socket; // biblioteca para TCP
import java.net.SocketTimeoutException; // biblioteca para timer
import java.util.HashMap; // biblioteca para criar hash

// bibliotecas para troca de dados utilizando o TCP
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;


public class Peer {

    //Gera um IPv4 e uma porta aleatória para o Peer e cria um socket
    public static Socket socketPeer() throws IOException {
        //long millis = System.currentTimeMillis();
        Random r = new Random();
        // gera números aleatórios entre 1 e 255 para em seguida gerar uma string de IPs
        String ip = r.nextInt(255 + 1) + "." + r.nextInt(255 + 1) + "." + r.nextInt(255 + 1) + "." + r.nextInt(255 + 1);
        // gera um valor númerico entre 1024 e 65535
        int port = ThreadLocalRandom.current().nextInt(1024, 65535 + 1);
        // Socket s do peer terá IP entre 1.1.1.1 e 255.255.255.255 e uma porta designada entre 1024 e 65535
        Socket s = new Socket(ip, port);
        return s;
    }

    // estabelece a conexão entre o Peer e o Servidor
    public static void conectToServer(){
        while()
    }
    public static void main (String[] args) throws IOException, InterruptedException {


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
}