package main.java.com.distributed.network;

import java.io.Serializable;
import main.java.com.distributed.Node;

public class Packet implements Serializable {
    public int type = 0;
    public int id = 0;
    public String key = "";
    public Node.Values value = null;    
    
    @Override
    public String toString()
    {
        return "[Type:"+type+" Value:["+((value == null)?"":value.toString())+"]]";
    }    
}