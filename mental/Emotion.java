/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nars.mental;

import nars.entity.BudgetValue;
import nars.entity.Sentence;
import nars.entity.Stamp;
import nars.entity.Task;
import nars.entity.TruthValue;
import nars.inference.BudgetFunctions;
import nars.io.Symbols;
import nars.language.Inheritance;
import nars.language.SetInt;
import nars.language.Term;
import nars.main.Parameters;
import nars.storage.Memory;

/**
 *
 * @author Xiang
 */
public class Emotion {
    
    private final float HAPPY_EVENT_HIGHER_THRESHOLD = 0.75f;
    private final float HAPPY_EVENT_LOWER_THRESHOLD = 0.25f;
    private double lastHappy;
    private long last_happy_time = 0;
    private final double CHANGE_THRESHOLD = 0.25f;
    private final int CHANGE_STEPS_DEMANDED = 1000;
    
    private float happy;
    
    public Emotion(){
        happy = 0.5f;
        lastHappy = 0.5f;
    }
    
    public void resetEmotion(){
        
        happy = 0.5f;
        lastHappy = 0.5f;
        
    }
    
    public void adjustSatisfation(float newValue, float weight, Memory memory){
        
        happy += newValue * weight;
        happy /= 1.0f + weight;
        
        float frequency = -1;
        
        if(Math.abs(happy - lastHappy) > CHANGE_THRESHOLD && memory.getTime() - last_happy_time > CHANGE_STEPS_DEMANDED){
            
            if(happy > HAPPY_EVENT_HIGHER_THRESHOLD && lastHappy <= HAPPY_EVENT_HIGHER_THRESHOLD)
                frequency = 1.0f;
            else if(happy < HAPPY_EVENT_LOWER_THRESHOLD && lastHappy >= HAPPY_EVENT_LOWER_THRESHOLD)
                frequency = 0.0f;
            
            lastHappy = happy;
            last_happy_time = memory.getTime();
            
        }
        
        if(frequency != -1){
            
            Term predicate = SetInt.make(new Term("Satisfied"), memory);
            Term subject = Term.SELF;
            Inheritance inh = Inheritance.make(subject, predicate, memory);
            TruthValue truth = new TruthValue(happy, Parameters.DEFAULT_JUDGMENT_CONFIDENCE);
            Sentence sentence = new Sentence(inh, Symbols.JUDGMENT_MARK, truth, new Stamp(memory.getTime()));
            sentence.getStamp().setOccurrenceTime(memory.getTime());
            
            BudgetValue budgetOfNewTask = new BudgetValue(Parameters.DEFAULT_JUDGMENT_PRIORITY,
                                                          Parameters.DEFAULT_JUDGMENT_DURABILITY,
                                                          BudgetFunctions.truthToQuality(truth));
            
            Task task = new Task(sentence, budgetOfNewTask);
            
            
        }
        
    }
}
