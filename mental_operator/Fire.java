/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nars.mental_operator;

import java.util.ArrayList;
import nars.entity.Task;
import nars.language.Operation;
import nars.language.Operator;
import nars.language.Term;
import nars.storage.Memory;

/**
 *
 * @author liyiy
 */
public class Fire extends Operator{
    
    public Fire() {
        super("^fire");        
    }

    public void escapeFeedback(final Term content, final Task t, final Memory memory, final long time) {

    }

    @Override
    protected ArrayList<Task> execute(Operation operation, ArrayList<Term> args, Memory memory, long time) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
