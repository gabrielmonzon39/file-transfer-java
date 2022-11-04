import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;  
public class Routing extends Thread{

    static final int PORT = 9080;
    static final int infinity = 99;

    static final int defaultT = 5; // 30
    static final int defaultU = 30; // 90
    static int timerT = defaultT;
    static int timerU = defaultU;
    static String myLetter;
    
    static HashMap<String, Costo> DvReceived;
    static HashMap<String, Costo> changed = new HashMap<>();
    static HashMap<String, Costo> Dv;
    static Hosts myHost;
    static FileWriter hostsWriter;
    public static HashMap<String, Integer> timeRemaining = new HashMap<>();

    public static ArrayList<String> vecinos = new ArrayList<>();

    static String from;

    public static void main(String[] args) throws Exception {
        myLetter = args[0];

        //***********    Leer tiempos T y U    ***********// 
        try {
            timerT = Integer.parseInt(args[1]);
        } catch (Exception e) {}
        try {
            timerU = Integer.parseInt(args[2]);
        } catch (Exception e) {}

        //***********    Realizar inicializaciones    ***********// 
        BufferedReader br = new BufferedReader(new FileReader("config.txt"));
        String line = null;
        Dv = new HashMap<String, Costo>();
        hostsWriter = new FileWriter("./hosts.txt", true);
        hostsWriter.write("\n");
        
        //***********    Leer el archivo    ***********// 
        while ((line = br.readLine()) != null) {
            String[] dir = line.split("-");

            String letter = dir[0].trim();
            int costo = Integer.parseInt(dir[1].trim());
            String ip = dir[2].trim();

            Dv.put(letter, new Costo(ip, costo, letter));

            vecinos.add(letter);
            timeRemaining.put(letter, timerU);
            hostsWriter.write(letter+"-"+ip+"\n");
        }
        br.close();
        hostsWriter.close();

        //***********    Leemos nuestra dirección    ***********// 
        myHost = new Hosts();

        //***********    Creamos la primera instancia del archivo    ***********// 
        writeToFile(Dv);
        
        DatagramSocket ds = new DatagramSocket();

        //***********    Mandamos la tabla    ***********// 
        Set<String> keys = Dv.keySet();
        for (String key : keys) {
            sendHello(key, myHost);
            send(Messages.makeDvSend(myHost.getMyAddress(), Dv),key);
        }

        
        //***********    Crea sockets para recibir mensajes    ***********//
        ServerSocket serverSocket = new ServerSocket(PORT); 
        //DatagramSocket dr = new DatagramSocket(PORT);
        //byte[] receive = new byte[65535];
        MyTimer.init(timerT, timerU);
        
        //***********    Esperamos las llamadas    ***********// 
        DatagramPacket DpReceive = null;
        boolean hasChange = false;

        
        while (true) {
            Socket socket = serverSocket.accept();
            Routing routing = new Routing();
            routing.run(socket);            
        }
    }

    public void run (Socket socket) {
        DatagramPacket DpReceive = null;
        boolean hasChange = false;
        DataOutputStream dataOutputStream = null;
        DataInputStream dataInputStream = null;

        while (true) {
            String request = "";

            try {
                char[] buffer = new char[1000];
                dataInputStream  = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                request = dataInputStream.readUTF();
            } catch (IOException e1) {
                ConsoleLog.printError();
            }
            
            String data = data(request.getBytes()).toString();
            
            if (data.toLowerCase().contains("hello")) {
                sendWelcome(data, myHost);
            } else {
                decode(data);
                resetTimeExceeded(from);
            }
            
            //***********    Hacemos el algoritmo    ***********// 
            if (DvReceived == null) continue;
            Set<String> receiveKeys = DvReceived.keySet();
            for (String key : receiveKeys) {
                if (Dv.get(key) == null) {
                    Dv.put(key, new Costo(Dv.get(from).ip, Dv.get(from).costo + DvReceived.get(key).costo, from));
                    changed.put(key, new Costo(Dv.get(from).ip, Dv.get(from).costo + DvReceived.get(key).costo, from));
                    try {
                        hostsWriter = new FileWriter("./hosts.txt", true);
                        hostsWriter.write(key+"\n");
                        hostsWriter.close();
                    } catch (IOException e) {
                        ConsoleLog.printError();
                    }
                    hasChange = true;
                    continue;
                }
                if (DvReceived.get(key).costo + Dv.get(from).costo < Dv.get(key).costo) {
                    hasChange = true;
                    Dv.replace(key, new Costo(DvReceived.get(key).ip, DvReceived.get(key).costo + Dv.get(from).costo, DvReceived.get(key).link));
                    changed.put(key, new Costo(DvReceived.get(key).ip, DvReceived.get(key).costo + Dv.get(from).costo, DvReceived.get(key).link));
                }
            }
            
            //***********    Escribe en el archivo el nuevo vector    ***********// 
            if (hasChange) {
                writeToFile(Dv);
                hasChange = false;
            }
        }
    }

    public static String getIpFromLetter (String hostletter) {
        try (BufferedReader br = new BufferedReader(new FileReader("config.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] dir = line.split("-");
                String letter = dir[0].trim();
                if (letter.equals(hostletter)) {
                    return dir[2].trim();
                }
            }
            br.close();
        } catch (Exception e) {}
        return null;
    }

    public static void resetTimeExceeded (String vecino) {
        timeRemaining.replace(vecino, timerU);
    }
 
    public static void sendTimeoutMessage () throws SocketException {
        DatagramSocket ds = new DatagramSocket();
        Hosts myHost = new Hosts();
        for (String vecino : vecinos) {
            if (changed.size() == 0) {
                send(Messages.makeKeepAlive(myHost.getMyAddress()), vecino);
            } else {
                send(Messages.makeDvSend(myHost.getMyAddress(), changed), vecino);
            }
        }  
        changed = new HashMap<>();
    }

    public static void writeToFile (HashMap<String, Costo> Dv) {
        String outputFilePath = "./RoutingTable.txt";
        File file = new File(outputFilePath);

        if (file.exists()) {
            file.delete();
        }
        try {
            FileWriter writer = new FileWriter(outputFilePath, true);
            Set<String> keys = Dv.keySet();
            for (String key : keys) {
                writer.write(key + ";" + Dv.get(key).costo + ";" + Dv.get(key).link + "\n");
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void decode (String data) {
        String[] dataDecoded = data.split("\n");
        from = dataDecoded[0].split(":")[1].trim();
        int size = Integer.parseInt(dataDecoded[2].split(":")[1].trim());
        DvReceived = new HashMap<>();

        int offset = 3;
        for (int i = 0; i < size; i++) {
            String[] entry = dataDecoded[i+offset].split(":");
            String hostLetter = entry[0].trim();
            DvReceived.put(hostLetter, new Costo(getIpFromLetter(hostLetter), Integer.parseInt(entry[1].trim()), null));
        }
    }

    public static StringBuilder data(byte[] a) {
        if (a == null) return null;
        StringBuilder ret = new StringBuilder();
        
        for (int i = 0; i < a.length; i++) {
            if (a[i] != 0) {
                ret.append((char) a[i]);
            }
        }
        return ret;
    }

    static void send(String dv, String letter)  {
        String ip = getIpFromLetter(letter);
        System.out.println("SEND TO -> " + letter + " (" + ip + ")");
        try (Socket socket = new Socket(ip, PORT)) {
 
            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
 
            dataOutputStream.write(dv.getBytes());
            dataOutputStream.flush();

            //System.out.println(dv);

            dataInputStream.close();
            dataOutputStream.close();

            socket.close();
        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("I/O error: " + ex.getMessage());
        }
    }

    static void sendHello(String Ip, Hosts host)  {
        String message = Messages.makeInitialMessage(host.getMyAddress());
        send(message, Ip);
    }

    static void sendWelcome(String data, Hosts host)  {
        String Ip = data.split("\n")[0].split(":")[1].trim();
        String message = Messages.ResponseInitialMessage(host.getMyAddress());
        
        // LOG
        ConsoleLog.printRoutingMessage(Ip);
        Log.makeLogRouting(Ip);

        send(message, Ip);
    }

}