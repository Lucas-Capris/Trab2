import java.io.*;
import java.util.Scanner;
import java.util.Arrays;

public class Trab2 {
  static int C; //Threads ConsumidorEscritoras
  static final int P = 1; //Threads produtoras

  static int out = 0; //Posicao do elemento a ser retirado do buffer
  static int count = 0; //Numero de elementos no buffer
  static int escrito = 0; //Ultima linha do arquivo que foi sobrescrita
  static int[][] buffer = new int[10][];

  public static void main(String[] args) throws Exception{
    File file = new File("Entrada.txt"); //Arquivo a ser lido
    BufferedReader br = new BufferedReader(new FileReader(file));
    String str = br.readLine();
    int Elementos = Integer.parseInt(str); //Quantidade de elementos (primeira linha do arquivo de entrada)

    PrintWriter saida = new PrintWriter(new File("Saida.txt"));

    Monitor monitor = new Monitor(); //Monitor

    Scanner scan = new Scanner(System.in);

    System.out.println("Diga a quantidade de threads ConsumidorEscritoras (C): ");
    C = scan.nextInt();
    
    System.out.println("Diga a quantidade de elementos por bloco (N): ");
    int N = scan.nextInt(); //Numero de elementos por bloco

    scan.close();

    int blocos = Elementos/N; //Numero de blocos

    //Criando as threads
    Produtor[] prod = new Produtor[P];
    for (int i = 0; i<P; i++){
      prod[i] = new Produtor(br, N, monitor);
      prod[i].start();
    }

    ConsumidorEscritor[] cons = new ConsumidorEscritor[C];
    for (int i = 0; i<C; i++){
      cons[i] = new ConsumidorEscritor(i, N, blocos, monitor, saida);
      cons[i].start();
    }
  }
}

class Monitor { //Monitor para fazer a exclusao mutua entre as threads
  public synchronized void Insere(int in, int[] linha){
    //Sem necessidade de ser while visto que só há uma thread produtora, não podendo haver incremento no valor de count antes da liberação do wait()
    if (Trab2.count == 10){
      try {
        wait();
      } catch (InterruptedException e) {
        System.out.println("ERRO -- wait()");
        e.printStackTrace();
      }
    }

    Trab2.buffer[in] = linha;
    Trab2.count++;
    
    notify(); //Liberando a thread para consumir caso haja alguma em wait()
  }

  public synchronized int[] Retira(int N){
    int[] ordena = new int[N];

    /*Sem necessidade de ser while visto que os únicos dois método que notify são synchronized, 
    não podendo haver múltiplas chamadas de notify antes de que uma outra thread fique barrada no check do if*/
    if (Trab2.count == 0){
      try {
        wait();
      } catch (InterruptedException e) {
        System.out.println("ERRO -- wait()");
        e.printStackTrace();
      }
    }

    for (int i=0; i<N; i++){
      ordena[i] = Trab2.buffer[Trab2.out][i];
    }

    Trab2.out = (Trab2.out+1)%10;

    Trab2.count--;
    notify(); //Liberando a thread para escrever caso haja alguma em wait()

    return ordena; //Bloco a ser ordenado pelas threads
  }

  //Faz escrita no arquivo de saída. Uso do "synchronized" garante exclusão mútua
  public synchronized void Sobresreve(int[] ordena, PrintWriter saida){
    String str = Arrays.toString(ordena);
    str = str.substring(1, str.length() - 1);
    str = str.replaceAll(",","");
    saida.println(str);
    saida.flush();
    Trab2.escrito++;
  }
}

class Produtor extends Thread {
  private BufferedReader br;
  private int N;
  private Monitor monitor;
  
  //Construtor da classe
  public Produtor(BufferedReader br, int N, Monitor monitor){
    this.br = br;
    this.N = N;
    this.monitor = monitor;
  }

  public void run(){
    String str = "";
    int in = 0;
    int i;

    //Preenchendo o buffer com os dados do arquivo
    try {
      while ((str = br.readLine()) != null){
        String[] parts = str.split(" ");
        int[] aux = new int[N];

        for (i = 0; i<N; i++){
          aux[i] = Integer.parseInt(parts[i]);
        }
        
        monitor.Insere(in, aux); //Metodo que insere elemento no buffer
        in = (in+1)%10;
      }
    } catch (IOException e) {
      System.out.println("ERRO -- br.readline()");
      e.printStackTrace();
    }
  }
}

class ConsumidorEscritor extends Thread {
  private int id;
  private int N;
  private int blocos;
  private Monitor monitor;
  private PrintWriter saida; //Arquivo saida
  
  private int[] ordena; //Bloco a ser ordenado

  //Construtor da classe
  public ConsumidorEscritor(int id, int N, int blocos, Monitor monitor, PrintWriter saida){
    this.id = id;
    this.N = N;
    this.blocos = blocos;
    this.monitor = monitor;
    this.saida = saida;
  }

  public void run(){
    //Threads executaram de forma alternada
    for (int i=id; i<blocos; i+=Trab2.C){
      ordena = monitor.Retira(N); //Metodo que retorna o bloco de tamanho N retirado do buffer

      Arrays.sort(ordena); //Sort() ordenando o bloco em ordem crescente

      monitor.Sobresreve(ordena, saida); //Metodo que faz a escrita no arquivo de saida
    }
  }
}
