///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package nars.io;
//
//import nars.main.NAR;
//import nars.storage.Buffer;
//import nars.storage.Memory;
//
///**
// *
// * @author Xiang
// */
//public class NarseseChannel implements InputChannel{
//
//    private NAR reasoner;
//    private String name;
//    public NarseseChannel(NAR reasoner){
//        this.reasoner = reasoner;
//        name = "Narsese Channel";
//    }
//
//    public void openNarsese(){
//        System.out.println("Open the Narsese Channel");
//
//        reasoner.addInputChannel(this);
//    }
//
//    @Override
//    public boolean nextInput() {
//        System.out.println("Next Narsese Input");
//        return false;
//    }
//
//
//
//}
