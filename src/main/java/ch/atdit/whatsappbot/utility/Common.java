package ch.atdit.whatsappbot.utility;

import ch.atdit.whatsappbot.Bot;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.Keys;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Common {
    public static final String ENTER = "\n";
    public static final String SLASH = "/";

    public static void waitOff() {
        Bot.getDriver().manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
    }

    public static void waitOn() {
        wait(3);
    }

    public static void wait(int seconds) {
        Bot.getDriver().manage().timeouts().implicitlyWait(seconds, TimeUnit.SECONDS);
    }

    public static String cleanMessage(String message) {
        message = message.replaceAll(ENTER, Keys.chord(Keys.SHIFT, Keys.ENTER)).replaceAll("\\\\ENTER", "\n");
        return message;
    }

    @SuppressWarnings("SameParameterValue")
    public static String readFile(String file) {
        try {
            return FileUtils.readFileToString(new File(file), "utf-8");
        } catch (IOException e) {
            e.printStackTrace();
            exit(1);
            return null;
        }
    }

    @SuppressWarnings("SameParameterValue")
    public static void saveFile(String string, String file) {
        try {
            FileUtils.writeStringToFile(new File(file), string, "utf-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("SameParameterValue")
    public static void exit(int code) {
        Runtime.getRuntime().exit(code);
    }

    @SuppressWarnings("SameParameterValue")
    public static void printN(Object object) {
        System.out.print(object);
    }

    @SuppressWarnings("SameParameterValue")
    public static void print(Object object) {
        System.out.println(object);
    }

    public static void print() {
        System.out.println();
    }

    public static void delay(long time) {
        try {
            Thread.sleep(time);
        } catch (Exception e) {
            print("Caught error while trying to delay...");
            e.printStackTrace();
        }
    }
}
