package p2p;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import com.google.gson.Gson;

// Cabeçalho que será usado para as mensagens
public class Mensagem {
    private String id; //id da mensagem
    private String mensagem; //conteúdo da mensagem
    private String tamanho; //tamanho da mensagem em bytes

    // Construtores da classe
    public Mensagem(){

    }

    //Seção 3: Cabeçalho da mensagem que será transmitida
    public Mensagem(String id, String msg){
        this.id = id;
        this.mensagem = msg;
        this.tamanho = Integer.toString((msg.length()));
    }

    // Métodos setters e getters

    public String getMensagem(){
        return this.mensagem;
    }

    public String getId(){
        return this.id;
    }

    public String getTamanho(){
        return this.tamanho;
    }

    public void setId(String idt){
        this.id = idt;
    }

    public void setMensagem(String msg){
        this.mensagem = msg;
    }

    public void setTamanho(int tmh){
        this.tamanho = Integer.toString(tmh);
    }


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

    // Envia o pacote com a string json através do socket
    public static void enviaPacket (String jmsgudp, DatagramSocket clientSocket, InetAddress IPAddress) throws IOException{
        byte[] sendData = new byte [1024]; // buffer de envio
        sendData = (jmsgudp).getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876); //cria datagrama de envio
        clientSocket.send(sendPacket); // envia pacote conforme opção
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
