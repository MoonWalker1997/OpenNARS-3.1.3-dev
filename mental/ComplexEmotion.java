/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nars.mental;

import nars.entity.Concept;
import nars.entity.Task;
import nars.storage.Memory;

/**
 *
 * @author Xiang
 */
public class ComplexEmotion {
    
    public static void fear(Memory memory, Task task){
        
        Concept c = memory.termToConcept(task.getContent());
        
        /*if(c != null && !c.getDesires().isEmpty()){
            
            if(task.getSentence().getTruth().getExpectation() >= 0.5 && c.getDesires().get(0).getSentence().getTruth().getExpectation() < 0.5)
            
        }*/
        
    }
    
}
