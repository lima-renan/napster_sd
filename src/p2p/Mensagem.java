package p2p;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.*;

import com.google.gson.Gson;

// Cabeçalho que será usado para as mensagens
public class Mensagem {
    private String ip; //IP do peer
    private int port; //Porta do Peer
    private String option; //Operação
    private String[] files; //arquivos
    private String comment; //utilizado para SEARCH, UPDATE ou DOWNLOAD


    // Construtores da classe
    public Mensagem() {

    }

    //Mensagem com arquivos
    public Mensagem(String ip, int port, String option, String[] files) {
        this.ip = ip;
        this.port = port;
        this.option = option;
        this.files = files;
    }



    // Métodos setters e getters
    public String getIp() {
        return this.ip;
    }

    public int getPort() {
        return this.port;
    }

    public String getOption() {
        return this.option;
    }

    public String[] getFiles() {
        return this.files;
    }

    public String getComment() {
        return this.comment;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setOption(String option) {
        this.option = option;
    }

    public void setFiles(String[] files) { this.files = files; }
    public void setComment(String comment) { this.comment = comment; }




    // Cria objeto que recebe o cabeçalho da mensagem que será enviada e retorna uma string json
    public static String preparaJson (Mensagem msg){
        Gson sendgson = new Gson(); // instância para gerar a string json de envio
        return sendgson.toJson(msg);
    }

    // Envia o pacote com a mensagem no formato JSON através do socket para o IP e Porta do Servidor (127.0.0.1:10098)
    public static void enviaPacket (String jsonMsg, DatagramSocket clientSocket, InetAddress ipServer, int portServer) throws IOException{
        byte[] sendData; // buffer de envio
        sendData = (jsonMsg).getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ipServer, portServer); //cria datagrama de envio
        clientSocket.send(sendPacket); // envia pacote com a mensagem
    }

    // Envia o ACK para o peer
    public static void setACK (String msg, DatagramPacket recPkt, DatagramSocket serverSocket) throws IOException{
        byte[] sendBuf; // Buffer para armazenar os bytes do ACK

        sendBuf = msg.getBytes(); //Prepara o buffer com a String que o servidor vai enviar

        InetAddress IPAddress = recPkt.getAddress();

        int port = recPkt.getPort();

        DatagramPacket sendPacket = new DatagramPacket(sendBuf, sendBuf.length, IPAddress, port);

        serverSocket.send(sendPacket);
    }

    // Trata as mensagens recebidas dos peers e envia um ACK
    public static void sendACK (DatagramSocket serverSocket, DatagramPacket recPkt, HashMap<String, Integer> ip_port_peers, HashMap<String,  ArrayList<String>> files_peers) throws IOException{

        Gson recgson = new Gson(); //instância para gerar a mensagem a partir string json do cliente

        String informacao = new String(recPkt.getData(),recPkt.getOffset(),recPkt.getLength()); //Datagrama do cliente é convertido em String json

        Mensagem msg = recgson.fromJson(informacao,Mensagem.class);  //gera a mensagem a partir da string json recebida do cliente
        // verifica a comunicação do peer
        switch (msg.getOption()) {
            case "JOIN":
                if(ip_port_peers.containsKey(msg.getIp())){ // se já houver algum peer com o mesmo IP, o JOIN é negado
                    setACK("JOIN_DONE", recPkt, serverSocket); // envia o ACK para o cliente
                }
                else {
                    ip_port_peers.put(msg.getIp(), msg.getPort()); // Adiciona o IP e a porta do peer na lista do HashMap
                    String[] files = msg.getFiles(); // váriavel com todos os arquivos do peer
                    for (String file : files) { // verifica todos os arquivos
                        if (files_peers.get(file) == null) { // se não houver o arquivo no HashMap, uma nova entrada é adicionada
                            ArrayList<String> ips = new ArrayList<>();
                            ips.add((msg.getIp() + ":" + msg.getPort())); //concatena o Ip com a porta do peer
                            files_peers.put(file, ips); // Um array com o IP do peer que contêm o arquivo é adicionado
                        } else {
                            files_peers.get(file).add((msg.getIp() + ":" + msg.getPort())); //caso algum peer já possua o arquivo, apenas mais um IP é acrescentado no Array indicando o outro peer que também possui
                        }
                    }
                    setACK("JOIN_OK", recPkt, serverSocket); // envia string ACK para o cliente
                    System.out.println("Peer " + (msg.getIp()) + ":" + msg.getPort() + " adicionado com arquivos " + Arrays.toString(msg.getFiles())); // imprime no prompt do servidor
                   /* System.out.println(Arrays.asList(ip_port_peers));
                    System.out.println(Arrays.asList(files_peers));*/
                }
                break;
            case "LEAVE":
                ip_port_peers.remove(msg.getIp());
                String[] files = msg.getFiles(); // váriavel com todos os arquivos do peer
                for (String file : files) { // verifica todos os arquivos
                    files_peers.get(file).remove((msg.getIp() + ":" + msg.getPort())); //caso algum peer já possua o arquivo, apenas mais um IP é acrescentado no Array indicando o outro peer que também possui
                }
                /*System.out.println(Arrays.asList(ip_port_peers));
                System.out.println(Arrays.asList(files_peers));
                System.out.println("teste");*/
                setACK("LEAVE_OK", recPkt, serverSocket); // envia string ACK para o cliente


        }
    }

    //Recebe o ACK do Servidor e retorna a Mensagem com detalhes da conexão
    public static String ACKfromServer (DatagramSocket clientSocket) throws IOException{

        byte[] recBuffer = new byte[1024]; // buffer de recebimento

        DatagramPacket recPkt = new DatagramPacket(recBuffer, recBuffer.length); // cria pacote de recebimento

        clientSocket.receive(recPkt); // recebe o pacote do servidor

        return new String(recPkt.getData(),recPkt.getOffset(),recPkt.getLength());

    }

    //Solicita um IPv4, uma porta e uma pasta para o Peer, os quais devem der inseridos pelo teclado
    public static void PeerConfig(Especificacoes peer) {
        Scanner keyboard = new Scanner(System.in);
        System.out.println("Digite seu IPv4 e a porta(entre 1024 e 65535) para conexão (e.g., 0.0.0.0:0000) : ");
        String ip = keyboard.next(); //váriavel que captura o IPv4 e a porta do peer
        System.out.println("Digite o endereço da pasta onde se encontram os arquivos: ");
        String folder = keyboard.next(); //váriavel que captura o endereço do diretório
        File dir = new File(folder); // Armazena os arquivos da pasta
        String[] files = dir.list(); // lsita os arquivos do diretório
        if (files != null) {
            Arrays.sort(files); // ordena o array
        }
        String[] ipPort = ip.split(":"); //separa IP e porta em duas strings
        //seta as especificações do peer
        peer.setIp(ipPort[0]);
        peer.setPort(Integer.parseInt(ipPort[1]));
        peer.setFiles(files);
        peer.setFolder(folder);
    }

    public static void prepSearch (Mensagem msgServer){
        Scanner keyboard = new Scanner(System.in);
        System.out.println("Digite o nome do arquivo desejado com a extensão (e.g., aula.mp4): ");
        String name = keyboard.next(); //váriavel com o nome do arquivo que será buscado
        msgServer.setComment(name); // atualiza a mensagem com o nome do arquivo
    }

    public static void initDownload (Mensagem msgServer){
        Scanner keyboard = new Scanner(System.in);
        System.out.println("Digite o IPv4 e a porta do peer (e.g., 0.0.0.0:0000) que contém o arquivo para DOWNLOAD: ");
        String ip = keyboard.next(); //váriavel que captura o IPv4 do peer
        msgServer.setComment(ip); // atualiza a mensagem com os dados do peer que contém o arquivo
    }

    // Menu

    public static void menu (Especificacoes peer, DatagramSocket clientSocket) throws IOException {
        Scanner keyboard = new Scanner(System.in);
        System.out.println("Digite o que deseja fazer: ");
        int name = keyboard.nextInt(); //váriavel com o nome do arquivo que será buscado
        switch (name){
            case 1:
                if(clientSocket.isClosed()) {
                    tryConect(peer, clientSocket, "JOIN");
                }
                else{
                    System.out.println("Sou peer " + peer.getIp() + ":" + peer.getPort() + " com arquivos " + Arrays.toString(peer.getFiles())); // imprime as informações do Peer
                }
                break;
            case 2:
                tryConect(peer,clientSocket, "LEAVE");
                break;
        }
    }

    // Tenta um retorno do Servidor para o JOIN, LEAVE ou SEARCH - Recebe o DatagramSocket e a opção de comunicação desejada
    public static void tryConect (Especificacoes peer, DatagramSocket clientSocket, String opt) throws IOException {
        InetAddress serverAddr = InetAddress.getByName("127.0.0.1"); // IP padrão do Servidor
        int serverPort = 10098; // porta do Servidor para conectar com os peers
        Mensagem msgServer = new Mensagem();
        switch (opt) { // Prepara a mensagem para enviar ao servidor
            case "JOIN": //Coloca os dados necessários para o JOIN
                PeerConfig(peer);
                msgServer.setIp(peer.getIp());
                msgServer.setPort(peer.getPort());
                msgServer.setOption(opt);
                msgServer.setFiles(peer.getFiles());
                break;

            case "LEAVE": //Coloca os dados necessários para o LEAVE
                msgServer.setIp(peer.getIp());
                msgServer.setPort(peer.getPort());
                msgServer.setOption(opt);
                msgServer.setFiles(peer.getFiles()); // envia o nome dos arquivos para serem removidos do servidor
                break;

            case "SEARCH": //Coloca os dados necessários para o SEARCH
                prepSearch(msgServer);
                msgServer.setIp(peer.getIp());
                msgServer.setPort(peer.getPort());
                msgServer.setOption(opt);
                break;

            case "DOWNLOAD": //Coloca os dados necessários para o SEARCH
                initDownload(msgServer);
                msgServer.setIp(peer.getIp());
                msgServer.setPort(peer.getPort());
                msgServer.setOption(opt);
                break;
        }
        String sendJson = Mensagem.preparaJson(msgServer); //Cria JSON com os dados do peer
        Mensagem.enviaPacket(sendJson,clientSocket,serverAddr,serverPort); //envia o datagrama para o servidor
        clientSocket.setSoTimeout(7000); // temporizador aguarda até 7s após o envio para o servidor, caso não haja retorno, uma nova tentativa é feita
        try {
            String answer = Mensagem.ACKfromServer(clientSocket); // retorno do servidor
            switch (opt) {
                case "JOIN":
                    System.out.println("Sou peer " + msgServer.getIp() + ":" + msgServer.getPort() + " com arquivos " + Arrays.toString(msgServer.getFiles())); // imprime as informações do Peer
                    break;
                case "LEAVE":
                    if (answer.equals("LEAVE_OK")) { // Se o LEAVE for aceito pelo servidor, a conexão é encerrada
                        clientSocket.close(); // quando receber a confirmação fecha o socket com o servidor
                    }
                    break;
            }
        }catch(SocketTimeoutException e){ // trata o timeout
            switch (opt) {
                case "JOIN":
                    tryConect(peer,clientSocket, "JOIN"); // Nova tentativa de JOIN com o servidor depois do timeout
                    break;
                case "LEAVE":
                    tryConect(peer,clientSocket, "LEAVE"); // Nova tentativa de LEAVE com o servidor depois do timeout
                    break;
            }
        }
    }

}

// Adiciona a pasta do peer na mensagem
class Especificacoes extends Mensagem {
    private String folder; //Endereço da apsta com os arquivos

    public Especificacoes (String ip, int port, String[] files, String folder) {
        this.setIp(ip);
        this.setPort(port);
        this.setFiles(files);
        this.folder = folder;
    }

    public Especificacoes() {

    }


    // Método get
    public String getFolder() {
        return this.folder;
    }

// Método set
    public void setFolder(String folder) { this.folder = folder; }
}
    /*
    // Boas-vindas: Captura a mensagem que o usuário deseja enviar
    public static String capturaMensagem(){
        System.out.print("Digite a mensagem que deseja enviar ou digite sair para encerrar: ");
        Scanner teclado = new Scanner(System.in);
        String mensagem = teclado.nextLine();
        return mensagem;
    }

    // Formata mensagem de entrada do usuário
    public static void formatInp(String op, String inp, String id){
        String format = "Mensagem " + "\"" + inp + "\"" + " enviada como " + op + " com id " + id;
        System.out.println(format);
    }

    // Seção 3: Buffer confirmadas - Retorna valor válido para o id baseada nas mensagens já confirmadas
    public static int vazioId (int i, HashMap<String, String> confirmadas){
        while(confirmadas.containsKey(String.format("%04d", i))){ // enquanto houver o id em sequência no Map, i é iterado
            i = i + 1;
        }
        return i; // retorna um i que pode ser usado como id de mensagem
    }

    //  HashMap que armazena os id e as mensagens que serão enviadas pelo sender
    public static boolean senderEnviadas (Mensagem msg, HashMap<String, String> enviadas){
        if(enviadas.isEmpty() || enviadas.size() < 10){ // janela de tamanho máximo 10, caso seja maior a mensagem é descartada: return False
            enviadas.put(msg.getId(), msg.getMensagem()); // preenche o HashMap
            return true;
        }
        return false;
    }

    // Envia o ACK para o cliente
    public static void setACK (Mensagem msg, DatagramPacket recPkt, DatagramSocket serverSocket) throws IOException{
        byte[] sendBuf = new byte[1024]; // Buffer para armazenar os bytes do ACK

        Gson gsonsend = new Gson(); // Objeto para armazernar a string json que será enviada no CAK

        String sendmsgudp = gsonsend.toJson(msg); // converte a mensagem em json

        sendBuf = sendmsgudp.getBytes(); //Prepara o buffer

        InetAddress IPAddress = recPkt.getAddress();

        int port = recPkt.getPort();

        DatagramPacket sendPacket = new DatagramPacket(sendBuf, sendBuf.length, IPAddress, port);

        serverSocket.send(sendPacket);
    }


    // Seção 3: Buffer recebidas - Trata as mensagens recebidas no receiver
    public static void setRecebidas (DatagramSocket serverSocket, HashMap<String, String> recebidas) throws IOException{

        Gson recgson = new Gson(); //instância para gerar a mensagem a partir string json do cliente

        byte[] recBuffer = new byte[1024];

        DatagramPacket recPkt = new DatagramPacket(recBuffer, recBuffer.length);

        serverSocket.receive(recPkt); //BLOCKING

        String informacao = new String(recPkt.getData(),recPkt.getOffset(),recPkt.getLength()); //Datagrama do cliente é convertido em String json

        Mensagem msg = recgson.fromJson(informacao,Mensagem.class);  //gera a mensagem a partir da string json recebida do cliente

        if(recebidas.isEmpty()){//se ainda não foram recebidas mensagens
            formatRec(msg.getId(), "normal", null); // exibe na console que a mensagem foi recebida pelo receiver no modo normal
            recebidas.put(msg.getId(), msg.getMensagem()); // preenche o Map
            setACK(msg, recPkt, serverSocket); // envia o ACK para o cliente
        }
        else{
            if(recebidas.containsKey(msg.getId())){ // verifica se a mensagem já foi recebida, caso sim é porque a mensagem é duplicada
                formatRec(msg.getId(), "duplicada", null);
            }
            else{
                int i = 1;
                while(recebidas.containsKey(String.format("%04d", i))){ // enquanto houver o id em sequência no Map, i é iterado
                    i = i + 1;
                }
                i = i - 1; // subtraí 1 para obter a primeira mensagem da janela
                int id = Integer.parseInt(msg.getId()); // converte o id em inteiro da mensagem atual em inteiro
                if((id - i) < 11){ // janela de tamanho máximo 10, caso seja maior a mensagem é descartada
                    if((id - i) == 1){ // se a diferença for 1 é porque a mensagem está na sequência correta
                        formatRec(msg.getId(), "normal", null); // exibe na console que a mensagem foi recebida pelo receiver no modo normal
                        recebidas.put(msg.getId(), msg.getMensagem()); // preenche o HashMap
                        setACK(msg, recPkt, serverSocket); // envia o ACK para o cliente
                    }
                    else{// fora de ordem
                        ArrayList<String>identficadores = new ArrayList<>(); // cria lista com os identificadores pendentes
                        //int index = 0; // indice do array
                        while (i < id){ // enquanto i for menor que o id atual, preenche a lista
                            if(!recebidas.containsKey(String.format("%04d", i))){ //verifica se não há esse id no map
                                identficadores.add((String.format("%04d", i)));
                            }
                            i = i + 1; // incrementa o contador
                        }
                        formatRec(msg.getId(), "fora de ordem", identficadores); // exibe na console que a mensagem foi recebida pelo receiver no modo fora de ordem e os identificadores faltantes
                        recebidas.put(msg.getId(), msg.getMensagem()); // preenche o HashMap
                        setACK(msg, recPkt, serverSocket); // envia o ACK para o cliente
                    }
                }
            }
        }
    }


    // Seção 3: Buffer recebidas - Recebe o ACK do receiver e libera atualiza a janela de envio do sender
    public static void senderACK (HashMap<String, String> enviadas, HashMap<String, String> confirmadas, DatagramSocket clientSocket) throws IOException{

        clientSocket.setSoTimeout(7000); // temporizador aguarda até 7s após o envio pelo setEnvio()

        byte[] recBuffer = new byte[1024]; // buffer de recebimento

        DatagramPacket recPkt = new DatagramPacket(recBuffer, recBuffer.length); // cria pacote de recebimento

        clientSocket.receive(recPkt); // recebe o pacote do servidor

        String informacao = new String(recPkt.getData(),recPkt.getOffset(),recPkt.getLength()); //obtem a mensagem no formato json string

        Gson recgson = new Gson(); // instância para gerar a string json de recebimento

        Mensagem msg = recgson.fromJson(informacao, Mensagem.class); //converte a string json em mensagem

        Mensagem.formatConf(msg.getId()); // Exibe na tela o id da mensagem que foi confirmada pelo servidor

        confirmadas.put(msg.getId(), msg.getMensagem()); // preenche o HashMap com os dados atualizados
        enviadas.remove(msg.getId()); // remove o id confirmado da janela de envio
    }

    // Formata mensagem recebida no receiver conforme a opção de envio, caso não seja fora de ordem, a lista de identificadores é ignorada
    public static void formatRec(String id, String op, ArrayList<String> ident){
        String format;
        // verifica a opção e retorna a mensagem do receiver
        switch(op){
            case "fora de ordem":
                format = "Mensagem id " + id + " recebida fora de ordem, ainda não recebidos os identificadores " + ident;
                System.out.println(format);
                break;
            case "duplicada":
                format = "Mensagem id " + id + " recebida de forma duplicada";
                System.out.println(format);
                break;
            case "normal":
                format = "Mensagem id " + id + " recebida na ordem, entregando para a camada de aplicação.";
                System.out.println(format);
                break;
            default:
                format = "Erro - Opção desconhecida";
                System.out.println(format);
        }
    }

    // Formata mensagem confirmada pelo receiver
    public static void formatConf(String id){
        String format;
        // verifica a opção e retorna a mensagem do receiver
        format = "Mensagem id " + id + " recebida pelo receiver.";
        System.out.println(format);
    }

    // Cria objeto que recebe o cabeçalho da mensagem que será enviada e retorna uma string json
    public static String preparaJson (Mensagem msg){
        Gson sendgson = new Gson(); // instância para gerar a string json de envio
        String jmsgudp = sendgson.toJson(msg); // converte a mensagem em string json para envio
        return jmsgudp;
    }

    // Envia o pacote com a mensagem no formato JSON através do socket para o IP e Porta do Servidor (127.0.0.1:10098)
    public static void enviaPacket (String jsonMsg, DatagramSocket clientSocket, InetAddress ipServer, int portServer) throws IOException{
        byte[] sendData = new byte [1024]; // buffer de envio
        sendData = (jsonMsg).getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ipServer, portServer); //cria datagrama de envio
        clientSocket.send(sendPacket); // envia pacote com a mensagem
    }

    // Verifica e configura a opção de envio - Seção 3: Buffer de envio
    public static void setEnvio(Mensagem msg, int id, HashMap<String, String> enviadas, DatagramSocket clientSocket, InetAddress IPAddress) throws IOException, InterruptedException{
        // Solicita a opcao de envio
        System.out.println("Escolha o número da opção de envio:");
        System.out.println("1 - lenta");
        System.out.println("2 - perda");
        System.out.println("3 - fora de ordem");
        System.out.println("4 - duplicada");
        System.out.println("5 - normal");
        System.out.print("Número: ");
        Scanner teclado = new Scanner(System.in);
        int opcao = teclado.nextInt();
        if(senderEnviadas(msg, enviadas)){ // se a janela de envio não tiver atingido a capacidade máxima, uma nova mensagem é inserida
            String jmsgudp = new String(); // String que recebe o json da mensagem
            // Formata o input conforme a opção selecionada
            switch(opcao){
                case 1: // Seção 3: envio lento
                    formatInp("lenta",msg.getMensagem(),msg.getId());
                    Thread.sleep(4000); // aguarda 4s antes de enviar a mensagem para o servidor
                    jmsgudp = preparaJson(msg);
                    enviaPacket(jmsgudp, clientSocket, IPAddress);
                    break;
                case 2: // Seção 3: envio com perda
                    formatInp("perda",msg.getMensagem(),msg.getId()); // Pacote não é enviado
                    break;
                case 3: // Seção 3: envio fora de ordem
                    formatInp("fora de ordem",msg.getMensagem(),msg.getId());
                    // Cria uma nova mensagem a partir do inteiro e da string  do input do usuário
                    if (vazioId((id + 2), enviadas) == (id + 2)){// Verifica se há 2 posições a frente da posição de id que deveria ser enviada                       id = id + 2; // incrementa o id em duas posições
                        msg.setId(String.format("%04d", (id + 2))); // Modifica o id
                        jmsgudp = preparaJson(msg); // prepara a string json
                        enviaPacket(jmsgudp, clientSocket, IPAddress); // envia o pacote
                        enviadas.remove((String.format("%04d", id))); // remove o id antigo do HashMap
                        enviadas.put(msg.getId(), msg.getMensagem()); // preenche o HashMap com os dados atualizados
                    }
                    break;
                case 4: // envio duplicado, envia duas vezes
                    formatInp("duplicada",msg.getMensagem(),msg.getId());
                    jmsgudp = preparaJson(msg);
                    enviaPacket(jmsgudp, clientSocket, IPAddress);
                    enviaPacket(jmsgudp, clientSocket, IPAddress);
                    break;
                case 5: // envio normal
                    formatInp("normal",msg.getMensagem(),msg.getId());
                    jmsgudp = preparaJson(msg);
                    enviaPacket(jmsgudp, clientSocket, IPAddress);
                    break;
                default:
                    System.out.println("Opção inválida.");
            }

        }
    }

    // Seção 3 - Repetição Seletiva: Reenvia pacote perdido após o timeout no sender
    public static void setReenvio(Mensagem msg, HashMap<String, String> enviadas, DatagramSocket clientSocket, InetAddress IPAddress) throws IOException, InterruptedException{
        if(senderEnviadas(msg, enviadas)){ // se a janela de envio não tiver atingido a capacidade máxima, uma nova mensagem é inserida
            String jmsgudp = preparaJson(msg); // String que recebe o json da mensagem
            System.out.println("A mensagem de id " + msg.getId() + " será reenviada."); //Mensagem exibida na console.
            enviaPacket(jmsgudp, clientSocket, IPAddress); // reenvia o pacote
        }
    }

}
*/