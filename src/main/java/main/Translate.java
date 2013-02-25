package main;
import com.google.devtools.j2objc.J2ObjC;


public class Translate {
    public static void main(String[] args) {
        J2ObjC.main(new String[] {
                //"--prefix", "com.google=G",
                "-v",
                //"-use-arc",
                "--no-package-directories",
                "-emit-llvm",
                "-d", System.getProperty("user.dir") + "/src/main/java",
                "-sourcepath", System.getProperty("user.dir") + "/src/main/java",
                "-classpath", System.getProperty("java.class.path"),
                "src/main/java/main/Main.java"
        });
    }
}
