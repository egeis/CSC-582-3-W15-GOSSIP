package main.java.com.distributed.network;

import java.util.Map.Entry;

/**
 * Creates the Packet Object to send across nodes.
 * @author Richard Coan
 */
public class PacketHelper {
    public static final int MESSAGE              = 1;
    
    public static final int INIT_SHUTDOWN        = 30;
    public static final int INIT_INITIATOR       = 31;
    
    public static final int SET_START            = 25;
    
    /**
     * Gets a Packet containing only a message type.
     * @param type of message.
     * @param id of node.
     * @param entry to be updated
     * @return 
     */
    public static Packet getPacket(int type, Integer id, Entry entry)
    {
        Packet p = new Packet();
        p.type = type;
        p.id = id;
        p.entry = entry;
        return p;
    }
    
    /**
     * Gets a Packet containing only a message type.
     * @param type of message.
     * @param id of node.
     * @return 
     */
    public static Packet getPacket(int type, Integer id)
    {
        Packet p = new Packet();
        p.type = type;
        p.id = id;
        return p;
    }
    
}