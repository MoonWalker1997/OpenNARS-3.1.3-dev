/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nars.language;

import java.util.ArrayList;
import java.util.Arrays;
import nars.entity.Task;
import nars.io.Symbols;

/**
 *
 * @author Xiang
 */
public class Operation extends Inheritance{
    
    private Task task;
    public final static ArrayList<Term> SELF = new ArrayList(Arrays.asList(Term.SELF));
    
    public Operation(ArrayList<Term> arg) {
        super(arg);
    }
    
    protected Operation(Term argProduct, Term operator){
        super(new ArrayList(Arrays.asList(argProduct, operator)));
    }
    
    @Override
    public Operation clone(){
        return new Operation(this.getComponents());
    }
    
    public static Operation make(Operator operator, ArrayList<Term> arg){
        return new Operation(new Product(arg), operator);
    }
    
    public Operator getOperator(){
        return (Operator)getPredicate();
    }
    
    public String makeName(String op, ArrayList<Term> arg){
        
        StringBuilder nameBuilder = new StringBuilder();
        nameBuilder.append(Symbols.COMPOUND_TERM_OPENER);
        nameBuilder.append(op);
        
        for(Term t : arg){    
            nameBuilder.append(Symbols.ARGUMENT_SEPARATOR);
            nameBuilder.append(t.getName());       
        }
        nameBuilder.append(Symbols.COMPOUND_TERM_CLOSER);
        return nameBuilder.toString();
    }
    
    public void setTask(Task task){
        this.task = task;
    }
    
    public Task getTask(){
        return task;
    }
    
    public Product getArguments(){
        return (Product)this.getSubject();
    }
}
