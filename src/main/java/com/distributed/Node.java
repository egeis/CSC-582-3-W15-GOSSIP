package main.java.com.distributed;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import main.java.com.distributed.io.FileIO;
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
    
    private static Thread updateThread = null;
    private static Thread sendThread = null;
    
    private static boolean done = false;
    /**
     * Requests the next counter from a global counter server.
     * @return a unique number [LONG].
     */
    private static long getTime()
    {
//        long time = 0L;
        LOGGER.info("Getting a new time.");
                
//        try { 
//            Socket socket = new Socket(TIMER_HOST, TIMER_PORT);
//            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
//            out.writeInt(1);
//            out.flush();
//            out.close();
//            socket.close();
//            
//            socket = new Socket(TIMER_HOST, TIMER_PORT);
//            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
//            time = in.readInt();
//            in.close();
//            socket.close();
//        } catch (IOException ex) {
//            LOGGER.log(Level.SEVERE, null, ex);
//        }
        
        return System.currentTimeMillis();
    }

    private static void parsePacket(Packet p)
    {                
        switch(p.type) 
        {
            case PacketHelper.MESSAGE:
                Values v = data.get(p.key);
                
                if (v == null)
                {
                    data.put(p.key, p.value);
                    addToUpdateQueue(p.key, p.value);
                }
                
                else
                {
                    if (p.value.TIME > v.TIME)
                    {
                        v.VALUE = p.value.VALUE;
                        addToUpdateQueue(p.key, v);
                    }
                }
                break;
            case PacketHelper.SET_START:
                start();
                break;
            case PacketHelper.INIT_SHUTDOWN:
                shutdown = true;
                done = true;
                break;
            default:
        }
    }
    
    private static void start()
    {
        LOGGER.info("Starting Threads.");
        updateThread = new Thread(new Updater());
        updateThread.start();
        
        sendThread = new Thread(new Sender());
        sendThread.start();
    }
    
    private static Packet acceptMessage()
    {        
        Packet p = null;
            
        try (Socket socket = server.accept()) {
            input = new ObjectInputStream(socket.getInputStream());
            LOGGER.info("Accepting a Packet @"+System.currentTimeMillis());
            
            p = (Packet) input.readObject();
            
            LOGGER.info("Received from node_"+p.id+" Packet:"+p.toString());
            
            input.close();
            socket.close();
        } catch (IOException | ClassNotFoundException ex) {
            LOGGER.info("Something went wrong, im sorry...");
            LOGGER.log(Level.SEVERE, null, ex);
        }          
        
        return p;
    }

    private static void sendCompletedMessage()
    {
        Socket socket;
        
        LOGGER.info("Completed!");
        
        try {
            socket = new Socket("localhost", 1211);
            ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream()); 
            Packet p = PacketHelper.getPacket(PacketHelper.NODE_COMPLETE, id, "", null);
            
            os.writeObject(p);
            os.flush();
            os.close();
            socket.close();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "localhost"+":"+1211, ex);
        }
    }
    
    /**
     * Sends a packet into the great unknown where it will meet an unknown fate
     * in this cruel digital world.
     * @param value 
     */
    private static void sendPacket(String key, Values value)
    {        
        Packet p = PacketHelper.getPacket(PacketHelper.MESSAGE, id, key, value);
        
        Set s = adjacent.keySet();
        Object[] keys = s.toArray();
            
        int randomIndex = (int)(Math.random() * (keys.length - 1));
            
        Conn.Child ch = adjacent.get(keys[randomIndex]);
        Socket socket;
            
        try {
            socket = new Socket(ch.host, ch.port);
            ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream()); 
            
            os.writeObject(p);
            os.flush();
//            os.close();
            socket.close();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ch.host+":"+ch.port, ex);
        }
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
    
    public static void waitForThreads()
    {
        int count = 0;
        
        LOGGER.info("Waiting for threads...");
        
        while(count < 2)
        {
            if(!updateThread.isAlive())
                count++;
            
            if(!sendThread.isAlive())
                count++;
        }
        
        LOGGER.info("Both threads are done...");
    }
    
    public static void loadFile(String contents)
    {
        String[] records = contents.split(System.getProperty("line.separator"));

        for(int i = 0; i < records.length; i++)
        {
            String[] keyValue = records[i].split(",");
            Values v = new Values(0L, Integer.parseInt(keyValue[1]));
            data.put(keyValue[0], v);       
        }
    }
    
    public static void main(String[] args)
    {
        port = 0;
        id = 0;
        String host = "";
        
        String path = "main/resources/init_data.txt";
        String contents = FileIO.ReadFile(path);
        
        loadFile(contents);
        
        if(args.length > 1)
        {
            k = Integer.parseInt(args[0]);
            Mn = Integer.parseInt(args[1]);
            N = Integer.parseInt(args[2]);
            
            // Setup Port and ID. 
            String parts[] = args[3].split(":");
            id = Integer.parseInt(parts[0]);
            host = parts[1];
            port = Integer.parseInt(parts[2]);
                                   
            // Get Adjacent List 
            for(int i = 4; i < args.length; i++)
            {
                parts = args[i].split(":");
                                
                Conn.Child c = new Conn.Child();
                c.id = Integer.parseInt(parts[0]);
                c.host = parts[1];
                c.port = Integer.parseInt(parts[2]);
                adjacent.put(c.id, c); //Adjacent Nodes...
            }
        }
        else 
        {
            System.exit(1);     //Something Went Wrong...
        }    
        LOGGER = Logger.getLogger(Node.class.getName()+"_"+id+"_"+host+"-"+port);
        
        // LOG FILES MUST BE UNIQUE PER NODE INSTANCE 
        FileHandler fh;
        try {
            fh = new FileHandler(Node.class.getName()+"_"+id+"_"+host+"-"+port+".log");
            LOGGER.addHandler(fh);
            SimpleFormatter frmt = new SimpleFormatter();
            fh.setFormatter(frmt);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        } catch (SecurityException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
           
        LOGGER.info("Starting Server...");
        try {
            server = new ServerSocket(port);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        
        LOGGER.info("Accepting Packets...");
        while(!shutdown) {
            parsePacket(acceptMessage());
        }

        LOGGER.info("Shutting down updates...");
        try {
            updateThread.join();
            sendThread.join();
        } catch (InterruptedException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        
        LOGGER.info("Shutting down Server...");
        try {
            server.close();
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, null, ex);
        }
        
        System.exit(0);
    }
    
    public static class Values implements Serializable{
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
//            try 
//            {
                while(!done)
                {
//                    Thread.sleep(500);

                    Set s = updateQueue.keySet();
                    Object[] keys = s.toArray();
                    
                    for (int i = 0; i < keys.length; i++)
                    {
                        Values v = updateQueue.get(keys[i]);
                        v.COUNT++;
                        sendPacket(keys[i].toString(), v);
                        
                        if (v.COUNT == k)
                        {
                            updateQueue.remove(keys[i]);
                        }
                    }
                }
                
//                System.exit(0);
//            } catch (InterruptedException ex) {
//                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
//            }         
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
            
            sendCompletedMessage();
//            System.exit(0);
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
            
//            System.exit(0);
        }
    }
}
