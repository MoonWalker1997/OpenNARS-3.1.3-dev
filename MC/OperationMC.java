package nars.MC;

import nars.Slave;

public abstract class OperationMC {

    private String name;

    public String getName() {
        return this.name;
    }

    // TODO
    // I don't know how to make this "execute()" function unspecific in arguments
    protected abstract void execute();
    protected abstract void execute(Slave controller);

}