/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.java.com.distributed;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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
 * @author Richard Coan
 */
public class Node {
    private static ConcurrentHashMap<String, Node.Values> data = new ConcurrentHashMap(16, 0.9f, 1);
    private static Logger LOGGER;        
    
    private ServerSocket server;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private boolean shutdown = false;
    
    public static Integer parent = null;
    private Integer id = null;
    private int port = -1;
    private String host = "";
    private Boolean isInit = false;
    
    private static final Map<Integer,Conn.Child> children = new HashMap();
    
    private static final Map<Integer,Boolean> incoming = new HashMap();
    private static final Map<Integer,Boolean> outgoing = new HashMap();
    
    public static String TIMER_HOST = "localhost";
    public static int TIMER_PORT = 1212;
            
    private Node(Integer id, String host, int port)
    {        
        this.id = id;
        this.host = host;
        this.port = port;
    }
    
    private long RequestNewUpdateTime()
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
    
    private void parsePacket(Packet p)
    {        
        Packet send;
        
        LOGGER.info("Recieved from node_"+p.id+" Packet:"+p.toString());
        
        switch(p.type) 
        {
            case PacketHelper.MESSAGE:
                
                //DO STUFF
                
                break;
            case PacketHelper.SET_START:
                
                //Start Generating Random Updates!
                
                break;
//            case PacketHelper.INIT_INITIATOR:
//                isInit = true;
//                break;
            case PacketHelper.INIT_SHUTDOWN:
                LOGGER.info(incoming.toString()+System.getProperty("line.separator")+outgoing.toString());
                shutdown = true;
                break;
            default:
        }
    }
    
    private void AcceptMessages()
    {
        try {
            server = new ServerSocket(port);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        
        while(true)
        {
            Packet p = null;
            
            try (Socket socket = server.accept()) {
                input = new ObjectInputStream(socket.getInputStream());
                p = (Packet) input.readObject();
                input.close();
                socket.close();
            } catch (IOException | ClassNotFoundException ex) {
                Logger.getLogger(Node.class.getName()).log(Level.WARNING, null, ex);
            } 
            
            if(p != null)
                this.parsePacket(p);
            
            if(shutdown) break;
        }      
        
        try {
            server.close();
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, null, ex);
        }
    }
    
    /**
     * Sends a packet of data to the initiator.
     * @param p the packet.
     * @param the type of send, 0 for parent, 1 for children (excluding parent)
     * 2 for all.
     */
    private void sendPacket(Packet p, int type)
    {        
        try {
            for(Map.Entry<Integer,Conn.Child> entry : children.entrySet()) 
            {
                Conn.Child c = entry.getValue();
                Integer key = entry.getKey();
                
                if(type == 0)           //Only Parent;
                {
                    if(!Objects.equals(parent, key)) {
                        //LOGGER.info("Skipping key:"+key);
                        continue;
                    }
                }
                else if (type == 1)     //Only Children;
                {
                    if(Objects.equals(key, parent)) {
                        //LOGGER.info("Skipping key:"+key);
                        continue;
                    }
                }
                
                
                LOGGER.info("Sending:"+"node_"+key+":"+c.host+":"+c.port+"---"+p.toString());
                
                outgoing.replace(key, true);
                
                Socket socket = new Socket(c.host, c.port);    
                output = new ObjectOutputStream(socket.getOutputStream()); 
                output.writeObject(p);
                output.flush();
                output.close();
                socket.close();
            }
        } catch (IOException ex) {
           LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
    
    public static void main(String[] args)
    {
        int port = 0;
        int id = 0;
        String host = "";
        
        if(args.length > 1 )
        {
            /* Setup Port and ID. */
            String parts[] = args[0].split(":");
            id = Integer.parseInt(parts[0]);
            host = parts[1];
            port = Integer.parseInt(parts[2]);
            LOGGER = Logger.getLogger(Node.class.getName()+"_"+id+"_"+host+"-"+port);

            FileHandler fh;
            try {
                fh = new FileHandler(System.getProperty("user.dir")+"/logs/"+Node.class.getName()+"_"+id+"_"+host+"-"+port+".log");
                LOGGER.addHandler(fh);
                SimpleFormatter frmt = new SimpleFormatter();
                fh.setFormatter(frmt);
            } catch (IOException ex) {
                Logger.getLogger(Node.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            } catch (SecurityException ex) {
                Logger.getLogger(Node.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            }
            
            LOGGER.info("Starting...");
            
            /* Get Adjacent List */
            for(int i = 1; i < args.length; i++)
            {
                parts = args[i].split(":");
                                
                Conn.Child c = new Conn.Child();
                c.host = parts[1];
                c.port = Integer.parseInt(parts[2]);
                
                children.put(Integer.parseInt(parts[0]), c);
                incoming.put(Integer.parseInt(parts[0]), false);
                outgoing.put(Integer.parseInt(parts[0]), false);
            }
            
            /* Create New Node */
            Node node = new Node(id, host, port);
            node.AcceptMessages();
        } 
        else 
        {
            System.exit(1);     //Something Went Wrong...
        }
        
        System.exit(0);
    }
    
    
    public static class Values {
        public long TIME;
        public Integer VALUE;
        
        public void Values(long time, Integer value)
        {
            this.TIME = time;
            this.VALUE = value;
        }

        @Override
        public String toString() {
            return "Values{" + "TIME=" + TIME + ", VALUE=" + VALUE + '}';
        }        
    }
}
