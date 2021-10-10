import java.io.*;
import java.util.Scanner;
import java.util.Arrays;

public class Trab2 {
  static int C; //Threads ConsumidorEscritoras
  static final int P = 1; //Threads produtoras

  static int out = 0; //inição do último elemento retirado do buffer
  static int count = 0; //Número de elementos no buffer
  static int escrito = 0; //Última linha do arquivo que foi sobrescrita
  static int[][] buffer = new int[10][];

  public static void main(String[] args) throws Exception{
    File file = new File("Entrada.txt");
    BufferedReader br = new BufferedReader(new FileReader(file));
    String str = br.readLine();
    int Elementos = Integer.parseInt(str); //Quantidade de elementos (primeira in do arquivo de entrada)

    PrintWriter saida = new PrintWriter(new File("Saida.txt"));

    Monitor monitor = new Monitor(); //Monitor

    Scanner scan = new Scanner(System.in);

    System.out.println("Diga a quantidade de threads ConsumidorEscritoras (C): ");
    C = scan.nextInt();
    
    System.out.println("Diga a quantidade de elementos por bloco (N): ");
    int N = scan.nextInt(); //Numero de elementos por bloco

    scan.close();

    int blocos = Elementos/N; //Numero de blocos

    Produtor[] prod = new Produtor[P];
    for (int i = 0; i<P; i++){
      prod[i] = new Produtor(br, N, blocos, monitor);
      prod[i].start();
    }

    ConsumidorEscritor[] cons = new ConsumidorEscritor[C];
    for (int i = 0; i<C; i++){
      cons[i] = new ConsumidorEscritor(i, N, blocos, monitor, saida);
      cons[i].start();
    }
  }
}

class Monitor {
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
    
    notify();
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
    notify();

    return ordena;
  }

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
  protected BufferedReader br;
  protected int N;
  protected int blocos;
  protected Monitor monitor;

  public Produtor(BufferedReader br, int N, int blocos, Monitor monitor){
    this.br = br;
    this.N = N;
    this.blocos = blocos;
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
        
        monitor.Insere(in, aux);
        in = (in+1)%10;
      }
      //System.out.println(Arrays.deepToString(Trab2.buffer));
    } catch (IOException e) {
      System.out.println("ERRO -- br.readline()");
      e.printStackTrace();
    }
  }
}

class ConsumidorEscritor extends Thread {
  protected int id;
  protected int N;
  protected int blocos;
  protected Monitor monitor;
  protected PrintWriter saida;
  
  protected int[] ordena;

  public ConsumidorEscritor(int id, int N, int blocos, Monitor monitor, PrintWriter saida){
    this.id = id;
    this.N = N;
    this.blocos = blocos;
    this.monitor = monitor;
    this.saida = saida;
  }

  public void run(){
    for (int i=id; i<blocos; i+=Trab2.C){
      ordena = monitor.Retira(N);

      Arrays.sort(ordena);

      monitor.Sobresreve(ordena, saida);
    }
  }
}
