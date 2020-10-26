package ch.atdit.whatsappbot;

import ch.atdit.whatsappbot.classes.Channels;
import ch.atdit.whatsappbot.classes.OS;
import ch.atdit.whatsappbot.commands.Command;
import ch.atdit.whatsappbot.commands.Say;
import ch.atdit.whatsappbot.commands.Status;
import ch.atdit.whatsappbot.jobs.Job;
import ch.atdit.whatsappbot.jobs.MessageJob;
import com.mashape.unirest.http.Unirest;
import lombok.Getter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.NoSuchElementException;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static ch.atdit.whatsappbot.utility.Common.*;

@SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal"})
public class Bot {

    @Getter
    private static boolean debugMode = false;

    @Getter
    private static final String osString = System.getProperty("os.name");
    private static final OS os = getOS();

    private static final String version = "1.1";

    private static final DecimalFormat twoDecimals = new DecimalFormat("#0.00");
    @Getter
    private static final SimpleDateFormat ddMMyyyyHHmmss = new SimpleDateFormat("dd.MM.yyyy - HH.mm.ss");

    private static String driverLocation;
    private static String windowsDriverLocation;
    private static String linuxDriverLocation;

    private static String whatsappweb;
    private static String server;
    private static String token;

    private static String prefix;

    private static Set<Command> commands = new HashSet<>();
    private static HashMap<String, Integer> permissionLevels = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static <T extends Command> T getCommand(String name) {
        for (Command command : commands) {
            print("command.name: <" + command.name + ">");
            print("name: <" + name + ">");
            if (command.name.equalsIgnoreCase(name)) return (T) command;
        }
        return null;
    }

    private static String klassenchat;
    private static String schwerpunktfach;
    private static String ergaenzungsfach;

    private static HashMap<String, String> subjectAbbreviations = new HashMap<>();
    private static HashMap<String, Channels> subjectChannels = new HashMap<>();

    @Getter
    private static WebDriver driver;

    private static final String testingChat = "BotTest";
    private static final String standbyChat = "Standby";

    private static final String initializationMessage =
            "*Notifier Initialization*\n" +
            "\nOS: " + osString +
            "\nSystem Time: " + System.currentTimeMillis() +
            "\nSystem Date: " + ddMMyyyyHHmmss.format(new Date(System.currentTimeMillis())) +
            "\nDebug: " + debugMode;
    private static boolean sendInitializationMessage = false;

    private static Timer heartbeatTimer;
    private static TimerTask heartbeatTimerTask;

    private static Timer queueTimer;
    private static TimerTask queueTimerTask;

    private static Timer messageCollectorTimer;
    private static TimerTask messageCollectorTimerTask;
    private static boolean messageCollection = false;

    private static Timer debugTimer;
    private static TimerTask debugTimerTask;
    private static final int[] onlineCounter = {0};
    private static final int[] offlineCounter = {0};
    private static int connectionFailures = 0;

    @Getter
    private static List<Job> queue = new ArrayList<>();
    private static boolean loggedIn = false;

    private static OS getOS() {
        String output = osString.toLowerCase();

        if (output.contains("windows")) {
            return OS.WINDOWS;
        } else if (output.contains("mac")) {
            return OS.MAC;
        } else {
            return OS.OTHER;
        }
    }

    private static void registerTimerTasks() {
        heartbeatTimer = new Timer();
        heartbeatTimerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    JSONObject jsonResponse = Unirest.post(server)
                            .field("token", token)
                            .asJson().getBody().getObject();

                    JSONArray jsonNoten = jsonResponse.getJSONArray("noten");

                    if (jsonNoten.length() > 0) {
                        print("[HB-T] Found " + jsonNoten.length() + " new note" + (jsonNoten.length() == 1 ? "" : "s") + "!");

                        for (int i = 0; i < jsonNoten.length(); i++) {
                            JSONObject jsonNote = jsonNoten.getJSONObject(i);

                            String subject = jsonNote.getString("subject");
                            String topic = jsonNote.getString("topic");
                            String date = jsonNote.getString("date");

                            subject = subjectAbbreviations.getOrDefault(subject.contains("-") ? subject.split("-")[0].toLowerCase() : subject.split("\\.")[0].toLowerCase(), subject);
                            //subject = subjectAbbreviations.getOrDefault(subject, subject);

                            print();
                            String noteToSend = "*Neue Note:*" + ENTER + ENTER +
                                    subject + ENTER +
                                    topic + ENTER +
                                    date;
                            print(noteToSend);
                            print();

                            List<String> chatsToSend = getChatsToSend(jsonNote.getString("subject"));

                            for (String chat : chatsToSend) {
                                if (debugMode) break;
                                print("[HB-T] Sending to chat: " + chat);
                                MessageJob job = new MessageJob(chat, noteToSend);
                                queue.add(job);
                            }

                            // Leave this for debugging and logging in bot group
                            MessageJob job = new MessageJob(testingChat, noteToSend + ENTER + ENTER + "Chats: " + String.join(", ", chatsToSend));
                            queue.add(job);
                        }
                    }
                } catch (Exception e) {
                    connectionFailures++;
                }
            }
        };

        queueTimer = new Timer();
        queueTimerTask = new TimerTask() {
            @Override
            public void run() {
                if (!loggedIn) return;

                List<Job> currentQueue = new ArrayList<>(queue);

                int size = currentQueue.size();

                if (size > 0) {
                    print("[Queue] Found " + size + " new job" + (size == 1 ? "" : "s") + "!");
                }

                for (int i = 0; i < size; i++) {
                    Job job = currentQueue.get(i);

                    print("[Queue] Running job (" + (i + 1) + "/" + size + "): " + job.name);
                    job.start();
                    print("[Queue] Done with job (" + (i + 1) + "/" + size + ")");
                    queue.remove(job);
                    selectStandbyChat();
                }

                currentQueue.clear();
            }
        };

        messageCollectorTimer = new Timer();
        messageCollectorTimerTask = new TimerTask() { // Doesn't work, don't use this
            @Override
            public void run() { // ToDo: Put this as job in the queue!
                if (!loggedIn) return;

                try {
                    List<WebElement> unreadChats = driver.findElements(By.className("_31gEB"));

                    if (unreadChats.size() > 0) {
                        print("Unread chats: " + unreadChats.size());
                        print();

                        for (WebElement unreadChat : unreadChats) { // _31gEB
                            int messages = -1; // Assume -1 if number doesn't exist or can't be parsed

                            try {
                                messages = Integer.parseInt(unreadChat.getText());
                            } catch (NumberFormatException ignored) {}

                            // Find newest message in chat element itself
                            WebElement chatElement = unreadChat.findElement(By.xpath("../../../../..")); // _2kHpK
                            WebElement chatNameElement = chatElement.findElement(By.tagName("span"));
                            WebElement messageElement = chatElement.findElement(By.className("_2iq-U"));

                            WebElement messageAuthorElement = messageElement.findElement(By.xpath(".//span[@dir = 'auto']"));
                            WebElement messageContentElement = messageElement.findElement(By.xpath(".//span[@dir = 'ltr']"));

                            String chatName = chatNameElement.getText();
                            String author = messageAuthorElement.getText();
                            String content = messageContentElement.getText();

                            /*
                            // If @<User> has been used, the information must be added to content
                            try {
                                if (doesClassExist(messageContentElement, "at-symbol")) {
                                    if (messageContentElement.findElement(By.className("at-symbol")).getText().equalsIgnoreCase("@")) {
                                        content = messageContentElement.findElement(By.xpath(".//span[@dir = 'ltr']")).getText() + content;
                                    }
                                }
                            } catch (Exception ignored) {}
                            */

                            print(chatName + ": Has " + messages + " unread message" + (messages == 1 ? "" : "s") + ":");
                            print("Last unread message:");
                            print("Author: <" + author + ">");
                            print("Content: <" + content + ">");
                            print();

                            chatNameElement.click(); // Click the chat to mark the chat as read

                            selectStandbyChat(); // Quickly switch back to standby chat to prevent other messages from not being registered

                            String commandString = content.split(" ")[0].toLowerCase();
                            print("CommandString: <" + commandString + ">");

                            if (commandString.startsWith(prefix)) {
                                print("Command starts with prefix!");
                                Command command = getCommand(commandString.substring(prefix.length()));

                                if (command != null) {
                                    print("Command isn't null, running...");
                                    command.run(chatName, author, content);
                                }
                            }



                            /*

                            WebElement messagesElement = driver.findElement(By.className("z_tTQ")); // Might not even need this

                            List<WebElement> messageDivs = messagesElement.findElements(By.className("_274yw"));

                            print("Message Divs: " + messageDivs.size());

                            print();

                            // Print all messages
                            for (int i = 0; i < messageDivs.size(); i++) { // Each message
                                WebElement messageElement = messageDivs.get(i);

                                String author = "";

                                List<WebElement> messageDivElements = messageElement.findElements(By.tagName("div"));

                                for (WebElement messageDiv : messageDivElements) {
                                    print(messageDiv.getAttribute("class"));
                                }

                                print();
                                print();
                            }
                            */
                        }

                        print();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        debugTimer = new Timer();
        debugTimerTask = new TimerTask() {
            @Override
            public void run() {
                if (driver.getPageSource().contains("Phone not connected")) {
                    offlineCounter[0] += 1;
                } else {
                    onlineCounter[0] += 1;
                }

                try {
                    if (((onlineCounter[0] + offlineCounter[0]) % (5 * 60)) != 0) return; // Only log downtime every 5 minutes
                    double downtimePercentage = ((double) offlineCounter[0]) / ((double) onlineCounter[0] + (double) offlineCounter[0]) * 100;

                    print("[Debug] Downtime: " + twoDecimals.format(downtimePercentage) + "% (" + onlineCounter[0] + "s on, " + offlineCounter[0] + "s off), Connection failures: " + connectionFailures);
                } catch (Exception ignored) {}
            }
        };

        heartbeatTimer.schedule(heartbeatTimerTask, 0, 1000);
        queueTimer.schedule(queueTimerTask, 0, 10);
        if (messageCollection) messageCollectorTimer.schedule(messageCollectorTimerTask, 0, 1000);
        debugTimer.schedule(debugTimerTask, 0, 1000);
    }

    @SuppressWarnings("SameParameterValue")
    private static boolean doesClassExist(WebElement element, String className) { // ToDo: Change this to use the html source code to check if class exists!!!!! (innerHTML)
        return element.findElements(By.className(className)).size() > 0;
    }

    private static void selectStandbyChat() { // Always is run within jobs, so mustn't be a job itself!
        if (!loggedIn) return;

        delay(300);

        WebElement chatElement = driver.findElement(By.xpath("//span[@title = '" + standbyChat + "']"));
        chatElement.click();

        delay(300);
    }

    private static List<String> getChatsToSend(String subject) { // ToDo: Finish this and consider white/blacklist!
        subject = subjectAbbreviations.getOrDefault(subject.contains("-") ? subject.split("-")[0].toLowerCase() : subject.split("\\.")[0].toLowerCase(), subject); // Or default for debugging

        List<String> chats = new ArrayList<>();

        for (String chat : subjectChannels.keySet()) {
            Channels channels = subjectChannels.get(chat);

            if (channels.whitelist.size() > 0) {
                if (channels.whitelist.contains(subject)) {
                    chats.add(chat);
                    continue;
                }
            }

            if (channels.blacklist.size() > 0) {
                if (!channels.blacklist.contains(subject)) {
                    chats.add(chat);
                }
            }
        }

        return chats;
    }

    private static void loadConfig() {
        String configString = readFile("config.json");

        if (configString == null) {
            print("Config is null!");
            exit(1);
        }

        JSONObject jsonConfig = new JSONObject(configString);

        JSONObject jsonSubjectAbbreviations = (JSONObject) jsonConfig.get("subject_abbreviations");

        for (Object object : jsonSubjectAbbreviations.keySet()) {
            String key = object.toString();
            String value = jsonSubjectAbbreviations.getString(key);

            subjectAbbreviations.put(key.toLowerCase(), value);
        }

        JSONObject jsonPermissions = jsonConfig.getJSONObject("permissions");

        for (String user : jsonPermissions.keySet()) {
            int permissionLevel = jsonPermissions.getInt(user);
            permissionLevels.put(user, permissionLevel);
        }

        JSONObject jsonDrivers = jsonConfig.getJSONObject("drivers");
        windowsDriverLocation = jsonDrivers.getString("windows");
        linuxDriverLocation = jsonDrivers.getString("linux");

        whatsappweb = jsonConfig.getString("whatsappweb");
        server = jsonConfig.getString("server");
        token = jsonConfig.getString("token");

        prefix = jsonConfig.getString("prefix");

        JSONObject jsonChats = jsonConfig.getJSONObject("chats");
        JSONObject jsonKlassenchat = jsonChats.getJSONObject("klassenchat");
        JSONObject jsonSchwerpunktfach = jsonChats.getJSONObject("schwerpunktfach");
        JSONObject jsonErgaenzungsfach = jsonChats.getJSONObject("ergänzungsfach");

        klassenchat = jsonKlassenchat.getString("name");
        schwerpunktfach = jsonSchwerpunktfach.getString("name");
        ergaenzungsfach = jsonErgaenzungsfach.getString("name");

        HashMap<String, JSONObject> jsonChatSubjects = new HashMap<>();
        jsonChatSubjects.put(klassenchat, jsonKlassenchat.getJSONObject("subjects"));
        jsonChatSubjects.put(schwerpunktfach, jsonSchwerpunktfach.getJSONObject("subjects"));
        jsonChatSubjects.put(ergaenzungsfach, jsonErgaenzungsfach.getJSONObject("subjects"));

        for (String chat : jsonChatSubjects.keySet()) {
            JSONArray jsonWhitelist = jsonChatSubjects.get(chat).getJSONArray("whitelist");
            JSONArray jsonBlacklist = jsonChatSubjects.get(chat).getJSONArray("blacklist");

            List<String> whitelist = new ArrayList<>();
            List<String> blacklist = new ArrayList<>();

            for (int i = 0; i < jsonWhitelist.length(); i++) {
                whitelist.add(jsonWhitelist.getString(i));
            }

            for (int i = 0; i < jsonBlacklist.length(); i++) {
                blacklist.add(jsonBlacklist.getString(i));
            }

            Channels channels = new Channels(whitelist, blacklist);
            subjectChannels.put(chat, channels);
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isLoggedIn() { // ToDo: Make it faster and more reliable using implicitly wait or innerHTML
        //return !driver.getPageSource().contains("Phone not connected");

        boolean loggedIn = false;

        waitOff();
        try {
            WebElement searchBar = driver.findElement(By.className("J3VFH"));
            if (searchBar.getText().equalsIgnoreCase("search or start new chat")) loggedIn = true;
        } catch (NoSuchElementException ignored) {}

        try {
            WebElement connectionBar = driver.findElement(By.className("aaIq_"));
            if (connectionBar.getText().equalsIgnoreCase("phone not connected")) loggedIn = false;
        } catch (NoSuchElementException ignored) {}
        waitOn();

        return loggedIn;
    }

    public static int getPermissionLevel(String sender) {
        return permissionLevels.getOrDefault(sender, 0);
    }

    private static void registerCommands() {
        commands.add(new Status());
        commands.add(new Say());
    }

    private static boolean initialize() {
        loadConfig();

        registerCommands();

        if (os == OS.WINDOWS) {
            if (!new File(windowsDriverLocation).exists()) {
                print("Windows driver not found under: " + windowsDriverLocation);
                exit(1);
            }
            driverLocation = windowsDriverLocation;
        } else if (os == OS.OTHER) {
            if (!new File(linuxDriverLocation).exists()) {
                print("Linux driver not found under: " + linuxDriverLocation);
                exit(1);
            }
            driverLocation = linuxDriverLocation;
        }

        System.setProperty("webdriver.chrome.driver", driverLocation);
        driver = new ChromeDriver();

        return true;
    }

    public static void main(String[] args) {
        if (!initialize()) exit(1);

        print();
        print("WhatsApp WebBot v-" + version + " started, scheduling heartbeats...");
        print();
        registerTimerTasks();
        print("[MAIN] Scheduled heartbeats.");
        print();

        driver.manage().window().maximize();
        driver.manage().deleteAllCookies();
        driver.manage().timeouts().pageLoadTimeout(40, TimeUnit.SECONDS);
        driver.manage().timeouts().implicitlyWait(3, TimeUnit.SECONDS);
        driver.get(whatsappweb);

        printN("[MAIN] Waiting for QR code scan");

        long before = System.currentTimeMillis();

        while (!isLoggedIn()) {
            printN(".");
            delay(500);
        }

        long after = System.currentTimeMillis();

        print();
        print();

        loggedIn = true;

        selectStandbyChat();

        print("[MAIN] Loaded after " + twoDecimals.format(((double) after - (double) before) / 1000) + " seconds!");
        print();

        onlineCounter[0] = 0;
        offlineCounter[0] = 0;

        if (sendInitializationMessage) {
            delay(1000);

            queue.add(new MessageJob(testingChat, initializationMessage));
        }
    }
}
