package main.java.com.distributed;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
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
    private static ServerSocket server;
    private static int seconds;
    private static int k;
    
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
            counterServer = Runtime.getRuntime().exec("java -cp "+getPath()+" main.java.com.distributed.CounterServer " + counterServerPort);            
            success = counterServer.isAlive();
            System.out.println("..."+((success)?"success":"failed"));
        } catch (IOException ex) {
            Logger.getLogger(Launcher.class.getName()).severe(ex.getMessage());
            success = false;
        } 
        
        return success;
    }
    
    /**
     * Loads the configuration from the provided file.
     * @param resource_path_nodes 
     */
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
        
        System.out.println(network);
    }
    
    /**
     * Starts all the nodes from the provided resource file.
     */
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
                
                int m = 5;
                                
                Process node = Runtime.getRuntime().exec("java -Xmx64m -Xms64m -cp "+getPath()+" main.java.com.distributed.Node "+k+" "+m+" "+seconds+" "+sb.toString() );
                
                processes.put(c.id, node);
            } catch (IOException ex) {
                Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, null, ex);
                processes.remove(c.id);
            } 
        }
        
        /* Pause to allow slower Processes time to start. */
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        /*Send the Start Command*/
        for(Map.Entry<Integer, Conn> entry : network.entrySet())
        {
            Conn c = entry.getValue();
            Socket socket;
            
            try {
                System.out.println("Creating Node: "+c.host+":"+c.port);
                socket = new Socket(c.host, c.port);
                ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream()); 
                Packet p = PacketHelper.getPacket(PacketHelper.SET_START);
                
                System.out.println(p.toString());
                
                os.writeObject(p);
                os.flush();
                os.close();
                socket.close();
            } catch (IOException ex) {
                Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, c.host+":"+c.port, ex);
                System.out.println(processes.get(c.id).isAlive());
                System.out.println(processes.get(c.id).exitValue());
            }
        } 
    }
    
    /**
     * Shutsdown all the nodes on the process list.
     */
    private static void ShutdownNodes()
    {
        /*Send the Start Command*/
        for(Map.Entry<Integer, Conn> entry : network.entrySet())
        {
            Conn c = entry.getValue();
            Socket socket;
            
            try {
                socket = new Socket(c.host, c.port);
                ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream()); 
                //Packet p = PacketHelper.getPacket(PacketHelper.INIT_SHUTDOWN, -1, null);
                Packet p = PacketHelper.getPacket(PacketHelper.INIT_SHUTDOWN);
                os.writeObject(p);
                os.flush();
                os.close();
                socket.close();
            } catch (IOException ex) {
                Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, c.host+":"+c.port, ex);
            }
        }
    }
    
    private static void WaitForTermination()
    {
        int completed = 0;
        Packet p = null;
        
        while(completed < network.size())
        {
            try (Socket socket = server.accept()) {
                ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
                p = (Packet) input.readObject();
                input.close();
                socket.close();
            } catch (IOException ex) {
                Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            if(p != null)
            {
                if(p.type == PacketHelper.NODE_COMPLETE)
                {
                    System.out.println(p.toString());
                    
                    completed++;
                }
            }
        }
    }
    
    private static void PrintResults()
    {
        
    }
    
    public static void main(String[] args)
    {
        /* Set Defaults */
        String resource_path_nodes = "main/resources/nodes.json";
        seconds = 1;
        k = 10;
        counterServerPort = 1212;
        
        /* Handle Arguments First */
//        if(args.length > 0 && args.length < 5)
//        {
//            counterServerPort = Integer.parseInt(args[1]);
//            resource_path_nodes = args[0];
//            seconds = Integer.parseInt(args[2]);
//            k = Integer.parseInt(args[3]);
//            min = Integer.parseInt(args[4]);
//            max = Integer.parseInt(args[5]);
//        }
//        else if(args.length > 0 && args.length < 3)
//        {
//            counterServerPort = Integer.parseInt(args[1]);
//            resource_path_nodes = args[0];
//
//        }
//        else if(args.length > 0 && args.length < 2)
//        {
//            resource_path_nodes = args[0];
//        }
                
        try {
            server = new ServerSocket(1211);
        } catch (IOException ex) {
            Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, null, ex);
        }

        /* Starting the Counter (incrementer) Server */ 
//        StartCounterServer();
        
//        Process CounterServer;
//        try {
//            System.out.println("Starting Counter Server");
//            CounterServer = Runtime.getRuntime().exec("java -cp "+getPath()+" main.java.com.distributed " + counterServerPort);
//            boolean started = CounterServer.isAlive();
//            
//            if(started) {
//                System.out.println("Counter Server Started:"+started);
//            } else {
//                System.out.println("Counter Server Started:"+started);
//                System.out.println("Unable to contine...");
//                System.exit(1);
//            }
//            
//        } catch (IOException ex) {
//            Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, null, ex);        
//        } 
               
        LoadConfiguration(resource_path_nodes);
        
        StartNodes();
        WaitForTermination();
//        PrintResults();
        ShutdownNodes();
    }
}