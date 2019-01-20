import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Created by Yahya Hassanzadeh on 20/09/2017.
 */

public class ConnectionToServer
{
    public static final String DEFAULT_SERVER_ADDRESS = "localhost";
    public static final int DEFAULT_SERVER_PORT = 5000;
    protected static final String DROPSYNC_MAIN_FOLDER = "/Users/erdemarisoy/Desktop/DropSyncFollower";
    private Socket s;
    //private BufferedReader br;
    protected BufferedReader is;
    protected PrintWriter os;

    protected String serverAddress;
    protected int serverPort;
    private HashMap<String,String> hashMap = new HashMap<String, String>();
    private HashMap<String,String> previousSyncFromMaster = new HashMap<String, String>();
    private ArrayList<String> inconsistentFiles = new ArrayList<>();
    /**
     *
     * @param address IP address of the server, if you are running the server on the same computer as client, put the address as "localhost"
     * @param port port number of the server
     */
    public ConnectionToServer(String address, int port)
    {
        serverAddress = address;
        serverPort    = port;
        GetHashValueFromFolder();
    }


    /**
     * Establishes a socket connection to the server that is identified by the serverAddress and the serverPort
     */
    public void Connect()
    {
        try
        {
            s=new Socket(serverAddress, serverPort);
            //br= new BufferedReader(new InputStreamReader(System.in));
            /*
            Read and write buffers on the socket
             */
            is = new BufferedReader(new InputStreamReader(s.getInputStream()));
            os = new PrintWriter(s.getOutputStream());

            System.out.println("Successfully connected to " + serverAddress + " on port " + serverPort);
        }
        catch (IOException e)
        {
            //e.printStackTrace();
            System.err.println("Error: no server has been found on " + serverAddress + "/" + serverPort);
        }
    }

    /**
     * sends the message String to the server and retrives the answer
     * @param message input message string to the server
     * @return the received server answer
     */
    public String SendForAnswer(String message)
    {
        String response = new String();

        try
        {
            /*
            Sends the message to the server via PrintWriter
             */
            os.println(message);
            os.flush();
            /*
            Reads a line from the server via Buffer Reader
             */
            response = is.readLine();
        }
        catch(IOException e)
        {
            e.printStackTrace();
            System.out.println("ConnectionToServer. SendForAnswer. Socket read Error");
        }
        return response;
    }

    /**
     * Disconnects the socket and closes the buffers
     */
    public void SyncCheckRequest(){
        if(hashMap.size()==0){
            // TODO  to inconsistincy function
            return;
        }
        String response;
        os.println("sync check");
        os.flush();
        try{
            do {
                response = is.readLine();
                if(response.equals("sync check finished")){
                    break;
                }else if(!hashMap.containsKey(response)){

                    inconsistentFiles.add(response);
                }else {

                    hashMap.remove(response);
                }
            }while (true);

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("ConnectionToServer. SendForAnswer. Socket read Error");
        }

        if(inconsistentFiles.size()!=0){
            System.out.println("The following inconsistencies found:");
            System.out.println("\t-<filename>\t\t<status>\t<size>");
            GetInconsistentFilesInfo();
        }else {
            System.out.println("No update is needed. Your storage is already updated");
        }
    }
    public void Disconnect()
    {
        try
        {
            is.close();
            os.close();
            //br.close();
            s.close();
            System.out.println("ConnectionToServer. SendForAnswer. Connection Closed");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    private void GetHashValueFromFolder(){
        File folder = new File(DROPSYNC_MAIN_FOLDER);
        File[] listOfFiles = folder.listFiles();
        MessageDigest digest;
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile() && !(listOfFiles[i].getName().equals(".DS_Store")) ) {
                File file = new File(DROPSYNC_MAIN_FOLDER+"/"+listOfFiles[i].getName());
                byte[] mybytearray = new byte[(int) file.length()];
                byte[] dig;
                try{
                    digest  = MessageDigest.getInstance("MD5");
                    BigInteger bigInt = new BigInteger(1,digest.digest(mybytearray));
                    String hashValue = bigInt.toString();
                    hashMap.put(hashValue,file.getName());
                } catch (NoSuchAlgorithmException e) {
                    System.err.println("I'm sorry, but MD5 is not a valid message digest algorithm");
                }



            } else if (listOfFiles[i].isDirectory()) {
                System.out.println("Directory " + listOfFiles[i].getName());
            }
        }
    }


    private void GetInconsistentFilesInfo (){
        os.println("sync check inconsistency");
        os.flush();
        Iterator itr = inconsistentFiles.iterator();
        long totalSize=0;
        while(itr.hasNext()){
            os.println(itr.next());
            os.flush();
            totalSize += InconsistencyCheck();
        }
        os.println("sync check inconsistency finished");
        os.flush();
        if(hashMap.size()!=0){
            for (String key : hashMap.keySet()) {
                File file = new File(DROPSYNC_MAIN_FOLDER+"/"+hashMap.get(key));
                System.out.println("\t"+hashMap.get(key)+"\t\t"+"add(m)\t" + GetSizeFrom(file.length()));
                totalSize+=file.length();
            }
        }
        System.out.println("The total size of the updates is "+GetSizeFrom(totalSize));
    }
    private Long getSizeInByte (String size){
        Long result;
        int indexB = size.indexOf("B");
        result = Long.parseLong(size.substring(0,indexB-1));
        switch (size.substring(indexB-1,indexB)){
            case "K": result*=1024;
            case "M": result*=1024*1024;
            default:break;
        }
        return  result;
    }
    private String GetSizeFrom(long byteLegnth){
        long mByte;
        long kByte;

        if(byteLegnth>1024){
            kByte = byteLegnth/1024;
            if ( kByte>1024){
                mByte=kByte/1024;
                if(mByte>1024){
                    return (mByte/1024)+"GB";
                }
                return mByte+"MB";
            }
            return kByte+"KB";
        }
        return  byteLegnth+"B";
    }
    private long InconsistencyCheck(){
        long size=0;
        Set<String> keyspreviousSyncFromMaster= previousSyncFromMaster.keySet();
        String response;
        String[] fileInfo = new String[3];
        try{
            response=is.readLine();
            int beginIndex=1;
            int endIndex;
            for (int j = 0; j < 3; j++) {
                endIndex = response.indexOf(",", beginIndex);
                if(endIndex==-1){
                    endIndex = response.indexOf(">", beginIndex);
                }
                fileInfo[j] = response.substring(beginIndex, endIndex);
                beginIndex = endIndex + 2;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(hashMap.containsValue(fileInfo[0])){
            File file = new File(DROPSYNC_MAIN_FOLDER+"/"+fileInfo[0]);

            if (file.lastModified() > Long.parseLong(fileInfo[2])) {
                System.out.println("\t"+fileInfo[0] + "\t\t" + "update(m)\t" + fileInfo[1]);

            } else {
                System.out.println("\t"+fileInfo[0] + "\t\t" + "update(f)\t" + fileInfo[1]);
            }

            Iterator it = hashMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                //System.out.println(pair.getKey() + " = " + pair.getValue());
                if(pair.getValue().equals(fileInfo[0])) {
                    it.remove();           //for avoiding a ConcurrentModificationException from https://stackoverflow.com/questions/1861658/hashmap-with-stringkey-problem
                    break;
                }

            }
            size+=getSizeInByte(fileInfo[1]);
        }else if (previousSyncFromMaster.containsValue(fileInfo[0])){
            for(String key: keyspreviousSyncFromMaster){
                if(fileInfo[0].equals(previousSyncFromMaster.get(key))){
                    previousSyncFromMaster.remove(key);
                }
            }
            System.out.println("\t"+fileInfo[0]+"\t"+"delete(m)\t\t" + fileInfo[1]);
            os.println("delete");
            os.flush();
            size+=getSizeInByte(fileInfo[1]);
        } else{
            if(previousSyncFromMaster.size()!=0){
                for(String key: keyspreviousSyncFromMaster){
                    if(hashMap.containsValue(previousSyncFromMaster.get(key))){
                        System.out.println("\t"+fileInfo[0]+"\t\t"+"delete(f)\t" + fileInfo[1]);
                        size+=getSizeInByte(fileInfo[1]);
                    }
                }
            }else {
                System.out.println("\t"+fileInfo[0]+"\t\t"+"add(f)\t" + fileInfo[1]);
                size+=getSizeInByte(fileInfo[1]);
            }
        }
        os.flush();
        return size;
    }
    public void SyncFile(String fileName){
        os.println(fileName);
        os.flush();
        ConnectionToDataServer cnd = new ConnectionToDataServer(fileName);
    }
}
