import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class Hosts {
    ArrayList<String> hosts;
    private static final String PATH = "./hosts.txt";

    public Hosts () {
        hosts = new ArrayList<>();
        try {  
            FileInputStream fis = new FileInputStream(PATH);       
            Scanner scanLine = new Scanner(fis);
            while (scanLine.hasNextLine()) {  
                hosts.add(scanLine.nextLine().trim());
            }  
            scanLine.close(); 
        } catch(IOException e) {  
            //e.printStackTrace();  
        }  
    }

    public String getMyAddress () {
        return hosts.get(0).split("-")[0];
    }

    public String getRandomHost () {
        Random rand = new Random();
        int i = 0;
        while (i == 0) {
            i = rand.nextInt(hosts.size());
        }
        return hosts.get(i).split("-")[0];
    }
}
