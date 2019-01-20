
import java.io.*;
import java.math.BigInteger;


import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.Hashtable;
import java.util.Set;

class ServerThread extends Thread
{
    protected BufferedReader is=null;
    protected PrintWriter os;
    protected Socket s=null;
    private String line = new String();

    //** for File transfer
    protected FileInputStream fis = null;
    protected BufferedInputStream bis = null;

    private Hashtable<String,String > fileNames = new Hashtable<String, String>();
    protected Socket sock = null;

    private static final int[] DATA_CONNECTION_PORT = {4445,4446,4447,4448};
    /**
     * Creates a server thread on the input socket
     *
     * @param s input socket to create a thread on
     */
    public ServerThread(Socket s)
    {
        this.s = s;
    }

    /**
     * The server thread, echos the client until it receives the QUIT string from the client
     */
    public void run()
    {
        ReadAllFilesInFolder();
       
        try
        {
            is = new BufferedReader(new InputStreamReader(s.getInputStream()));
            os = new PrintWriter(s.getOutputStream());

        }
        catch (IOException e)
        {
            System.err.println("Server Thread. Run. IO error in server thread");
        }

        try
        {
            line = is.readLine();
            while (line.compareTo("QUIT") != 0)
            {
                String fileName="";


                if(line.equals("sync check")){
                   //ReadAllFilesInFolder();
                    SynCheck();
                }else if(line.equals("sync check inconsistency")) {
                    System.out.println("Client " + s.getRemoteSocketAddress() + " sent : " + line);
                    SynCheckInconsistency();
                }else if(line.substring(0,4).equals("sync") && line.substring(5,6).equals("<")){
                    System.out.println("sync <fileName>");
                    Server server=new Server(line.substring(5,line.indexOf(">")));
                } else {
                    os.println(line);
                    os.flush();

                }
                System.out.println("Client " + s.getRemoteSocketAddress() + " sent : " + line);
                line = is.readLine();

            }


        }
        catch (IOException e)
        {
            line = this.getName(); //reused String line for getting thread name
            System.err.println("Server Thread. Run. IO Error/ Client " + line + " terminated abruptly");
        }
        catch (NullPointerException e)
        {
            line = this.getName(); //reused String line for getting thread name
            System.err.println("Server Thread. Run.Client " + line + " Closed");

        }finally
        {
            try
            {
                System.out.println("Closing the connection");
                if (is != null)
                {
                    is.close();
                    System.err.println(" Socket Input Stream Closed");
                }

                if (os != null)
                {
                    os.close();
                    System.err.println("Socket Out Closed");
                }
                if (s != null)
                {
                    s.close();
                    System.err.println("Socket Closed");
                }

            }
            catch (IOException ie)
            {
                System.err.println("Socket Close Error");
            }
        }//end finally
    }
    public void SynCheck(){
        Set<String> keys = fileNames.keySet();
        for(String key: keys){
            File file = new File(Server.DROPSYNC_MAIN_FOLDER+"/"+fileNames.get(key));
            os.println(key);
            os.flush();
        }
        os.println("sync check finished");
        os.flush();
    }



    private void ReadAllFilesInFolder(){
        File folder = new File(Server.DROPSYNC_MAIN_FOLDER);
        File[] listOfFiles = folder.listFiles();
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                File file = new File(Server.DROPSYNC_MAIN_FOLDER+"/"+listOfFiles[i].getName());
                byte[] bytes = new byte[(int) file.length()];
                byte[] dig;
                MessageDigest digest;
                try{
                    digest  = MessageDigest.getInstance("MD5");

                    BigInteger bigInt = new BigInteger(1,digest.digest(bytes)) ;
                    String hashtext = bigInt.toString();
                    System.out.println(file.getName()+" "+hashtext);
                    fileNames.put(hashtext,file.getName());

                } catch (NoSuchAlgorithmException e) {
                   System.err.println("MD5 is not a valid message digest algorithm");
                }
            } else if (listOfFiles[i].isDirectory()) {
                System.out.println("Directory " + listOfFiles[i].getName());
            }
        }

    }
    private String GetSizeFrom(long byteLegnth){
        long kByte;

        if(byteLegnth>1024){
            kByte = byteLegnth/1024;
            if ( kByte>1024){
                return (kByte/1024)+"MB";
            }
            return kByte+"KB";
        }
        return  byteLegnth+"B";
    }
    private void SynCheckInconsistency(){
        String response;
        File file;
        String hash;
        try
        {
            do {
                response = is.readLine();
                System.out.println(response);
                file = new File(Server.DROPSYNC_MAIN_FOLDER+"/"+fileNames.get(response));
                Long size=file.length();
                if(response.equals("sync check inconsistency finished")) {
                    break;
                }else if(response.equals("delete")){
                    //TODO delete the file which corresponds to the hash value
                }else{
                    System.out.println("<"+fileNames.get(response)+", "+ GetSizeFrom(size)+", "+file.lastModified()+">");
                    os.println("<"+fileNames.get(response)+", "+ GetSizeFrom(size)+", "+file.lastModified()+">");
                    os.flush();
                }
                hash=response;

            } while (true);

        } catch (IOException e) {
            e.printStackTrace();
            response= this.getName(); //reused String line for getting thread name
            System.err.println("Server Thread. Run. IO Error/ Client " + response + " terminated abruptly");
        }
    }

}