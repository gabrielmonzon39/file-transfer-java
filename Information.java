import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Information {
    ConcurrentLinkedQueue<FileData> files;
    private static final String PATH = "./Received/Information.txt";

    public Information () {
        files = new ConcurrentLinkedQueue<>();
        try {  
            FileInputStream fis = new FileInputStream(PATH);       
            Scanner scanLine = new Scanner(fis);
            String[] fileData;
            while (scanLine.hasNextLine()) {  
                fileData = scanLine.nextLine().split(" ");
                files.add(new FileData(fileData[0], Integer.parseInt(fileData[1])));
            }  
            scanLine.close(); 
        } catch(IOException e) {  
            e.printStackTrace();  
        }  
    }

    public boolean hasRemainingFiles () {
        return !files.isEmpty();
    }

    public FileData getFile () {
        FileData fileData;
        do {
            fileData = files.poll();
        } while (isAlready(fileData));
        return fileData;
    }

    public boolean isAlready(FileData fileData) {
        File file = new File(fileData.fileName);
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