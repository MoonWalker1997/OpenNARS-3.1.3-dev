package nars.MC.channels;

import com.cyberbotics.webots.controller.CameraRecognitionObject;
import com.cyberbotics.webots.controller.Motor;
import nars.MC.EventBufferMC;
import nars.MC.OperationMC;
import nars.MC.SensorimotorChannelMC;
import nars.Slave;
import nars.entity.Task;
import nars.io.StringParser;
import nars.storage.Memory;
import com.cyberbotics.webots.controller.Robot;

import java.util.ArrayList;

public class ExpChannel6 extends SensorimotorChannelMC {

    private ArrayList<OperationMC> atomic_operations = null;

    double[] image_size = new double[]{64.0, 64.0};

    double threshold = 16;

    public ExpChannel6(String channel_ID, EventBufferMC eventBuffer, Memory memory) {
        super(channel_ID, eventBuffer, memory);
    }

    private Slave controller = new Slave();

    private Motor[] motors = new Motor[] {controller.getMotor("left wheel motor"),controller.getMotor("right wheel motor")};

    @Override
    public void execute(String name) {
        if (name.equals("^move")) {
            controller.step(32);
            motors[0].setVelocity(10);
            motors[1].setVelocity(10);
        }
        else if (name.equals("^turn")) {
            controller.step(32);
            motors[0].setVelocity(-10);
            motors[1].setVelocity(10);
        }
    }

    @Override
    protected ArrayList<Task> gathering() {
        controller.step(32);
        ArrayList<Task> tasks = new ArrayList<>();
        CameraRecognitionObject[] tmp = controller.camera.getRecognitionObjects();
        if (tmp != null) {
            for (int i = 0; i < tmp.length; i++) {
                String label_name = tmp[i].getModel();
                int[] position = tmp[i].getPositionOnImage();
                double dtc = Math.sqrt(Math.pow(position[0] - image_size[0] / 2, 2) + Math.pow(position[1] - image_size[1] / 2, 2));
                double frequency = 1.0;
                double confidence = 0.9;
                double priority;
                if (dtc <= threshold) {
                    if (dtc <= 0.5 * threshold) {
                        priority = 0.85;
                    } else {
                        priority = 0.85 * (threshold - dtc) / threshold;
                    }
                } else {
                    priority = 0;
                }
                double durability = 0.2;
                String narsese = "$" + priority + ";" + durability + "$ <(*,{Channel_2}, " + label_name + ") --> ^see>. %" + frequency + ";" + confidence + "%";
                tasks.add(StringParser.parseTask(narsese, this.memory, memory.getTime()));
            }
        }
        return tasks;
    }
}
