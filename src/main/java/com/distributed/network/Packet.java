package main.java.com.distributed.network;

import java.io.Serializable;
import java.util.Map.Entry;

public class Packet implements Serializable {
    public int type = 0;
    public Integer id = null;
    public Entry entry = null;    
    
    @Override
    public String toString()
    {
        return "[Type:"+type+" Id:"+id+" Entry:["+entry.toString()+"]]";
    }    
}