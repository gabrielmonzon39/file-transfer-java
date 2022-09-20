package Client;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
 
public class Client {
    @SuppressWarnings("resource")
    public static void main(String[] args) throws UnknownHostException, IOException {
        Socket sock = new Socket("localhost", 3456);
        String FileName = "prueba.txt";
        File MyFile = new File(FileName);
        int FileSize = (int) MyFile.length();
        OutputStream os =sock.getOutputStream();
        PrintWriter pr = new PrintWriter(sock.getOutputStream(), true);
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(MyFile));
        Scanner in = new Scanner(sock.getInputStream());
         
        pr.println(FileName);
        pr.println(FileSize);
        byte[] filebyte = new byte[FileSize];
        bis.read(filebyte, 0, filebyte.length);
        os.write(filebyte, 0, filebyte.length);
        System.out.println(in.nextLine());
        os.flush();
        sock.close();
    }
}