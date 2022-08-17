/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nars.inference;

import nars.entity.TruthValue;

/**
 *
 * @author liyiy
 */
public class Testing {
    
    public static void main(String[] args) {
        
        for (float i = 0; i <= 1; i += 0.1) {
            
            for (float j = 0; j < 0.9; j += 0.1) {
                
                System.out.println("f: " + i + ", c " + j);
                System.out.println("Expectation: " + (new TruthValue(i, j)).getExpectation());
            }
            
        }
        
    }
    
}
