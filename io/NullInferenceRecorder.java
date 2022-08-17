/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nars.io;

/**
 * @author Peter 
 * Empty class that does nothing. This was moved from memory.java
 * since it is more appropriate for it to be separate in IO package.
 */
public class NullInferenceRecorder implements IInferenceRecorder {

    @Override
    public void init() {
    }

    @Override
    public void show() {
    }

    @Override
    public void play() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void append(String s) {
    }

    @Override
    public void openLogFile() {
    }

    @Override
    public void closeLogFile() {
    }

    @Override
    public boolean isLogging() {
        return false;
    }
}
