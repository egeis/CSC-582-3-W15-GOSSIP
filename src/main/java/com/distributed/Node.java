package main.java.com.distributed;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import main.java.com.distributed.network.Conn;
import main.java.com.distributed.network.Packet;
import main.java.com.distributed.network.PacketHelper;

/**
 *
 * @author Richard Coan & Matt Chung
 */
public class Node {
    private static ConcurrentHashMap<String, Node.Values> data = new ConcurrentHashMap(16, 0.9f, 1);
    private static ConcurrentHashMap<String, Node.Values> updateQueue = new ConcurrentHashMap(16, 0.9f, 1);
    
    private static Logger LOGGER;        
    
    private static ServerSocket server;
    private static ObjectInputStream input;
    private static ObjectOutputStream output;
    private static boolean shutdown = false;
    
    private static Integer id = null;
    private static int port = -1;
    private static String host = "";
    private static Boolean isInit = false;
    
    private static final Map<Integer,Conn.Child> adjacent = new HashMap();
    
    public static String TIMER_HOST = "localhost";
    public static int TIMER_PORT = 1212;
    
    private static int k = 0;
    private static int Mn = 0;
    private static int N = 0;
    
    /**
     * Requests the next counter from a global counter server.
     * @return a unique number [LONG].
     */
    private long RequestUpdateTime()
    {
        long time = 0;
        try ( 
            Socket socket = new Socket(TIMER_HOST, TIMER_PORT)) {
            output = new ObjectOutputStream(socket.getOutputStream());
            output.writeInt(1);
            output.flush();
            
            input = new ObjectInputStream(socket.getInputStream());
            time = input.readInt();
            socket.close();
        } catch (IOException ex) {
            Logger.getLogger(Node.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return time;
    }

    private static void parsePacket(Packet p)
    {                
        LOGGER.info("Recieved from node_"+p.id+" Packet:"+p.toString());
        
        switch(p.type) 
        {
            case PacketHelper.MESSAGE:
                
                //DO STUFF
                
                break;
            case PacketHelper.SET_START:
                
                start();
                
                break;
            case PacketHelper.INIT_SHUTDOWN:
                shutdown = true;
                break;
            default:
        }
    }
    
    private static void start()
    {
        Thread t = new Thread(new Updater());
        t.start();
    }
    
    private static Packet acceptMessage()
    {
        try {
            server = new ServerSocket(port);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        
        Packet p = null;
            
        try (Socket socket = server.accept()) {
            input = new ObjectInputStream(socket.getInputStream());
            p = (Packet) input.readObject();
            input.close();
            socket.close();
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(Node.class.getName()).log(Level.WARNING, null, ex);
        }   
        
        try {
            server.close();
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, null, ex);
        }
        
        return p;
    }

    /**
     * Sends a packet into the great unknown where it will meet an unknown fate
     * in this cruel digital world.
     * @param value 
     */
    private static void sendPacket(Values value)
    {        
        Packet p = PacketHelper.getPacket(PacketHelper.MESSAGE, id, value);
        
//        try 
//        {
//            
//        } catch (IOException ex) {
//           LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
//        }
    }
    
    public static long getTime()
    {
        DataOutputStream output;
        DataInputStream input;
        long time = 0L;
        
        try
        {
            Socket socket = new Socket("localhost", 1212);
            output = new DataOutputStream(socket.getOutputStream());
            output.writeInt(1);
            output.flush();
            input = new DataInputStream(socket.getInputStream());
            time = input.readLong();
            socket.close();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        
        return time;
    }
    
    public static void addToUpdateQueue(String key, Values value)
    {
        Values v = updateQueue.get(key);
        
        if (v == null)
        {
            value.COUNT = 0;
            updateQueue.put(key, value);
        }
        
        else
        {
            v.COUNT = 0;
            v.TIME = value.TIME;
            v.VALUE = value.VALUE;
        }
    }
    
    public static void main(String[] args)
    {
        port = 0;
        id = 0;
        String host = "";
        
        if(args.length > 1 )
        {
            k = Integer.parseInt(args[0]);
            Mn = Integer.parseInt(args[1]);
            N = Integer.parseInt(args[2]);
            
            /* Setup Port and ID. */
            String parts[] = args[3].split(":");
            id = Integer.parseInt(parts[0]);
            host = parts[1];
            port = Integer.parseInt(parts[2]);
                                   
            /* Get Adjacent List */
            for(int i = 1; i < args.length; i++)
            {
                parts = args[i].split(":");
                                
                Conn.Child c = new Conn.Child();
                c.host = parts[1];
                c.port = Integer.parseInt(parts[2]);
                adjacent.put(Integer.parseInt(args[0]), c); //Adjacent Nodes...
            }
        }
        else 
        {
//            Values v1 = new Values(1L, 10);
//            Values v2 = new Values(2L, 20);
//            Values v3 = new Values(3L, 30);
//            
//            data.put("A", v1);
//            data.put("B", v2);
//            data.put("C", v3);
//           
//            Values v = data.get("A");
//            System.out.println(v.VALUE);
//            
//            v.VALUE = 5000;
//            
//            Values va = data.get("A");
//            System.out.println(va.VALUE);
            System.exit(1);     //Something Went Wrong...
        }    
           
        LOGGER = Logger.getLogger(Node.class.getName()+"_"+id+"_"+host+"-"+port);
        
        /* LOG FILES MUST BE UNIQUE PER NODE INSTANCE */
        FileHandler fh;
        try {
            fh = new FileHandler(System.getProperty("user.dir")+"/logs/"+Node.class.getName()+"_"+id+"_"+host+"-"+port+".log");
            LOGGER.addHandler(fh);
            SimpleFormatter frmt = new SimpleFormatter();
            fh.setFormatter(frmt);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        } catch (SecurityException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
            
        while(!shutdown) {
            parsePacket(acceptMessage());
        }

        
        System.exit(0);
    }
    
    public static class Values {
        public long TIME;
        public Integer VALUE;
        public Integer COUNT;
        
        public Values(long time, Integer value)
        {
            this.TIME = time;
            this.VALUE = value;
            COUNT = 0;
        }

        @Override
        public String toString() {
            return "Values{" + "TIME=" + TIME + ", VALUE=" + VALUE + ", COUNT=" + COUNT + "}";
        }        
    }
    
    public static class Sender implements Runnable
    {
        public void run()
        {
            try 
            {
                while(true)
                {
                    Thread.sleep(3000);

                    Set s = updateQueue.keySet();
                    Object[] keys = s.toArray();
                    
                    if(keys.length == 0)
                        break;
                    
                    for (int i = 0; i < keys.length; i++)
                    {
                        Values v = updateQueue.get(keys[i]);
                        sendPacket(v);
                        v.COUNT++;
                        
                        if (v.COUNT == k)
                        {
                            updateQueue.remove(keys[i]);
                        }
                    }
                }
            } catch (InterruptedException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }            
        }
    }
    
    public static class Updater implements Runnable
    {
        public void run()
        {
            for (int i = 0; i < Mn; i++)
            {
                Timer timer = new Timer();
                timer.schedule(new UpdateTask(), N * 1000);
            }
            
            System.exit(0);
        }
    }
    
    public static class UpdateTask extends TimerTask
    {
        public void run()
        {
            Set s = data.keySet();
            Object[] keys = s.toArray();
            
            int randomIndex = (int)(Math.random() * (keys.length - 1));
            
            Values v = data.get(keys[randomIndex]);
            v.TIME = getTime();
            v.VALUE = (int)(Math.random() * 1000);
            
            data.replace(keys[randomIndex].toString(), v);
            
            addToUpdateQueue(keys[randomIndex].toString(), v);
            
            System.exit(0);
        }
    }
}
