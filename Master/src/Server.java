// echo server


import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;


public class Server
{
    private ServerSocket serverSocket;
    private ArrayList<ServerSocket> dataSockets = new ArrayList<>();

    public static final int DEFAULT_SERVER_PORT = 5000;
    public static final int[] DATA_CONNECTION_PORT = {4445,4446,4447,4448};
    protected static final String DROPSYNC_MAIN_FOLDER = "/Users/erdemarisoy/Desktop/DropSync" ;

    /**
     * Initiates a server socket on the input port, listens to the line, on receiving an incoming
     * connection creates and starts a ServerThread on the client
     * @param port
     */
    public Server(int port)
    {
        try
        {
            serverSocket = new ServerSocket(port);
            System.out.println("Oppened up a server socket on " + Inet4Address.getLocalHost());
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.err.println("Server class.Constructor exception on oppening a server socket");
        }
        while (true)
        {
            ListenAndAccept();
        }
    }
    public Server(String fileName)
    {
        File file = new File(DROPSYNC_MAIN_FOLDER+"/"+fileName);
        long sizeInMB = file.length()/(1024*1024);
        int numOfPorts = (int) sizeInMB/100;
        if(sizeInMB%100!=0){
            numOfPorts++;
        }
        numOfPorts++;
        try
        {
            for (int i = 0; i <numOfPorts ; i++) {
                dataSockets.add(new ServerSocket(DATA_CONNECTION_PORT[i]));
            }

            System.out.println("Oppened up "+numOfPorts+" data socket on " + Inet4Address.getLocalHost());
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.err.println("Server class.Constructor exception on oppening a server socket");
        }
        while (true)
        {
            OpenDataConnection(fileName);
        }
    }


    /**
     * Listens to the line and starts a connection on receiving a request from the client
     * The connection is started and initiated as a ServerThread object
     */
    private void ListenAndAccept()
    {
        Socket s;

        try
        {
            s = serverSocket.accept();
            System.out.println("A connection was established with a client on the address of " + s.getRemoteSocketAddress());
            ServerThread st = new ServerThread(s);
            st.start();
        }

        catch (Exception e)
        {
            e.printStackTrace();
            System.err.println("Server Class.Connection establishment error inside listen and accept function");
        }

    }
    private void OpenDataConnection(String fileName) {
        Socket[] dS = new Socket[dataSockets.size()];
        for (int i = 0; i < dS.length ; i++) {
            try {

                dS[i] = dataSockets.get(i).accept();
                System.out.println("A connection was established with a client on the address of " + dS[i].getRemoteSocketAddress());
                SendFileThread m = new SendFileThread(dS[i],i+1,dS.length,fileName);
                m.start();
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Server Class.Connection establishment error inside open data connection");
            }
        }

    }

}

