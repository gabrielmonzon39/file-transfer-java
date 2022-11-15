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

public class Routing extends Thread {

    // static final int PORT = 9080;
    static final int PORT = 5790;
    static final int infinity = 99;

    static final int defaultT = 30; // 30
    static final int defaultU = 90; // 90
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

        // *********** Leer tiempos T y U ***********//
        try {
            timerT = Integer.parseInt(args[1]);
        } catch (Exception e) {
            //System.out.println("Error parseando tiempo T");
        }
        try {
            timerU = Integer.parseInt(args[2]);
        } catch (Exception e) {
            //System.out.println("Error parseando tiempo U");
        }

        // *********** Realizar inicializaciones ***********//
        BufferedReader br = new BufferedReader(new FileReader("config.txt"));
        String line = null;
        Dv = new HashMap<String, Costo>();
        hostsWriter = new FileWriter("./hosts.txt", true);
        hostsWriter.write("\n");

        // *********** Leer el archivo ***********//
        while ((line = br.readLine()) != null) {
            String[] dir = line.split("-");

            String letter = dir[0].trim();
            int costo = Integer.parseInt(dir[1].trim());
            String ip = dir[2].trim();

            Dv.put(letter, new Costo(ip, costo, letter));

            vecinos.add(letter);
            timeRemaining.put(letter, timerU);
            hostsWriter.write(letter + "-" + ip + "\n");
        }
        br.close();
        hostsWriter.close();

        // *********** Leemos nuestra dirección ***********//
        myHost = new Hosts();

        // *********** Creamos la primera instancia del archivo ***********//
        writeToFile(Dv);

        DatagramSocket ds = new DatagramSocket();

        // *********** Mandamos la tabla ***********//
        /*
         * Set<String> keys = Dv.keySet();
         * for (String key : keys) {
         * sendHello(key, myHost);
         * send(Messages.makeDvSend(myHost.getMyAddress(), Dv),key);
         * }
         */
        Set<String> keys = Dv.keySet();
        for (String key : keys) {
            Routing routingTable = new Routing();
            routingTable.socket = null;
            routingTable.action = false;
            routingTable.key = key;
            routingTable.start();
            routingTable.timer();
        }

        //System.out.println("DESPUES DE LO DE INICIALIZACION");

        // *********** Crea sockets para recibir mensajes ***********//
        ServerSocket serverSocket = new ServerSocket(PORT);
        // DatagramSocket dr = new DatagramSocket(PORT);
        // byte[] receive = new byte[65535];
        //MyTimer.init(timerT, timerU);

        // *********** Esperamos las llamadas ***********//
        DatagramPacket DpReceive = null;
        boolean hasChange = false;

        while (true) {
            //System.out.println("Esta escuchando");
            Socket socket = serverSocket.accept();
            Routing routing = new Routing();
            routing.socket = socket;
            routing.action = true;
            routing.key = "";
            //System.out.println("Entro, se creo Thread");
            // routing.run(socket, true);
            routing.start();
        }
    }

    Socket socket;
    boolean action;
    boolean repeat = false;
    String key;
    DatagramPacket DpReceive = null;
    boolean hasChange = false;
    DataOutputStream dataOutputStream = null;
    DataInputStream dataInputStream = null;

    public void timer () {
        Timer timer = new Timer();
         
        // Helper class extends TimerTask
        TimerTask task = new Helper(timerT, timerU, key, this);
         
        timer.schedule(task, 1000, 1000);
    }

    public void run() {

        if (!action) {
            String ip = getIpFromLetter(key);
            try (Socket socket = new Socket(ip, PORT)) {

                // *********** Mandamos hello y la tabla ***********//
                int count = 0;
                while (true) {
                    try {
                        dataInputStream = new DataInputStream(socket.getInputStream());
                        dataOutputStream = new DataOutputStream(socket.getOutputStream());
                        sendHello(key, myHost);
                        send(Messages.makeDvSend(myHost.getMyAddress(), Dv), key);
                        if (!repeat) break;
                        if (count > 100) break;
                        count++;
                    } catch (Exception e) {
                    }
                }

                // *********** Nos quedamos escuchando ***********//
                while (true) {
                    String request = "";
                    try {
                        dataInputStream = new DataInputStream(socket.getInputStream());
                        dataOutputStream = new DataOutputStream(socket.getOutputStream());
                        request = dataInputStream.readUTF();
                        if (request == null || request.equals("")) {
                            continue;
                        }
                    } catch (IOException e1) {
                        // continue;
                        ConsoleLog.printError();
                        e1.printStackTrace();
                    }

                    String data = request;

                    if (data.toLowerCase().contains("hello")) {
                        sendWelcome(data, myHost);
                        // LOG
                        String letter = data.split("\n")[0].split(":")[1].trim();
                        ConsoleLog.printRoutingMessage(letter, ConsoleLog.HELLO, ConsoleLog.RECEIVED);
                        Log.makeLogRouting(letter, ConsoleLog.HELLO, ConsoleLog.RECEIVED);

                    } else if (data.toLowerCase().contains("keepalive") || data.toLowerCase().contains("welcome")) {
                        from = data.split("\n")[0].split(":")[1];
                        if (data.toLowerCase().contains("keepalive")) {
                            // LOG
                            ConsoleLog.printRoutingMessage(from, ConsoleLog.KEEPALIVE, ConsoleLog.RECEIVED);
                            Log.makeLogRouting(from, ConsoleLog.KEEPALIVE, ConsoleLog.RECEIVED);
                        } else {
                            // LOG
                            ConsoleLog.printRoutingMessage(from, ConsoleLog.WELCOME, ConsoleLog.RECEIVED);
                            Log.makeLogRouting(from, ConsoleLog.WELCOME, ConsoleLog.RECEIVED);
                        }
                        resetTimeExceeded(from);
                    } else {
                        decode(data);

                        // LOG
                        String letter = data.split("\n")[0].split(":")[1].trim();
                        ConsoleLog.printRoutingMessage(letter, ConsoleLog.DISTANCEVECTOR, ConsoleLog.RECEIVED);
                        Log.makeLogRouting(letter, ConsoleLog.DISTANCEVECTOR, ConsoleLog.RECEIVED);

                        resetTimeExceeded(from);
                    }

                    // *********** Hacemos el algoritmo ***********//
                    if (DvReceived == null)
                        continue;
                    Set<String> receiveKeys = DvReceived.keySet();
                    for (String key : receiveKeys) {
                        if (Dv.get(key) == null) {
                            Dv.put(key,
                                    new Costo(Dv.get(from).ip, Dv.get(from).costo + DvReceived.get(key).costo, from));
                            changed.put(key,
                                    new Costo(Dv.get(from).ip, Dv.get(from).costo + DvReceived.get(key).costo, from));
                            try {
                                hostsWriter = new FileWriter("./hosts.txt", true);
                                hostsWriter.write(key + "-" + Dv.get(from).ip + "\n");
                                hostsWriter.close();
                            } catch (IOException e) {
                                ConsoleLog.printError();
                            }
                            hasChange = true;
                            continue;
                        }
                        if (DvReceived.get(key).costo + Dv.get(from).costo < Dv.get(key).costo) {
                            hasChange = true;
                            Dv.replace(key, new Costo(DvReceived.get(key).ip,
                                    DvReceived.get(key).costo + Dv.get(from).costo, DvReceived.get(key).link));
                            changed.put(key, new Costo(DvReceived.get(key).ip,
                                    DvReceived.get(key).costo + Dv.get(from).costo, DvReceived.get(key).link));
                        }
                    }

                    // *********** Escribe en el archivo el nuevo vector ***********//
                    if (hasChange) {
                        writeToFile(Dv);
                        hasChange = false;
                    }
                    break;
                }
            } catch (UnknownHostException ex) {
                repeat = true;
                //System.out.println("Server not found: " + ex.getMessage());
            } catch (IOException ex) {
                repeat = true;
                //System.out.println("I/O error: " + ex.getMessage());
            }
        }

        if (action) {
            while (true) {
                String request = "";
                try {
                    dataInputStream = new DataInputStream(socket.getInputStream());
                    dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    request = dataInputStream.readUTF();
                    if (request == null || request.equals("")) {
                        continue;
                    }
                } catch (IOException e1) {
                    // continue;
                    ConsoleLog.printError();
                    e1.printStackTrace();
                }

                String data = request;

                if (data.toLowerCase().contains("hello")) {
                    sendWelcome(data, myHost);
                    // LOG
                    String letter = data.split("\n")[0].split(":")[1].trim();
                    ConsoleLog.printRoutingMessage(letter, ConsoleLog.HELLO, ConsoleLog.RECEIVED);
                    Log.makeLogRouting(letter, ConsoleLog.HELLO, ConsoleLog.RECEIVED);

                } else if (data.toLowerCase().contains("keepalive") || data.toLowerCase().contains("welcome")) {
                    from = data.split("\n")[0].split(":")[1];
                    if (data.toLowerCase().contains("keepalive")) {
                        // LOG
                        ConsoleLog.printRoutingMessage(from, ConsoleLog.KEEPALIVE, ConsoleLog.RECEIVED);
                        Log.makeLogRouting(from, ConsoleLog.KEEPALIVE, ConsoleLog.RECEIVED);
                    } else {
                        // LOG
                        ConsoleLog.printRoutingMessage(from, ConsoleLog.WELCOME, ConsoleLog.RECEIVED);
                        Log.makeLogRouting(from, ConsoleLog.WELCOME, ConsoleLog.RECEIVED);
                    }
                    resetTimeExceeded(from);
                } else {
                    decode(data);

                    // LOG
                    String letter = data.split("\n")[0].split(":")[1].trim();
                    ConsoleLog.printRoutingMessage(letter, ConsoleLog.DISTANCEVECTOR, ConsoleLog.RECEIVED);
                    Log.makeLogRouting(letter, ConsoleLog.DISTANCEVECTOR, ConsoleLog.RECEIVED);

                    resetTimeExceeded(from);
                }

                // *********** Hacemos el algoritmo ***********//
                if (DvReceived == null)
                    continue;
                Set<String> receiveKeys = DvReceived.keySet();
                for (String key : receiveKeys) {
                    if (Dv.get(key) == null) {
                        Dv.put(key,
                                new Costo(Dv.get(from).ip, Dv.get(from).costo + DvReceived.get(key).costo, from));
                        changed.put(key,
                                new Costo(Dv.get(from).ip, Dv.get(from).costo + DvReceived.get(key).costo, from));
                        try {
                            hostsWriter = new FileWriter("./hosts.txt", true);
                            hostsWriter.write(key + "-" + Dv.get(from).ip + "\n");
                            hostsWriter.close();
                        } catch (IOException e) {
                            ConsoleLog.printError();
                        }
                        hasChange = true;
                        continue;
                    }
                    if (DvReceived.get(key).costo + Dv.get(from).costo < Dv.get(key).costo) {
                        hasChange = true;
                        Dv.replace(key, new Costo(DvReceived.get(key).ip,
                                DvReceived.get(key).costo + Dv.get(from).costo, DvReceived.get(key).link));
                        changed.put(key, new Costo(DvReceived.get(key).ip,
                                DvReceived.get(key).costo + Dv.get(from).costo, DvReceived.get(key).link));
                    }
                }

                // *********** Escribe en el archivo el nuevo vector ***********//
                if (hasChange) {
                    writeToFile(Dv);
                    hasChange = false;
                }
                break;
            }
        }

    }

    public static String getIpFromLetter(String hostletter) {
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
        } catch (Exception e) {
        }
        return null;
    }

    public static void resetTimeExceeded(String vecino) {
        timeRemaining.replace(vecino, timerU);
    }

    public void sendTimeoutMessage() throws SocketException {
        Hosts myHost = new Hosts();
        if (changed.size() == 0) {

            // LOG
            ConsoleLog.printRoutingMessage(key, ConsoleLog.KEEPALIVE, ConsoleLog.SENT);
            Log.makeLogRouting(key, ConsoleLog.KEEPALIVE, ConsoleLog.SENT);

            send(Messages.makeKeepAlive(myHost.getMyAddress()), null);
        } else {
            // LOG
            ConsoleLog.printRoutingMessage(key, ConsoleLog.DISTANCEVECTOR, ConsoleLog.SENT);
            Log.makeLogRouting(key, ConsoleLog.DISTANCEVECTOR, ConsoleLog.SENT);

            send(Messages.makeDvSend(myHost.getMyAddress(), changed), null);
        }
        changed = new HashMap<>();
    }

    public static void writeToFile(HashMap<String, Costo> Dv) {
        String outputFilePath = "./RoutingTable.txt";
        File file = new File(outputFilePath);

        if (file.exists()) {
            file.delete();
        }
        try {
            FileWriter writer = new FileWriter(outputFilePath, true);
            writer.write(myHost.getMyAddress() + ";" + 0 + ";" + myHost.getMyAddress() + "\n");
            Set<String> keys = Dv.keySet();
            for (String key : keys) {
                if (!key.equals(myHost.getMyAddress()))
                    writer.write(key + ";" + Dv.get(key).costo + ";" + Dv.get(key).link + "\n");
            }
            writer.close();
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    public static void decode(String data) {
        // System.out.println("---------" + data + "---------");
        String[] dataDecoded = data.split("\n");
        // System.out.println(dataDecoded[0]);
        from = dataDecoded[0].split(":")[1].trim();
        int size = Integer.parseInt(dataDecoded[2].split(":")[1].trim());
        DvReceived = new HashMap<>();

        int offset = 3;
        for (int i = 0; i < size; i++) {
            String[] entry = dataDecoded[i + offset].split(":");
            String hostLetter = entry[0].trim();
            DvReceived.put(hostLetter, new Costo(getIpFromLetter(hostLetter), Integer.parseInt(entry[1].trim()), null));
        }
    }

    public static StringBuilder data(byte[] a) {
        if (a == null)
            return null;
        StringBuilder ret = new StringBuilder();

        for (int i = 0; i < a.length; i++) {
            if (a[i] != 0) {
                ret.append((char) a[i]);
            }
        }
        return ret;
    }

    void send (String dv, String letter) {
        try {
            dataOutputStream.writeUTF(dv);
            dataOutputStream.flush();

            // LOG
            ConsoleLog.printRoutingMessage(key, ConsoleLog.DISTANCEVECTOR, ConsoleLog.SENT);
            Log.makeLogRouting(key, ConsoleLog.DISTANCEVECTOR, ConsoleLog.SENT);

        } catch (Exception e) {
            repeat = true;
            ////e.printStackTrace();
            //System.out.println("No se logro enviar");
        }
    }

    void sendHello(String Ip, Hosts host) {
        String message = Messages.makeInitialMessage(host.getMyAddress());

        // LOG
        ConsoleLog.printRoutingMessage(key, ConsoleLog.HELLO, ConsoleLog.SENT);
        Log.makeLogRouting(key, ConsoleLog.HELLO, ConsoleLog.SENT);

        send(message, Ip);
    }

    void sendWelcome(String data, Hosts host) {
        String letter = data.split("\n")[0].split(":")[1].trim();
        String message = Messages.ResponseInitialMessage(host.getMyAddress());

        // LOG
        ConsoleLog.printRoutingMessage(key, ConsoleLog.WELCOME, ConsoleLog.SENT);
        Log.makeLogRouting(key, ConsoleLog.WELCOME, ConsoleLog.SENT);

        send(message, letter);
        send(Messages.makeDvSend(myHost.getMyAddress(), Dv), letter);
    }

}