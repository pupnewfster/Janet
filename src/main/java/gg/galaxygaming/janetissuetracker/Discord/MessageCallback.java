package gg.galaxygaming.janetissuetracker.Discord;

import com.google.common.util.concurrent.FutureCallback;
import de.btobastian.javacord.entities.message.Message;
import gg.galaxygaming.janetissuetracker.Janet;

public class MessageCallback implements FutureCallback<Message> {
    @Override
    public void onSuccess(Message message) {
        if (Janet.DEBUG)
            System.out.println("[DEBUG] Message sent successfully");
    }

    @Override
    public void onFailure(Throwable t) {
        System.out.println("[ERROR] Failed to send message.");
        t.printStackTrace();
    }
}