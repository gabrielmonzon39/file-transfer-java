import java.net.SocketException;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
 
class Helper extends TimerTask {
    static final int infinity = 99;
    public int i = 0;
    int T;
    int U;
    String key;
    Routing routing;

    public Helper (int timerT, int timerU, String key, Routing routing) {
        this.T = timerT;
        this.U = timerU;
        this.key = key;
        this.routing = routing;
    }
    
    // TimerTask.run() method will be used to perform the action of the task
    public void run() {
        i++;
        //System.out.println(i + " : " + T);
        if (i >= T) {
            try {
                routing.sendTimeoutMessage();
            } catch (SocketException e) {
                //e.printStackTrace();
            }
            i = 0;
        }
        
        // Disminuir time de los vecinos
        int remaining = Routing.timeRemaining.get(key) - 1;
        Routing.timeRemaining.replace(key, remaining);
        

        //***********    Verifica si el cuate no ha mandado su keep alive    ***********// 
        checkTimeExceeded();
    }

    public void checkTimeExceeded () {
        HashMap<String, Costo> changesToInfinity = new HashMap<>();

        if (Routing.timeRemaining.get(key) == 0) {
            changesToInfinity.put(key, new Costo(Routing.getIpFromLetter(key), infinity, null));
        }

        if (changesToInfinity.size() != 0) {
            try {
                sendTimeExceededMessage(changesToInfinity);   
            } catch (Exception e) {
                ConsoleLog.printError();
            }
        }
    }

    public void sendTimeExceededMessage (HashMap<String, Costo> changesToInfinity) throws SocketException {
        Hosts myHost = new Hosts();
        routing.send(Messages.makeDvSend(myHost.getMyAddress(), changesToInfinity), null);
    }

    
}

public class MyTimer {
    public static void init(int timerT, int timerU, String key) {
 
        Timer timer = new Timer();
         
        // Helper class extends TimerTask
        TimerTask task = new Helper(timerT, timerU, key, null);
         
        timer.schedule(task, 1000, 1000);
 
    }
}