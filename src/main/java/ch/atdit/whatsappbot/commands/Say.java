package ch.atdit.whatsappbot.commands;

import static ch.atdit.whatsappbot.utility.Common.*;

public class Say extends Command {
    public Say() {
        name = "say";
        permissionLevel = 1;
    }

    @Override
    public boolean run(String chat, String author, String content) {
        if (!super.run(chat, author, content)) return false; // or return only false if no feedback should be provided

        if (args.size() < 2) {
            return false;
        }

        String chatToSend = args.get(0);
        String messageToSend = args.get(1);

        print("Sending <" + messageToSend + "> to <" + chatToSend + ">");

        send(chatToSend, messageToSend);

        return true;
    }
}
