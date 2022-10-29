import java.time.format.DateTimeFormatter;  
import java.time.LocalDateTime;    
public class Time {

    public static String now () {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");  
        LocalDateTime now = LocalDateTime.now();  
        return dtf.format(now);
    }

}    