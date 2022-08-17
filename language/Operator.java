/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nars.language;

import java.util.ArrayList;
import nars.entity.BudgetValue;
import nars.entity.Task;
import nars.entity.TruthValue;
import nars.main.Parameters;
import nars.storage.Memory;

/**
 * 操作符
 * @author Xiang
 */
public abstract class Operator extends Term{
    
    protected Operator(){
        super();
    }
    
    protected Operator(String name){
        
        super(name);
        // 加入操作符不是以^开头，那么返回一个exception
        if(!name.startsWith("^"))
            throw new IllegalStateException("Operator name needs ^ prefix");
        
    }
    
    /**
     * 每个operator都需要的方法，尤其是对应的操作
     * @param operation 操作
     * @param args 
     * @param memory
     * @param time
     * @return 
     */
    protected abstract ArrayList<Task> execute(Operation operation, ArrayList<Term> args, Memory memory, long time);
    
    /**
     * 施行操作的标准方式，实施operator中定义的实行方法，并且解决feedback任务作为input
     * @param operation the operator to be executed
     * @param args the arguments to be taken byt he operator
     * @param memory the reference to the memory
     * @param time used to retrieve the time
     * @return  true if successful, false if an error occurred
     */
    public final boolean call(Operation operation, ArrayList<Term> args, Memory memory, long time){
        // 初始化feedback
        ArrayList<Task> feedback = null;
        // 尝试执行一个操作
        try{
            feedback = execute(operation, args, memory, time);
        }catch(Exception ex){ 
            System.out.println("No feedback is available");
            return false;
        }

        // 如果反馈为空，或者反馈列表为空，则在记忆中执行任务
        if(feedback == null || feedback.isEmpty())
            memory.executeTask(time, operation, new TruthValue(1f, Parameters.DEFAULT_JUDGMENT_CONFIDENCE));
        
        //reportExecution(operation, args, feedback, memory);
        // 如果反馈不为空
        if(feedback != null){
            // 遍历整个反馈
            for (Task t : feedback) 
            {   
                // 将每个任务的创建时间都设置为给定时间，一般都是当前时间
                t.getStamp().setCreationTime(time, Parameters.DURATION);
                //memory.addTaskIntoInternalBuffer(t);
            }
        }
        
        return true;
    }
    
    /**
     * 调用
     * @param op
     * @param memory
     * @param time
     * @return 
     */
    public boolean call(Operation op, Memory memory, long time){
        // 如果操作是不可执行的，返回假，判断条件为op是否为一个operation
        if(!op.isExecutable(memory))
            return false;
        // arg为op的词项
        Product args = op.getArguments();
        return call(op, args.getComponents(), memory, time);
    }
    
    public Operator clone(){
        return this;
    }
    
    public static class ExecutionResult{
        
        private final Operation operation;
        private final Object feedback;
        
        public ExecutionResult(Operation op, Object feedback){
            this.operation = op;
            this.feedback = feedback;
        }
        
        public Task getTask(){
            return operation.getTask();
        }
        
        @Override
        public String toString(){
            
            BudgetValue b = null;
            
            if(getTask() != null)
                b = getTask().getBudget();
            
            ArrayList<Term> args = operation.getArguments().getComponents();
            Operator operator = operation.getOperator();
            
            return "";
            
        }
        
    }
    
    public static String addPrefix(String opName){
        
        if(!opName.startsWith("^"))
            return "^" + opName;
        
        return opName;
        
    }
}
