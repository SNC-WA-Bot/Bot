package ch.atdit.whatsappbot.commands;

import ch.atdit.whatsappbot.Bot;

import java.util.Date;

public class Status extends Command {
    public Status() {
        name = "status";
        permissionLevel = 1;
    }

    @Override
    public boolean run(String chat, String author, String content) {
        if (!super.run(chat, author, content)) return false; // or return only false if no feedback should be provided

        String response = "\n\n*Status*\n" +
                "\nDebug Mode: " + Bot.isDebugMode() +
                "\nOS: " + Bot.getOsString() +
                "\nSystem Time: " + System.currentTimeMillis() +
                "\nSystem Date: " + Bot.getDdMMyyyyHHmmss().format(new Date(System.currentTimeMillis()));
        respond(response);

        return true;
    }
}
