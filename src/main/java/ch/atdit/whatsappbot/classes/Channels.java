package ch.atdit.whatsappbot.classes;

import java.util.List;

public class Channels {
    public List<String> whitelist;
    public List<String> blacklist;

    public Channels(List<String> whitelist, List<String> blacklist) {
        this.whitelist = whitelist;
        this.blacklist = blacklist;
    }
}
