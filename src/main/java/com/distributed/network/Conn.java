/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.java.com.distributed.network;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author Richard Coan
 */
public class Conn {
    public String host;
    public int port;
    public int id = 0;
    public Map<Integer, Conn.Child> adj = new HashMap();

    public static class Child {
        public String host;
        public int port;
        public int id;
        
        @Override
        public String toString()
        {
            return "["+id+"=>"+host+":"+port+"]";
        }
    }
    
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        for(Entry<Integer, Child> child : adj.entrySet())
        {
            Child c = child.getValue();
            Integer key = child.getKey();
            sb.append("[ID:"+key+" "+c.host+":"+c.port+"] ");
        }
        
        return "[ID:"+id+" Host:"+host+", Port:"+port+" Children:[ "+sb.toString()+"]]";
    }
}
