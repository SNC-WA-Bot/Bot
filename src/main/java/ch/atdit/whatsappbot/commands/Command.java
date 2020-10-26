package ch.atdit.whatsappbot.commands;

import ch.atdit.whatsappbot.Bot;
import ch.atdit.whatsappbot.jobs.MessageJob;
import org.openqa.selenium.WebDriver;

import java.util.Arrays;
import java.util.List;

import static ch.atdit.whatsappbot.utility.Common.*;

public abstract class Command {
    public String NO_PERMISSIONS = "Insufficient permissions.\nYour permission level: %AUTHOR_PERMISSION_LEVEL%\nRequired permission level: %PERMISSION_LEVEL%";

    // These variables must be set in the constructor for each command!
    public String name;
    public int permissionLevel;

    public static WebDriver driver = Bot.getDriver();

    public String chat;
    public String author;
    public String content;
    public List<String> args;

    public boolean run(String chat, String author, String content) {
        this.chat = chat;
        this.author = author;
        this.content = content;
        args = Arrays.asList(content.split(" "));
        args = args.subList(1, args.size());

        print();
        print("Running command...");
        print("Chat: <" + chat + ">");
        print("Author: <" + author + ">");
        print("Content: <" + content + ">");
        print("Args (joined): <" + String.join(";", args) + ">");

        return Bot.getPermissionLevel(author) >= permissionLevel;
    }

    public void respond(String response) {
        MessageJob responseJob = new MessageJob(chat, "@" + author + "\\ENTER" + response);
        Bot.getQueue().add(responseJob);
    }

    public void send(String chat, String message) {
        MessageJob responseJob = new MessageJob(chat, message, false);
        Bot.getQueue().add(responseJob);
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean noPermissions() {
        String response = NO_PERMISSIONS;
        response = response.replaceAll("%PERMISSION_LEVEL%", String.valueOf(permissionLevel));
        response = response.replaceAll("%AUTHOR_PERMISSION_LEVEL%", String.valueOf(Bot.getPermissionLevel(author)));
        respond(response);

        return false;
    }
}
