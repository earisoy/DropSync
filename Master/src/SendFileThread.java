import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.ServerError;

public class SendFileThread extends Thread {
    protected Socket s = null;
    private int socketNum;
    private int numberOfSockets;
    private String fileName;
    protected OutputStream outStream = null;
    private ServerSocket[] dataConnectionSocket = new ServerSocket[4];

    FileInputStream fis = null;
    BufferedInputStream bis = null;
    OutputStream os = null;
    
    public SendFileThread(Socket s, int socketNum,int numOfSockets, String fileName) {
        this.s = s;
        this.socketNum = socketNum;
        this.fileName = fileName;
        this.numberOfSockets = numOfSockets;
    }
    public void run() {
        File file = new File (Server.DROPSYNC_MAIN_FOLDER+"/"+fileName);
        int offset = (int) file.length() % numberOfSockets;
        int fileSize;
        if(numberOfSockets==socketNum) {
            fileSize = (int)file.length()/numberOfSockets+offset;
        }else {
            fileSize = (int)file.length()/numberOfSockets;
        }

        byte [] fileByte  = new byte [fileSize];
        try {
            fis = new FileInputStream(file);
            bis = new BufferedInputStream(fis);
            if(numberOfSockets==socketNum){

                bis.read(fileByte, fileSize*(socketNum-1),fileSize*socketNum+offset-1);
            }else{
                bis.read(fileByte, fileSize*(socketNum-1),fileSize*socketNum-1);
            }
            os = s.getOutputStream();
            os.write(fileByte,0,fileByte.length);
            os.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
