import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ConnectionToDataServer {
    protected String serverAddress;
    private ArrayList<Socket> sockets = new ArrayList<>();
    private static final int[] DATA_CONNECTION_PORT = {4445,4446,4447,4448};
    private String fileName ;
    private int bytesRead;
    private int current = 0;
    FileOutputStream fos = null;
    BufferedOutputStream bos = null;
    public final static int FILE_SIZE = 6022386;
    public ConnectionToDataServer(String fileName)
    {
        serverAddress = ConnectionToServer.DEFAULT_SERVER_ADDRESS;
        this.fileName = fileName;
    }
    public void Connect() {
        for (int i = 0; i < 4; i++) {
            try {


                sockets.add(i, new Socket(serverAddress, DATA_CONNECTION_PORT[i]));

                System.out.println("Successfully connected to " + serverAddress + " on port " + DATA_CONNECTION_PORT[i]);

                byte[] mybytearray = new byte[FILE_SIZE];
                InputStream is = sockets.get(i).getInputStream();
                fos = new FileOutputStream(ConnectionToServer.DROPSYNC_MAIN_FOLDER + "/" + fileName);
                bos = new BufferedOutputStream(fos);
                bytesRead = is.read(mybytearray, 0, mybytearray.length);
                current = bytesRead;
                do {
                    bytesRead =
                            is.read(mybytearray, current, (mybytearray.length - current));
                    if (bytesRead >= 0) current += bytesRead;
                } while (bytesRead > -1);
                bos.write(mybytearray, 0, current);
                bos.flush();
                System.out.println("File " + ConnectionToServer.DROPSYNC_MAIN_FOLDER + "/" + fileName
                        + " downloaded (" + current + " bytes read)");



            //is = new BufferedReader(new InputStreamReader(s.getInputStream()));
            //os = new PrintWriter(s.getOutputStream());


        }
        catch(IOException e)
        {
            //e.printStackTrace();
            System.err.println("Error: no server has been found on " + serverAddress + "/" + DATA_CONNECTION_PORT[i]);
        }
    }
    }
}
