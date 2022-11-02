import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
 
class Helper extends TimerTask {
    static final int infinity = 99;
    public static int i = 0;
    int T;
    int U;

    public Helper (int timerT, int timerU) {
        this.T = timerT;
        this.U = timerU;
    }
    
    // TimerTask.run() method will be used to perform the action of the task
    public void run() {
        i++;
        if (i >= T) {
            try {
                Routing.sendTimeoutMessage();
            } catch (SocketException e) {
                e.printStackTrace();
            }
            i = 0;
        }
        
        // Disminuir time de los vecinos
        Set<String> keys = Routing.timeRemaining.keySet();
        for (String key : keys) {
            //System.out.println("Cambiando");
            int remaining = Routing.timeRemaining.get(key) - 1;
            Routing.timeRemaining.replace(key, remaining);
        }

        //***********    Verifica si alguien no ha mandado su keep alive    ***********// 
        checkTimeExceeded();
    }

    public void checkTimeExceeded () {
        HashMap<String, Costo> changesToInfinity = new HashMap<>();
        Set<String> keys = Routing.timeRemaining.keySet();
        for (String key : keys) {
            //System.out.println(key + " ---> " + Routing.timeRemaining.get(key));
            if (Routing.timeRemaining.get(key) == 0) {
                changesToInfinity.put(key, new Costo(infinity, null));
            }
        }
        if (changesToInfinity.size() != 0) {
            try {
                sendTimeExceededMessage(changesToInfinity);   
            } catch (Exception e) {
                System.out.println("Ocurrió un error.");
            }
        }
    }

    public void sendTimeExceededMessage (HashMap<String, Costo> changesToInfinity) throws SocketException {
        DatagramSocket ds = new DatagramSocket();
        Hosts myHost = new Hosts();
        String vecino;
        for (int i = 0; i < Routing.vecinos.size(); i++) {
            vecino = Routing.vecinos.get(i); 
            Routing.send(ds, Messages.makeDvSend(myHost.getMyAddress(), changesToInfinity), vecino);
        }
    }

    
}

public class MyTimer {
    public static void init(int timerT, int timerU) {
 
        Timer timer = new Timer();
         
        // Helper class extends TimerTask
        TimerTask task = new Helper(timerT, timerU);
         
        timer.schedule(task, 1000, 1000);
 
    }
}