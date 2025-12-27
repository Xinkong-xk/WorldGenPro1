package io.xk.worldgenpro.debug;

public class Debug {
    public static boolean debug = true;

    public static void info(String message){
        if(debug){
            System.out.println(message);
        }
    }
}
