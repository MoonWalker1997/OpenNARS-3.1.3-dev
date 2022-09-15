**How to use OpenNARS in Webots** | Tangrui Li (tuo90515@temple.edu)

<font color="red"> [last edited 22/9/15] </font>



We may find many tutorials to connect Java external controllers to Webots, but some of them are not to date, even the official documentation.<font color="red"> [By the way, while writing this document, I found the official documentation is just updated, as long as Webots. So I am not sure whether this document is still valid.]</font> Here is what I did to let Java controller run in Webots. But note that this is not about using terminals; this is based on IDEA. But I think it will be simpler to run on terminals.



1. Environment variable configuration.

   Following what it is on this official page. https://www.cyberbotics.com/doc/guide/running-extern-robot-controllers?version=master

2. IDEA configuration.

   1. File-Project Structure, add "%WEBOTS HOME%\lib\controller\java\Controllar.jar". Note that this "%WEBOTS HOME%" is just a placeholder, use your absolute address. 
   2. File-Project Structure, add your controller as well. This controller should be well-defined, I recommend to use one official controller "Slave" (I don't like the name) to start. I will provide it in the repository (in "Webots Appendix"). Make sure you add the folder but not all these files separately.
   3. After importing these above stuffs, you may see them in File-Project Structure, and you may notice the scope of these files should be "Compile". They are "Runtime" as default.

![image-20220915161015948](https://raw.githubusercontent.com/MoonWalker1997/OpenNARS-3.1.3-dev/main/Webots%20appendix/resources/How%20to%20use%20OpenNARS%20in%20Webots%201.jpg)

4. Then you need to go to the Java class you want to run with the controller running. For example, I would like to run the controller while running "Shell.java". 

   ![image-20220915161224894](https://raw.githubusercontent.com/MoonWalker1997/OpenNARS-3.1.3-dev/main/Webots%20appendix/resources/How%20to%20use%20OpenNARS%20in%20Webots%202.jpg)

   Select "Edit Configurations", select the blue "Modify options", then select "Add VM options" then add "-Djava.library.path=${WEBOTS_HOME}\lib\controller\java" to that.

   ![image-20220915161341984](https://raw.githubusercontent.com/MoonWalker1997/OpenNARS-3.1.3-dev/main/Webots%20appendix/resources/How%20to%20use%20OpenNARS%20in%20Webots%203.jpg)



After that, I think you can run the Java external controller if you also make the configuration in Webots (select the controller of your robot as "extern"). In the simulation, you can see it is not paused, but the stopwatch is not running. It is normal since Webots is waiting for your Java controller. If you run your script now, the simulation will get started.