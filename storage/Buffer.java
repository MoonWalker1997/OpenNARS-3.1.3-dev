/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nars.storage;

import java.util.ArrayList;
import java.util.Iterator;
import nars.entity.Item;
import nars.entity.Task;

/**
 *
 * @author Xiang
 * @param <E>
 */
public abstract class Buffer<E extends Item> implements Iterable<E>{
    
    //private int duration;
    private final Memory memory;
    private String name;
    private final int capacity;
    private final ArrayList<E> itemTable;
    
    public Buffer(Memory memory, String name)
    {
        this.memory = memory;
        //this.duration = duration;
        this.name = name;
        capacity = capacity();
        itemTable = new ArrayList();
    }
    
    /*public int getDuration(){
        return duration;
    }*/
    
    /*public void setDuration(int duration){
        this.duration = duration;
    }*/
    
    public Memory getMemory(){
        return memory;
    }
    
    public String getName(){
        return name;
    }
    
    public void setName(String name){
        this.name = name;
    }
    
    public int getCapacity(){
        return capacity;
    }
    
    public ArrayList<E> getItemTable(){
        return itemTable;
    }
    
    protected abstract int capacity();
   
    protected abstract boolean putIn(E item, ArrayList<E> list, int capacity);
    
    protected abstract int findInsertIndex(E item);
    
    protected abstract Task takeOut();  

    protected abstract boolean isEmpty();
    
    //protected abstract ArrayList<E> eventInference(Task newEvent, boolean allowOverlap);
    
    @Override
    public Iterator<E> iterator(){
        return itemTable.iterator();
    }
}
