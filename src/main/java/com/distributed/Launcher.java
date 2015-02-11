package main.java.com.distributed;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import main.java.com.distributed.network.Conn;
import main.java.com.distributed.network.Packet;
import main.java.com.distributed.network.PacketHelper;

/**
 *
 * @author Richard Coan
 */
public class Launcher {
    private static Process counterServer;
    private static int counterServerPort = 1212;
    private static Map<Integer, Conn> network = new HashMap<Integer, Conn>();
    private static Map<Integer, Process> processes = new HashMap();
    
    /**
     * @return the file path to JAR location.
     */
    private static String getPath()
    {
        String path = null;
        
        try {
            path = Launcher.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            path = URLDecoder.decode(path, "UTF-8");
            
            if(path.charAt(0) == '/')
                path = new StringBuilder(path).deleteCharAt(0).toString();
            
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, null, ex);
            path = null;
        }
        
        return path;
    }
    
    /**
     * Starts the Counter / Time Server.
     * @return success
     */
    private static boolean StartCounterServer()
    {
        boolean success = true;

        try {
            System.out.print("Starting Counter Server");
            String path = getPath();
            if(path == null) counterServer = Runtime.getRuntime().exec("java -cp "+path+" main.java.com.distributed " + counterServerPort);
            success = counterServer.isAlive();
            System.out.println("..."+((success)?"success":"failed"));
        } catch (IOException ex) {
            Logger.getLogger(Launcher.class.getName()).severe(ex.getMessage());
            success = false;
        } 
        
        return success;
    }
    
    private static void LoadConfiguration(String resource_path_nodes)
    {
        System.out.println("Loading Nodes Configuration...from: "+resource_path_nodes);
        
        ClassLoader classLoader = Launcher.class.getClassLoader();
        InputStream is = classLoader.getResourceAsStream(resource_path_nodes);
               
        JsonReader rdr = Json.createReader(is);       
        JsonObject obj = rdr.readObject();
        JsonArray results = obj.getJsonArray("nodes");
        
        Map<Integer, Integer[]> adjacent = new HashMap<Integer, Integer[]>();
        
        for (JsonObject result : results.getValuesAs(JsonObject.class)) {
            Conn c = new Conn();
            c.host = result.getString("host");
            c.id = result.getInt("id");
            c.port = result.getInt("port");
            
            JsonArray nbs = result.getJsonArray("adj");
            
            Integer[] a = new Integer[nbs.size()];
            for(int i = 0; i < nbs.size(); i++)
            {
                a[i] = nbs.getInt(i);
            }
            
            adjacent.put(c.id, a);            
            network.put(c.id, c);
        }
        
        /*Add Children*/
        for (Conn c : network.values())
        {
            Integer[] ports = adjacent.get(c.id);
            
            for(int i = 0 ; i < ports.length; i++)
            {
                Conn.Child d = new Conn.Child();
                d.host = network.get( ports[i] ).host;
                d.port = network.get( ports[i] ).port;
                c.adj.put(ports[i],d);
            }
        }
    }
    
    private static void StartNodes()
    {        
        for(Map.Entry<Integer, Conn> entry : network.entrySet())
        {
            Conn c = entry.getValue();
            try {
                StringBuilder sb = new StringBuilder();
                sb.append(c.id+":"+c.host+":"+c.port+" ");
                
                for(Map.Entry<Integer, Conn.Child> d : c.adj.entrySet())
                {
                    Conn.Child child = d.getValue();
                    sb.append(d.getKey()+":"+child.host+":"+child.port+" ");
                }
                
                Process node = Runtime.getRuntime().exec("java -cp "+getPath()+" main.java.com.distributed.Node "+sb.toString() );
                processes.put(c.id, node);
            } catch (IOException ex) {
                Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, null, ex);
            } 
        }
        
        /* Pause to allow slower Processes time to start. */
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //Send the Start Command
        for(Map.Entry<Integer, Conn> entry : network.entrySet())
        {
            Conn c = entry.getValue();
            Socket socket;
            
            
            try {
                socket = new Socket(c.host, c.port);
                ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream()); 
                Packet p = PacketHelper.getPacket(PacketHelper.SET_START, -1);
                os.writeObject(p);
                os.flush();
                os.close();
                socket.close();
            } catch (IOException ex) {
                Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, c.host+":"+c.port, ex);
            }
        } 
    }
    
    private static void ShutdownNodes()
    {
        
    }
    
    private static void WaitForTermination()
    {
        while(true)
        {
            //???
        }
    }
    
    private static void PrintResults()
    {
        
    }
    
    public static void main(String[] args)
    {
        /* Handle Arguments First */
        String resource_path_nodes = "main/resources/nodes.json";    //Default
        
        if(args.length > 0 && args.length < 3)
        {
            counterServerPort = Integer.parseInt(args[1]);
            resource_path_nodes = args[0];
        } else if(args.length > 0 && args.length < 2)  {
            resource_path_nodes = args[0];
        } 
        
        LoadConfiguration(resource_path_nodes);
        StartNodes();
        WaitForTermination();
        PrintResults();
    }
}