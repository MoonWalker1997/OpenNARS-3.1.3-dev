/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nars.main;

import nars.MC.channels.ExpChannel5;
import nars.io.OutputChannel;
import nars.io.StringParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * @author Pei, Xiang, Patrick, Peter
 */
public class Shell {

    static String inputString = "";


    private static class InputThread extends Thread {
        private final BufferedReader bufIn;
        private final NAR reasoner;

        InputThread(final InputStream in, NAR reasoner) {
            this.bufIn = new BufferedReader(new InputStreamReader(in));
            this.reasoner = reasoner;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    final String line = bufIn.readLine();
                    if (line != null) {
                        synchronized (inputString) {
                            inputString = line;
                            if ("".equals(line)) {
                                inputString = "1";
                            }
                        }
                    }

                } catch (final IOException e) {
                    throw new IllegalStateException("Could not read line.", e);
                }

                try {
                    Thread.sleep(1);
                } catch (final InterruptedException e) {
                    throw new IllegalStateException("Unexpectadly interrupted while sleeping.", e);
                }
            }
        }
    }


    public static class ShellOutput implements OutputChannel {
        @Override
        public void nextOutput(ArrayList<String> arg0) {
            for (String s : arg0) {
                if (!s.matches("[0-9]+")) {
                    System.out.println(s);
                }
            }
        }

        @Override
        public void tickTimer() {
        }
    }


    public static void main(String[] args) {
        NAR reasoner = new NAR();

        // as an initialization process of a system, some basic beliefs and goals are input
//        reasoner.memory.immediateProcess(StringParser.parseTask("<^move =/> know_the_world>.", reasoner.memory, reasoner.memory.getTime()));
//        reasoner.memory.immediateProcess(StringParser.parseTask("<^turn =|> know_the_world>.", reasoner.memory, reasoner.memory.getTime()));
        // it looks that the current version does not support the "goal reasoning".
        reasoner.memory.immediateProcess(StringParser.parseTask("^turn!", reasoner.memory, reasoner.memory.getTime()));

        int round = 1;
        reasoner.walk(90 * round + 20);

        for (int i = 0; i < 90 * round; i++) {
            if (i == 50) {
                int A = 1;
            }
            System.out.println(i + "=============================>");
            reasoner.cycle();
        }

        System.out.println("\n\n\nFOR NEW IMAGES\n\n\n");

        ((ExpChannel5) reasoner.inputChannels.get(1)).test_mode();

        for (int i = 0; i < 90; i++) {
            System.out.println(i + "=============================>");
            reasoner.cycle();
        }


//        System.out.println("=============================>");
//        reasoner.memory.immediateProcess(StringParser.parseTask("(&|, <(*, {Channel_3}, nothing)-->^see>, <(*, {Channel_1}, sting)-->^see>, <(*, {Channel_2}, pain)-->^feel>)?", reasoner.memory, reasoner.memory.getTime()));
//        System.out.println("Time " + 400 + " | Input from User: " + "(&|, <(*, {Channel_3}, nothing)-->^see>, <(*, {Channel_1}, sting)-->^see>, <(*, {Channel_2}, pain)-->^feel>)?");
//        for (int i = 0; i < 50; i++) {
//            reasoner.memory.workCycle(400+i);
//        }

        // (&|, <(*, {Channel_3}, nothing)-->^see>, <(*, {Channel_1}, sting)-->^see>, <(*, {Channel_2}, pain)-->^feel>)?

//        reasoner.addOutputChannel(new ShellOutput());
//        InputThread t = new InputThread(System.in, reasoner);
//        t.start();
//        System.out.println("Welcome to OpenNARS v" + Parameters.VERSION  +
//                           " Shell! Type Narsese input and press enter, use questions to get "
//                         + "answers or increase volume with *volume=n with n=0..100");
//        reasoner.run();
//        reasoner.getSilenceValue().set(0);
//        int cnt = 0;
//        while(true){
//            synchronized(inputString) {
//                if(!"".equals(inputString)) {
//                    try {
//                        if(inputString.startsWith("*volume=")) { //volume to be consistent with OpenNARS
//                            int val = Integer.parseInt(inputString.split("\\*volume=")[1]);
//                            if(val >= 0 && val <= 100) {
//                                reasoner.getSilenceValue().set(100-val);
//                            } else{
//                                System.out.println("Volume ignored, not in range");
//                            }
//                        } else {
//                            reasoner.textInputLine(inputString);
//                        }
//                        inputString = "";
//                    } catch(Exception ex) {
//                        inputString = "";
//                    }
//                }
//            }
//            if(reasoner.getWalkingSteps() > 0)
//                reasoner.cycle();
//            cnt++;
    }
}
