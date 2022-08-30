package nars.MC;

import nars.entity.Task;
import nars.storage.Memory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.sort;

class base_class {
    protected int a = 1;

    public void showA() {
        System.out.println(a);
    }

    public void fun1() {
        a += 1;
    }

    public void fun2() {
        a += 10;
    }

    public void fun() {
        fun1();
        fun2();
    }

}

class child1 extends base_class {
    public void setA(int newA) {
        this.a = newA;
    }

    public void fun1() {
        a += 100;
    }


}

public class draft{

    public static void main(String[] args) {
        base_class b = new base_class();
        child1 c = new child1();
        b.fun();
        b.showA();
        c.fun();
        c.showA();
    }



}
