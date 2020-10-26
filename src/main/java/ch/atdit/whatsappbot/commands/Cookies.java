package ch.atdit.whatsappbot.commands;

import org.openqa.selenium.Cookie;

import java.util.Set;

public class Cookies extends Command {
    public Cookies() {
        name = "cookies";
        permissionLevel = 1;
    }

    @Override
    public boolean run(String chat, String author, String content) {
        if (!super.run(chat, author, content)) return false;

        Set<Cookie> cookies = driver.manage().getCookies();

        for (Cookie cookie : cookies) {
            cookie.toJson();
        }

        return true;
    }
}
