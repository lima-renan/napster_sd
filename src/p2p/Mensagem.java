package p2p;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.Gson;
import static java.lang.Thread.sleep;

// Cabeçalho que será usado para as mensagens
public class Mensagem {
    public static volatile int id; //id da mensagem que será enviada
    private int idRet; //id de retorno da mensagem, se diferente do id, a mensagem não é o ack esperado
    private String option; //Operação
    private String comment; //utilizado para SEARCH, UPDATE ou DOWNLOAD
    private AbstractMap.SimpleEntry<String, String>ipPortPeer; // aramzena o ip e a porta do peer
    private ArrayList<String> filesPeer; // armazena o nome dos arquivos do peer

    public static volatile ConcurrentHashMap <Integer, Boolean>msgStatus = new ConcurrentHashMap<>(); // registra se as mensagens enviadas já receberam o retorno
    public static volatile boolean ack;


    // Construtores da classe
    public Mensagem() {

    }

    // Métodos setters e getters

    public Integer getIdRet() {
        return this.idRet;
    }
    public String getOption() {
        return this.option;
    }
    public String getComment() {
        return this.comment;
    }
    public String getIpPeer(){ return this.ipPortPeer.getKey();}
    public String getPortPeer(){ return this.ipPortPeer.getValue();}
    public AbstractMap.SimpleEntry<String, String> getIpPortPeer(){ return this.ipPortPeer;}
    public ArrayList<String> getFilesPeer() {
        return this.filesPeer;
    }
    public void setIdRet(Integer id){ this.idRet =id; }
    public void setOption(String option) {
        this.option = option;
    }
    public void setComment(String comment) {
        this.comment = comment;
    }
    public void setIpPortPeer(String ip, String port) { this.ipPortPeer =  new AbstractMap.SimpleEntry<>(ip,port); }
    public void setFilesPeer (ArrayList<String>files) { this.filesPeer = files; }
    public static void setMsgStatus(Integer id, Boolean ack){ msgStatus.put(id,ack); }

    // Welcome - exibe mensagem assim que o peer é inicializado
    public static void welcome() {
        System.out.println("Projeto Napster: Peer inicializado" + "\n" + "\n");
    }

    // Menu com as opções de JOIN, LEAVE e SEARCH - retorna dados do datagrama da conexão com o servidor
    public static void menu(Peer peer,DatagramSocket clientSocket) {
            Scanner keyboard = new Scanner(System.in);
            System.out.println("\nMenu: " + "\n" +
                    "   " + "1 - JOIN" + "\n" +
                    "   " + "2 - SEARCH" + "\n" +
                    "   " + "3 - DOWNLOAD" + "\n" +
                    "   " + "4 - LEAVE");
            System.out.print("Digite o número da opção desejada: ");
            try {
                int opt = keyboard.nextInt(); //váriavel com o nome do arquivo que será buscado
                if (opt > 4 || opt < 1) {
                    throw new Exception("Exception thrown"); // lança uma exceção - entrada inválida
                }
                switch (opt) {
                    case 1: //JOIN
                        if (!clientSocket.isConnected()) {
                            System.out.println("Sou peer " + peer.getIp() + ":" + peer.getPort() + " com arquivos " + (peer.getFiles()).toString()); // imprime as informações do Peer
                        }
                        else if (!clientSocket.isClosed()) {
                            PeerThreadSend send = new PeerThreadSend(peer, clientSocket, "JOIN");
                            send.join();
                        } else {
                            clientSocket = new DatagramSocket();
                            PeerThreadSend send = new PeerThreadSend(peer, clientSocket, "JOIN");
                            send.join();
                        }
                        break;
                    case 2: //SEARCH
                        if (!clientSocket.isClosed()) { // Se o SOCKET estiver aberto, uma solicitação de SEARCH é enviada
                            PeerThreadSend send = new PeerThreadSend(peer, clientSocket, "SEARCH");
                            send.join();
                        } else {
                            System.err.println("O peer não está conectado ao servidor!"); // exibe aviso de que o peer não está conectado
                        }
                        break;
                    case 3: //DOWNLOAD
                        if (!clientSocket.isClosed()) { // Se o SOCKET estiver aberto, uma solicitação de DOWNLOAD é enviada
                            peer.setAnswer(false); //sinaliza que ainda não recebeu retorno do outro peer
                            initDownload(peer); // inicia o processo de download
                            boolean run = false;
                            while(!run){// aguarda o retorno do outro peer
                                run = peer.getAnswer();
                            }
                            sleep(35000); // espera o retorno do Download
                            break;
                        } else {
                            System.err.println("O peer não está conectado ao servidor!"); // exibe aviso de que o peer não está conectado
                        }
                        break;
                    case 4: //LEAVE
                        if (!clientSocket.isClosed()) { // Se o SOCKET estiver aberto, uma solicitação de LEAVE é enviada
                            PeerThreadSend send = new PeerThreadSend(peer, clientSocket, "LEAVE");
                            send.join();
                        } else {
                            System.err.println("O peer não está conectado ao servidor!"); // exibe aviso de que o peer não está conectado
                        }
                        break;
                }
            } catch (Exception e) {
                System.err.println("Opção inválida!"); //mensagem de warning sobre nova tentativa de conexão
            }
    }

    // Tenta um retorno do Servidor para o JOIN, LEAVE ou SEARCH - Recebe o DatagramSocket e a opção de comunicação desejada
    public static void tryConect (Peer peer, DatagramSocket clientSocket, String opt) throws IOException, InterruptedException {
        InetAddress serverAddr = InetAddress.getByName("127.0.0.1"); // IP padrão do Servidor
        int serverPort = 10098; // porta do Servidor para conectar com os peers
        Mensagem msgServer = new Mensagem();
        switch (opt) { // Prepara a mensagem para enviar ao servidor
            case "START": //Coloca os dados necessários para o JOIN inicial
                msgServer.setIpPortPeer(peer.getIp(),peer.getPort());
                msgServer.setOption("JOIN");
                msgServer.setFilesPeer(peer.getFiles());
                break;

            case "JOIN": //Coloca os dados necessários para o JOIN
                PeerConfig(peer);
                msgServer.setIpPortPeer(peer.getIp(),peer.getPort());
                msgServer.setOption(opt);
                msgServer.setFilesPeer(peer.getFiles());
                break;

            case "SEARCH": //Coloca os dados necessários para o SEARCH
                prepSearch(msgServer);
                msgServer.setIpPortPeer(peer.getIp(),peer.getPort());
                msgServer.setOption(opt);
                break;

            case "LEAVE": //Coloca os dados necessários para o LEAVE
                msgServer.setIpPortPeer(peer.getIp(),peer.getPort());
                msgServer.setOption(opt);
                msgServer.setFilesPeer(peer.getFiles()); // envia o nome dos arquivos para serem removidos do servidor
                break;

            case "UPDATE": //Coloca os dados necessários para o UPDATE
                msgServer.setIpPortPeer(peer.getIp(),peer.getPort());
                msgServer.setOption(opt);
                msgServer.setComment(peer.getLastFile()); // envia o nome do arquivo baixado
                break;

            case "ALIVE_OK": //Coloca os dados necessários para retornar o ALIVE
                msgServer.setIpPortPeer(peer.getIp(),peer.getPort());
                msgServer.setOption(opt);
                break;
        }
        msgServer.id = new Random().nextInt(10000) + 1; // número rândomico entre 1 e 10000
        msgServer.setIdRet(msgServer.id);
        msgServer.ack = false;
        String sendJson = Mensagem.preparaJson(msgServer); //Cria JSON com os dados do peer
        enviaPacket(sendJson,clientSocket,serverAddr,serverPort); //envia o datagrama para o servidor
        setMsgStatus(msgServer.id,false); // coloca a mensegem enviada e o status na hashmap para confimarção
        long timer = System.currentTimeMillis();
        if(opt.equals("LEAVE")){
            while(peer.getRunning() && (System.currentTimeMillis() - timer < 10000)){ //aguarda o peer ser desligado ou timeout (10s) no envio da mensagem
            }
        }else {
            while (!Mensagem.ack && (System.currentTimeMillis() - timer < 10000)) { // esperar enquanto não receber o ack ou o contador(10s) não enviar o alerta
            }
        }
        if (!Mensagem.ack) { // se não recebeu o ack reenvia a mensagem
            Mensagem.enviaPacket(sendJson, clientSocket, serverAddr, serverPort); //envia o datagrama para o servidor
            while (!Mensagem.ack && (System.currentTimeMillis() - timer < 10000)) { // esperar enquanto não receber o ack ou o contador(10s) não enviar o alerta
            }
        }
        if (!Mensagem.ack) { // Se ainda não receber, exibir mensagem de erro
            System.err.println("Problema de conexão com o servidor!"); // exibe aviso de que o peer não está conectado
        }
        sleep(500); //aguarda a mensagem de resposta ser impressa
}


    //Solicita um IPv4, uma porta e uma pasta para o Peer, os quais devem der inseridos pelo teclado
    public static void PeerConfig (Peer peer){
        Scanner keyboard = new Scanner(System.in);
        System.out.println("Digite seu IPv4 (ou localhost) e a porta(entre 1024 e 65535) para conexão (e.g., 0.0.0.0:1024, localhost:1024) : ");
        String ip = keyboard.next(); //váriavel que captura o IPv4 e a porta do peer
        try { //verifica se o formato do IP foi digitado corretamente
            String[] ipPort = ip.toLowerCase().split(":"); //separa IP e porta em duas strings
            if (ipPort.length != 2) {
                System.err.println("Formato IP:Porta incorreto (não é do tipo 0.0.0.0:1024, localhost:1024)"); // exibe mensagem de erro sobre formato incorreto do IP e Porta digitados
                throw new Exception("Exception thrown"); // lança uma exceção
            }
            if (Integer.parseInt(ipPort[1]) < 1024 || Integer.parseInt(ipPort[1]) > 65535) {
                System.err.println("Intervalo da porta digitada é inválido (não está entre 1024 e 65535)"); // exibe mensagem de erro sobre intervalo da porta digitada
                throw new Exception("Exception thrown"); // lança uma exceção
            }
            if(ipPort[0].equals("localhost")){
                ipPort[0] = "127.0.0.1"; //define o localhost como 127.0.0.1
            }
            String[] ipName = ipPort[0].split("[\\.]");
            if (ipName.length != 4) {
                System.err.println("Formato do IP digitado é inválido (não é do tipo 0.0.0.0 ou localhost)"); // exibe erro se o IP não foi digitado corretamente - tamanho incorreto
                throw new Exception("Exception thrown"); // lança uma exceção
            } else if (!ipPort[0].equals("localhost")
                    && ((Integer.parseInt(ipName[0]) < 0 || Integer.parseInt(ipName[0]) > 255)
                    || (Integer.parseInt(ipName[1]) < 0 || Integer.parseInt(ipName[1]) > 255)
                    || (Integer.parseInt(ipName[2]) < 0 || Integer.parseInt(ipName[2]) > 255)
                    || (Integer.parseInt(ipName[3]) < 0 || Integer.parseInt(ipName[3]) > 255))) {
                System.err.println("Intervalo do IP digitado é inválido (não está entre 0.0.0.0 e 255.255.255.255 ou é localhost"); // exibe erro se o IP não foi digitado corretamente - fora do intervalo válido
                throw new Exception("Exception thrown"); // lança uma exceção
            }
            System.out.println("Digite o endereço da pasta onde se encontram os arquivos: ");
            String folder = keyboard.next(); //váriavel que captura o endereço do diretório
            File dir = new File(folder); // Armazena os arquivos da pasta
            if (dir.exists() && dir.isDirectory()) { //verifica se foi digitado um diretório válido
                ArrayList<String> files = new ArrayList<String>(Arrays.asList(dir.list()));
                Collections.sort(files); // Ordena os arquivos em ordem crescente
                //seta as especificações do peer
                peer.setIp(ipPort[0]);
                peer.setPort(ipPort[1]);
                peer.setFiles(files);
                peer.setFolder(folder);
            } else {
                System.err.println("Diretório inválido"); // exibe erro se o diretório digitado não é válido
                throw new Exception("Exception thrown"); // lança uma exceção
            }
        } catch (Exception e) {
            // se ocorrer erros durante o processamento dos dados fornecidos
            System.err.println("Nova tentativa de conexão!"); //mensagem de warning sobre nova tentativa de conexão
            PeerConfig(peer);
        }

    }

    public static void prepSearch (Mensagem msgServer){
        Scanner keyboard = new Scanner(System.in);
        System.out.print("\nDigite o nome do arquivo desejado com a extensão (e.g., aula.mp4): ");
        String name = keyboard.next(); //váriavel com o nome do arquivo que será buscado
        msgServer.setComment(name); // atualiza a mensagem com o nome do arquivo
    }

// Inicia o processo de download
    public static void initDownload (Peer peer){
        Scanner keyboard = new Scanner(System.in);
        System.out.print("\nDigite o nome do arquivo para download com a extensão (e.g., aula.mp4): ");
        String fileDown = keyboard.next();
        System.out.println("\nDigite o IPv4 e a porta do peer (e.g., 0.0.0.0:0000) que contém o arquivo para DOWNLOAD: ");
        String ip = keyboard.next(); //váriavel que captura o IPv4 do peer
        try { //verifica se o formato do IP foi digitado corretamente
            String[] ipPort = ip.toLowerCase().split(":"); //separa IP e porta em duas strings
            if (ipPort.length != 2) {
                System.err.println("Formato IP:Porta incorreto (não é do tipo 0.0.0.0:1024, localhost:1024)"); // exibe mensagem de erro sobre formato incorreto do IP e Porta digitados
                throw new Exception("Exception thrown"); // lança uma exceção
            }
            if (Integer.parseInt(ipPort[1]) < 1024 || Integer.parseInt(ipPort[1]) > 65535) {
                System.err.println("Intervalo da porta digitada é inválido (não está entre 1024 e 65535)"); // exibe mensagem de erro sobre intervalo da porta digitada
                throw new Exception("Exception thrown"); // lança uma exceção
            }
            if (ipPort[0].equals("localhost")) {
                ipPort[0] = "127.0.0.1"; //define o localhost como 127.0.0.1
            }
            String[] ipName = ipPort[0].split("[\\.]");
            if (ipName.length != 4) {
                System.err.println("Formato do IP digitado é inválido (não é do tipo 0.0.0.0 ou localhost)"); // exibe erro se o IP não foi digitado corretamente - tamanho incorreto
                throw new Exception("Exception thrown"); // lança uma exceção
            } else if (!ipPort[0].equals("localhost")
                    && ((Integer.parseInt(ipName[0]) < 0 || Integer.parseInt(ipName[0]) > 255)
                    || (Integer.parseInt(ipName[1]) < 0 || Integer.parseInt(ipName[1]) > 255)
                    || (Integer.parseInt(ipName[2]) < 0 || Integer.parseInt(ipName[2]) > 255)
                    || (Integer.parseInt(ipName[3]) < 0 || Integer.parseInt(ipName[3]) > 255))) {
                System.err.println("Intervalo do IP digitado é inválido (não está entre 0.0.0.0 e 255.255.255.255 ou é localhost"); // exibe erro se o IP não foi digitado corretamente - fora do intervalo válido
                throw new Exception("Exception thrown"); // lança uma exceção
            }
            peer.setSolicitedFile(fileDown); // armazena o nome do arquivo solicitado
            PeerThreadSendStringTCP sendTCP = new PeerThreadSendStringTCP(peer,ipPort[0],Integer.parseInt(ipPort[1]),fileDown);
            sendTCP.start();
            sendTCP.join();

        }catch (Exception e) {
            // se ocorrer erros durante o processamento dos dados fornecidos
            System.err.println("Nova tentativa de download!"); //mensagem de warning sobre nova tentativa de conexão
            initDownload(peer);
        }
    }

    // Cria objeto que recebe o cabeçalho da mensagem que será enviada e retorna uma string json
    public static String preparaJson(Mensagem msg) {
        Gson sendgson = new Gson(); // instância para gerar a string json de envio
        return sendgson.toJson(msg);
    }

    // Envia o pacote com a mensagem no formato JSON através do socket para o IP e Porta do Servidor (127.0.0.1:10098)
    public static void enviaPacket(String jsonMsg, DatagramSocket clientSocket, InetAddress ipServer, int portServer) throws IOException {
        byte[] sendData; // buffer de envio
        sendData = (jsonMsg).getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ipServer, portServer); //cria datagrama de envio
        clientSocket.send(sendPacket); // envia pacote com a mensagem
    }

    // Envia o ACK para o peer
    public static void setACK(String msg, DatagramPacket recPkt, DatagramSocket serverSocket) throws IOException {
        byte[] sendBuf; // Buffer para armazenar os bytes do ACK

        sendBuf = msg.getBytes(); //Prepara o buffer com a String que o servidor vai enviar

        InetAddress IPAddress = recPkt.getAddress();

        int port = recPkt.getPort();

        DatagramPacket sendPacket = new DatagramPacket(sendBuf, sendBuf.length, IPAddress, port);

        serverSocket.send(sendPacket);
    }

    // Trata as mensagens recebidas dos peers e envia um ACK
    public static void sendACK(DatagramSocket serverSocket, DatagramPacket recPkt, ConcurrentHashMap<AbstractMap.SimpleEntry<String, String>,Boolean> ip_port_peers, ConcurrentHashMap<String, ArrayList<String>> files_peers) throws IOException {

        Gson recgson = new Gson(); //instância para gerar a mensagem a partir string json do cliente

        String informacao = new String(recPkt.getData(), recPkt.getOffset(), recPkt.getLength()); //Datagrama do cliente é convertido em String json

        Mensagem msg = recgson.fromJson(informacao, Mensagem.class);  //gera a mensagem a partir da string json recebida do cliente
        // verifica a comunicação do peer
        switch (msg.getOption()) {
            case "JOIN":
                if (ip_port_peers.containsKey(msg.getIpPortPeer())) { // se já houver algum peer com o mesmo IP, o JOIN é negado
                    msg.setOption("JOIN_DONE");
                    setACK(preparaJson(msg), recPkt, serverSocket); // envia o ACK para o cliente
                } else {
                    ip_port_peers.put(msg.getIpPortPeer(),true); // Adiciona o IP e a porta do peer na lista do HashMap
                    for(String file : msg.filesPeer) {  // verifica todos os arquivos
                        if(!files_peers.containsKey(file)){ // se não houver o arquivo no HashMap, uma nova entrada é adicionada
                            files_peers.put(file, new ArrayList<>(Arrays.asList(msg.getIpPeer() + ":" + msg.getPortPeer()))); //concatena o Ip com a porta do peer e adiciono na Hashmap
                        }else {
                            files_peers.get(file).add(msg.getIpPeer() + ":" + msg.getPortPeer()); //caso algum peer já possua o arquivo, apenas mais um IP é acrescentado no Array indicando o outro peer que também possui
                        }
                    }
                    msg.setOption("JOIN_OK");
                    setACK(preparaJson(msg), recPkt, serverSocket); // envia string ACK para o cliente
                    new ServerThreadSendAlive(serverSocket, recPkt, ip_port_peers, files_peers); // ativa o ALIVE
                    if(!msg.getFilesPeer().isEmpty()) {
                        System.out.println("Peer " + (msg.getIpPeer()) + ":" + msg.getPortPeer() + " adicionado com arquivos " + (msg.getFilesPeer()).toString().replace("[","").replace("]","").replace(",","")); // imprime no prompt do servidor
                    }
                    else{
                        System.out.println("Peer " + (msg.getIpPeer()) + ":" + msg.getPortPeer() + " adicionado com arquivos " + (msg.getFilesPeer()).toString()); // imprim
                    }

                }
                break;
            case "SEARCH":
                System.out.println("Peer " + (msg.getIpPeer()) + ":" + msg.getPortPeer() + " solicitou arquivo " + msg.getComment()); // imprime no prompt do servidor o arquivo solicitado pelo peer
                if(files_peers.containsKey(msg.getComment())) {
                    String searchList = files_peers.get(msg.getComment()).toString().replace("[", "").replace("]", "");
                    msg.setComment(searchList);
                }else {
                    msg.setComment("");
                }
                setACK(preparaJson(msg), recPkt, serverSocket); // envia string ACK para o cliente com lista de IPs e Portas de peers que ppossuem o arquivo
                break;
            case "LEAVE":
                ip_port_peers.remove(new AbstractMap.SimpleEntry<>(msg.getIpPeer(),msg.getPortPeer()));
                files_peers.forEach((key, list) -> { //apaga o ip e porta do peer
                    // remove o IP do hashmap de arquivos
                    list.removeIf(uuid -> uuid.equals(msg.getIpPeer()+":"+msg.getPortPeer()));
                });
                files_peers.entrySet()
                        .removeIf(e -> Objects.isNull(e.getValue()) ||
                                (e.getValue() instanceof Collection && ((Collection) e.getValue()).isEmpty())); // remove se hover algum arquivo sem peer

                msg.setOption("LEAVE_OK");
                setACK(preparaJson(msg), recPkt, serverSocket); // envia string ACK para o cliente
                break;
            case "UPDATE": // no comment da Mensagem recebida do peer há o nome do arquivo baixado, com essa informação e com o IP  e Porta do Peer é possível atualizar o hashmap
                if (files_peers.get(msg.getComment()) == null) { // se não houver o arquivo no HashMap, uma nova entrada é adicionada
                    files_peers.put(msg.getComment(), new ArrayList<>(Arrays.asList(msg.getIpPeer() + ":" + msg.getPortPeer()))); // Um array com o IP do peer que contêm o arquivo é adicionado
                } else {
                    files_peers.get(msg.getComment()).add((msg.getIpPeer() + ":" + msg.getPortPeer())); //caso algum peer já possua o arquivo, apenas mais um IP é acrescentado no Array indicando o outro peer que também possui
                }
                msg.setOption("UPDATE_OK");
                setACK(preparaJson(msg), recPkt, serverSocket); // envia string ACK para o cliente com lista de IPs e Portas de peers que ppossuem o arquivo
                break;
            case "ALIVE_OK":
                ip_port_peers.put(new AbstractMap.SimpleEntry<>(recPkt.getAddress().getHostAddress(),String.valueOf(recPkt.getPort())),true); //define o alive como true
                setACK(informacao, recPkt, serverSocket); //confirma o ACK de recebimento
                break;
        }

    }

    public static boolean sendAlive(DatagramSocket sock,DatagramPacket recPkt, ConcurrentHashMap<AbstractMap.SimpleEntry<String, String>,Boolean> ip_port_peers, ConcurrentHashMap<String, ArrayList<String>> files_peers) throws IOException {
        Mensagem alive = new Mensagem();
        AbstractMap.SimpleEntry<String,String>ipPort = new AbstractMap.SimpleEntry<>(recPkt.getAddress().getHostAddress(),String.valueOf(recPkt.getPort())); // IP e Porta do peer
        alive.setOption("ALIVE");
        setACK(preparaJson(alive), recPkt, sock); //envia o alive para o peer
        ip_port_peers.put(ipPort,false); //define o alive temporariamente como false
        long timer = System.currentTimeMillis();
        while(!ip_port_peers.get(ipPort)&&(System.currentTimeMillis() - timer < 10000)){ // aguarda o retorno em até 10 s
        }
        if(!ip_port_peers.get(ipPort)){ // Se não houver retorno, excluir os dados do Peer do Servidor
            ArrayList<String>files = new ArrayList<>();
            ip_port_peers.remove(ipPort);
            for(String file : Collections.list(files_peers.keys())){ //verifica quais arquivos o peer possuia
               if(files_peers.get(file).contains(ipPort.getKey()+":"+ipPort.getValue())){
                   files.add(file);
               }
            }
            Collections.sort(files); // ordena os nomes dos arquivos
            files_peers.forEach((key, list) -> { //apaga o ip e porta do peer
                // remove o IP do hashmap de arquivos
                list.removeIf(uuid -> uuid.equals(ipPort.getKey()+":"+ipPort.getValue()));
            });
            files_peers.entrySet()
                    .removeIf(e -> Objects.isNull(e.getValue()) ||
                            (e.getValue() instanceof Collection && ((Collection) e.getValue()).isEmpty())); // remove se hover algum arquivo sem peer
            System.out.println("Peer " + ipPort.getKey() + ":" + ipPort.getValue() + " morto. Eliminando seus arquivos " + String.join(", ", files).replace(",","")); // imprime na console do servidor
            return false; // Para o alive para o peer
        }
        return true; //continua o alive
    }

    //Recebe o ACK do Servidor e retorna a Mensagem com detalhes da conexão
    public static void ACKfromServer(Peer peer, DatagramSocket clientSocket, DatagramPacket recPkt) throws IOException, InterruptedException {
        clientSocket.receive(recPkt); // recebe o pacote do servidor
        Gson recgson = new Gson(); //instância para gerar a mensagem a partir string json do servidor
        String informacao = new String(recPkt.getData(), recPkt.getOffset(), recPkt.getLength()); //Datagrama do servidor é convertido em String json
        Mensagem msg = recgson.fromJson(informacao, Mensagem.class);  //gera a mensagem a partir da string json recebida do cliente
        boolean isFilesEmpty = peer.getFiles().isEmpty();
        boolean isSearchEmpty;
        if ((msg.getComment()) != null){
            isSearchEmpty = (msg.getComment()).equals("");
        }else if ((msg.getComment()) != null){
            isSearchEmpty = false;
        }else{
            isSearchEmpty = true;
        }
        if(!msg.getOption().equals("ALIVE") && id == msg.getIdRet()){ // se a mensagem tiver o mesmo ack, a mensagem enviada é confirmada
            ack = true;
            setMsgStatus(id,true); //confirma o ack
            switch (msg.getOption()) {
                case "JOIN_OK": // quando receber o retorno do JOIN do servidor
                case "JOIN_DONE": // quando o peer já fez o join anteriormente
                    if(!isFilesEmpty){
                        System.out.println("Sou peer " + peer.getIp() + ":" + peer.getPort() + " com arquivos " + (peer.getFiles()).toString().replace("[","").replace("]","").replace(",","")); // imprime as informações do Peer
                    }
                    else{
                        System.out.println("Sou peer " + peer.getIp() + ":" + peer.getPort() + " com arquivos " + (peer.getFiles()).toString()); // imprime as informações do Peer
                    }
                    break;
                case "SEARCH": // o caso default foi definido como retorno do SEARCH do servidor
                    if(!isSearchEmpty) {
                        String[] ipsList = msg.getComment().split(",");
                        ArrayList<String> ipsListSearch = new ArrayList<>();
                        for (int i = 0; i < ipsList.length; i++) {
                            ipsListSearch.add(ipsList[i].trim());
                        }
                        peer.setPeersSearch(ipsListSearch); //salva a lista de ips e portas
                        System.out.println("peers com arquivo solicitado " + msg.getComment().replace(",","")); // imprime as informações dos Peers que possuem o arquivo solicitado
                    }else{
                        System.out.println("peers com arquivo solicitado " + msg.getComment()); // imprime as informações dos Peers que possuem o arquivo solicitado
                    }
                    break;
                case "LEAVE_OK": // quando receber o retorno do LEAVE do servidor
                    peer.setRunning(false); //desliga o peer
                    break;
                case "UPDATE_OK": // quando receber o servidor retornar a requisição de UPDATE
                    break; // Apenas não faz nova tentativa de envio
                default: // quando receber um ack do ALIVE_OK do servidor
                    break;
            }
        }else{
            new PeerThreadSend(peer,clientSocket,"ALIVE_OK"); // envia um ALIVE_OK
        }

    }
}