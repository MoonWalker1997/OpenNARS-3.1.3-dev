///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package nars.io;
//
//import nars.main.NAR;
//
///**
// *
// * @author Xiang
// */
//public class KnowledgeChannel implements InputChannel{
//
//    private final NAR reasoner;
//    private final String name;
//
//    public KnowledgeChannel(NAR reasoner){
//        this.reasoner = reasoner;
//        name = "Knowledge Channel";
//    }
//
//    public void openKnowledgeChannel(){
//        System.out.println("Open the Knowledge Channel");
//        reasoner.addInputChannel(this);
//    }
//
//    @Override
//    public boolean nextInput() {
//        System.out.println("Next Knowledge Input");
//        return false;
//    }
//
//}
