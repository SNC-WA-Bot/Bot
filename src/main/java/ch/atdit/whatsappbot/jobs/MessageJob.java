package ch.atdit.whatsappbot.jobs;

import ch.atdit.whatsappbot.Bot;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static ch.atdit.whatsappbot.utility.Common.*;

public class MessageJob extends Job {

    private final WebDriver driver;
    private final String chat;
    private final String message;

    private boolean cleanMessage = true;

    public MessageJob(String chat, String message) {
        name = "Message Job";

        this.driver = Bot.getDriver();
        this.chat = chat;
        this.message = message;
    }

    public MessageJob(String chat, String message, boolean cleanMessage) {
        this(chat, message);
        this.cleanMessage = cleanMessage;
    }

    @Override
    public void start() {
        int counter = 0;
        while (!Bot.isLoggedIn()) {
            if (counter == 1000 * 1000 * 1000) counter = 0;
            counter += 10;

            if (counter % 1000 == 0) {
                print("Not logged in.");
            }

            delay(10);
        }

        print("[MJ] Finding chatElement");
        waitOff();
        WebElement chatElement;
        try {
            chatElement = driver.findElement(By.xpath("//span[@title = '" + chat + "']"));
        } catch (NoSuchElementException ignored) {
            waitOn();
            return;
        }
        chatElement.click();

        delay(100);

        print("[MJ] Finding inputElement");
        WebElement inputElement = driver.findElement(By.className("_3uMse"));
        inputElement.sendKeys(cleanMessage ? cleanMessage(message) : message);

        delay(100);

        print("[MJ] Finding sendButton");
        WebElement sendButton = driver.findElement(By.className("_1U1xa"));
        sendButton.click();

        waitOn();
        delay(100);
    }
}
