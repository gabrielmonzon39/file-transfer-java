import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Information {
    ConcurrentLinkedQueue<FileData> files;
    private static final String INFOTXT = "./Received/Information.txt";
    private static final String PATH = "./Received/";

    public Information () {
        files = new ConcurrentLinkedQueue<>();
        try {  
            FileInputStream fis = new FileInputStream(INFOTXT);       
            Scanner scanLine = new Scanner(fis);
            String[] fileData;
            while (scanLine.hasNextLine()) {  
                fileData = scanLine.nextLine().split(" ");
                files.add(new FileData(fileData[0], Integer.parseInt(fileData[1])));
            }  
            scanLine.close(); 
        } catch(IOException e) {  
            //e.printStackTrace();  
        }  
    }

    public boolean hasRemainingFiles () {
        return !files.isEmpty();
    }

    public FileData getFile () {
        FileData fileData;
        while (hasRemainingFiles()) {
            fileData = files.poll();
            if (!isAlready(fileData)) {
                return fileData;
            }
        }
        return null;
    }

    public boolean isAlready(FileData fileData) {
        File file = new File(PATH+(fileData.fileName));
        return file.exists();
    }
}

class FileData {
    String fileName;
    int size;

    FileData (String fileName, int size) {
        this.fileName = fileName;
        this.size = size;
    }
}