package it.hydr4.lifes.paper;

import it.hydr4.lifes.api.LifeChangeReason;
import it.hydr4.lifes.api.LifeChange;
import it.hydr4.lifes.api.LivesListener;
import it.hydr4.lifes.text.MessageKey;
import it.hydr4.lifes.text.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** Notifies an online player when an administrator changes their lives. */
public final class AdminChangeNotifier implements LivesListener {
    private final java.util.function.Supplier<Messages> messages;

    public AdminChangeNotifier(java.util.function.Supplier<Messages> messages) {
        this.messages = java.util.Objects.requireNonNull(messages, "messages");
    }

    @Override
    public void onLifeChange(LifeChange change) {
        if (change.reason() == LifeChangeReason.DEATH) {
            return;
        }
        Player player = Bukkit.getPlayer(change.after().uuid());
        if (player != null) {
            player.sendMessage(messages.get().render(MessageKey.LIVES_TARGET_NOTIFY, "lives", change.after().lives()));
        }
    }
}
