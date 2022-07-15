package p2p;

import java.io.*;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap; // biblioteca para criar hash

// bibliotecas para troca de dados utilizando o TCP
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;


public class Peer {

    //Solicita um IPv4, uma porta e uma pasta para o Peer, os quais devem der inseridos pelo teclado
    public static Mensagem PeerConfig() throws IOException {
        Scanner keyboard = new Scanner(System.in);
        System.out.println("Digite um IPv4 (0.0.0.0 a 255.255.255.255): ");
        String ip = keyboard.next();
        System.out.println("Digite uma porta (entre 1024 e 65535): ");
        int port = keyboard.nextInt();
        System.out.println("Digite o endereço da pasta onde se encontram os arquivos: ");
        String folder = keyboard.next();

        List<File> filesInFolder = Files.walk(Paths.get(folder))
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .collect(Collectors.toList());
        Mensagem ipconfig = new Mensagem(ip,port);
        return ipconfig;
    }

    public static void tryJOIN (DatagramSocket clientSocket, Mensagem peer) throws IOException {
        InetAddress serverAddr = InetAddress.getByName("127.0.0.1"); // IP padrão do Servidor
        int serverPort = 10098; // porta do Servidorpara conectar com os peers
        peer = PeerConfig();
        peer.setOption("JOIN"); // atribuí a opção de JOIN a opção
        String sendJson = Mensagem.preparaJson(peer); //Cria JSON com os dados do peer
        Mensagem.enviaPacket(sendJson,clientSocket,serverAddr,serverPort); //envia o datagrama para o servidor
        clientSocket.setSoTimeout(7000); // temporizador aguarda até 7s após o envio para o servidor, caso não haja retorno, uma nova tentativa é feita
        try {
            String answer = Mensagem.ACKfromServer(clientSocket); // retorno do servidor
            if (answer.equals("JOIN_OK")) { // Se o JOIN for aceito pelo servidor, o peer imprime a mensagem na console
                System.out.println("Sou peer " + (peer.getIp()) + ":" + peer.getPort() + " com arquivos " + peer.getFiles());
                clientSocket.close();
            } else { // Caso contrário, uma nova tentativa é feita com outro IP
                tryJOIN(clientSocket,peer);
            }
        }catch(SocketTimeoutException e){
            tryJOIN(clientSocket,peer); // Nova tentativa de join com o servidor
        }
    }

    // estabelece a conexão UDP entre o Peer e o Servidor
    public static void conectToServer() throws IOException {
        DatagramSocket clientSocket = new DatagramSocket(); // Datagrama para conexão UDP com o Servidor
        Mensagem peer = new Mensagem(); // gera mensagem que para armezenar dados do peer
        tryJOIN(clientSocket,peer); // prepara a mensagem e tenta o JOIN com o Servidor
    }

    public static void main(String[] args) throws IOException, InterruptedException {
            conectToServer();
            while(true){

                continue;
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

