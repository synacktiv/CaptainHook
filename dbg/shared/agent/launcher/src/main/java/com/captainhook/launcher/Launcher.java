package com.captainhook.launcher;

import net.bytebuddy.agent.ByteBuddyAgent;
import java.io.File;
import java.lang.Integer;


public class Launcher {

    public static final String HELP = "Command:\n\tjava -jar launcher.jar [path to agent .jar] [PID of running JVM]";
    public static final String FILE_ERROR = "An error occurred while trying to open the agent JAR file.\nPlease provide a correct path to the agent JAR as first argument.";
    public static final String PID_ERROR = "The PID (second argument) must be an integer.";

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println(HELP);
            System.exit(-1);
        }
        
        File f;
        try {
            f = new File(args[0]);
        } catch (Exception e) {
            System.out.println(FILE_ERROR);
            System.exit(-1);
        }
        try {
            Integer.parseInt(args[1]);;
        } catch (Exception e) {
            System.out.println(PID_ERROR);
            System.exit(-1);
        }

        System.out.println("Injecting the agent in the main JVM...");
        try {
            ByteBuddyAgent.attach(f, args[1]);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        
        System.out.println("Injected!");
    }
}
