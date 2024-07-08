## Projeto mini napster

### Sobre:
Projeto de Sistemas Distribuídos P2P em JAVA.

A proposta desse projeto é aplicar o conhecimento sobre a arquitetura P2P.


### Premissa:

1. Definição do Sistema

Criar um sistema P2P que permita a transferência de arquivos de vídeo gigantes (mais de 1 GB) entre peers, intermediados por um servidor centralizado, utilizando TCP e UDP como protocolo da camada de transporte.

O sistema funcionará de forma similar (porém muito reduzida) ao sistema Napster, um dos primeiros sistemas P2P, criado por Shawn Fanning aos 18 anos.

### Arquitetura: 

2. Servidor

	O servidor cuja classe é Servidor.java, possui o IP 127.0.0.1  e atende aos peers através de conexão UDP na porta 10098. Nas linhas 20 e 21 foram implementadas as estruturas para armazenar os dados dos peers no servidor, no caso, optou-se pelo ConcurrentHashMap para sincronizar o acesso simultâneo realizado pelas threads. No total são dois  ConcurrentHashMap, um para armazenar apenas o IP e porta de cada peer e se está ativos, e outro para armazenar os arquivos de cada peer.
	Para o envio e recebimento de mensagens utilizou-se Threads, sendo a ServerThread para responder via UDP as solicitações dos peers (linha 42), tal que, para cada nova solicitação uma nova Thread é executada. Outra Thread da classe servidor é a ServerThreadSendAlive (linha 75), destinada para verificar se o peer permanece ativo, enviando uma solicitação UDP de confirmação a cada 30s a partir do JOIN. 
	Quanto as tratativas e envios das mensagens recebidas são feitas através da classe Mensagem.java (linhas 328 à 400), tendo como base o switch case verifica-se a mensagem recebida do peer que podem ser: JOIN, SEARCH, LEAVE, UPDATE, ALIVE_OK. Tanto para envio quanto para recebimento, encapsula-se a mensagem em uma string json através da biblioteca externa Gson versão 2.9.0. Para o ALIVE, as mensagens também são string json e são preparadas no método sendAlive da classe Mensagem (linhas 402 a 431).
	Uma vez que o Alive não é respondido, os dados do peer são excluídos do servidor (linhas 411 a 428 da classe Mensagem). O mesmo ocorre quando um peer solicita o LEAVE (linhas 372 a 383 da classe Mensagem).
	O servidor permanece on-line mesmo que não haja peers conectado.

3. Peers

	Nos peers (vide Peer.java) há duas possibilidades de envio: UDP e TCP. O UDP é usado para comunicação com o servidor enviando e respondendo requisições de JOIN, SEARCH, LEAVE, UPDATE e ALIVE, enquanto o TCP trata as requisições e envios de DOWNLOADS entre os peers.
	Todo peer precisa de IP e porta fornecidos pelo usuário, além de diretório para troca de arquivos que pode estar vazio ou não. Esses dados são adicionados nas especificações do peer (linhas 19 à 29 da classe Peer.java), além dos dados já citados há variáveis para armazenar o último download, o último arquivo solicitado, o status de execução do peer, a última busca no servidor e status de recebimento da última mensagem enviada a outro peer.
	Assim como no servidor, o envio e recebimento de mensagens é feito através de Threads, sendo a PeerThreadSend (linha 101) a responsável por interagir enviando via UDP ao servidor,  a PeerThreadReceiver (linha 127) a responsável por receber o retorno do servidor, a PeerThreadDownload (linha 160) responsável por tratar as solicitações TCP de download de outros peers, a PeerThreadSendStringTCP (linha 261) responsável por enviar strings json via TCP a outros peers e a PeerThreadSendFileTCP (linha 307) responsável por enviar arquivos via TCP a outros peers.
	O projeto comporta enviar grandes arquivos, entre eles, arquivos de vídeo (.mp4), para isso utilizou-se o DataOutPutStream (linhas 333 à 350) enviando na seguinte ordem: primeiro um char para diferenciar da string (adotou-se s para string e f para arquivo); depois um texto em UTF com o nome do arquivo; em seguida um Long Int com o tamanho do arquivo; e, por fim, o arquivo em sequência de bytes. Todos os dados são consumidos pelo DataInputStream do peer cliente (174 à  246) e para funcionar tanto para string (linhas 283 à 293) quanto para arquivos a mesma sequência de envio deve ser seguida.
	Todas as trocas de mensagens utilizaram a classe Mensagem.java, sendo que as tratativas e retentativas de envio foram configuradas utilizando timer e dados de confirmação de recebimento, caso alguma mensagem chegue fora de ordem, ela é descartada, caso não tenha retorno nas mensagens com o servidor, uma nova tentativa é feita (vide linhas 127 à 191 e 434 à 484 da classe Mensagem.
	As interações na interface do peer são feitas inicialmente digitando IP, porta e diretório, e em seguida, a partir de um menu que permite: JOIN, SEARCH, DOWNLOAD e LEAVE. Algumas tratativas foram implantadas prevendo erros de digitação de IP, porta e diretório, permitindo assim uma nova tentativa.

4. Compilação do código fonte

1 - Para compilar o projeto, o primeiro passo é ter a biblioteca gson-2.9.0 e o JDK 1.8;

2 - O gson utilizado foi obtido do repositório Maven (https://mvnrepository.com/artifact/com.google.code.gson/gson/2.9.0) no formato .jar (https://repo1.maven.org/maven2/com/google/code/gson/gson/2.9.0/gson-2.9.0.jar);

3 - No Ubuntu/Debian, deve-se adicionar o gson-2.9.0.jar ao CLASSPATH, uma maneira é no terminal: nano ~/.bashrc; adicione export CLASSPATH=$CLASSPATH:/diretorio_em_que_esta_a_biblioteca/gson-2.9.0.jar; após salvar as alterações basta executar source ~/.bashrc

4 - Com o gson configurado pode-se iniciar a compilação, sendo que todas classes devem ser compiladas juntas, assim no diretório que se encontra as classes, executar: javac -d . Mensagem.java Peer.java Servidor.java

5 – Em seguida pode-se inicializar o servidor: java p2p.Servidor

6 – Agora já pode-se inicializar os peers: java p2p.Peer

### Referências:

Material das aulas:
     https://www.youtube.com/watch?v=FOwKxw9VYqI 
      https://www.baeldung.com/udp-in-java
      https://www.geeksforgeeks.org/working-udp-datagramsockets-java/
      https://docs.oracle.com/javase/tutorial/networking/datagrams/index.html
      https://www.baeldung.com/java-inputstream-server-socket

### Contato:

LinkedIn: https://www.linkedin.com/in/renanflimabr