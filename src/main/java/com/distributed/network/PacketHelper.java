package main.java.com.distributed.network;

import main.java.com.distributed.Node;

/**
 * Creates the Packet Object to send across nodes.
 * @author Richard Coan
 */
public class PacketHelper {
    public static final int MESSAGE              = 1;
    public static final int NODE_COMPLETE        = 2;
    
    public static final int INIT_SHUTDOWN        = 30;
    public static final int INIT_INITIATOR       = 31;
    
    public static final int SET_START            = 25;
    
    /**
     * Gets a Packet containing only a message type.
     * @param type of message.
     * @param value
     * @return 
     */
    public static Packet getPacket(int type, int id, Node.Values value)
    {
        Packet p = new Packet();
        p.type = type;
        p.value = value;
        return p;
    }
    
    /**
     * Gets a Packet containing only a message type.
     * @param type of message.
     * @param id of node.
     * @return 
     */
    public static Packet getPacket(int type)
    {
        Packet p = new Packet();
        p.type = type;
        return p;
    }
    
}