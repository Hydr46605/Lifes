package it.hydr4.lifes.death.actions;

import it.hydr4.lifes.ConfigException;
import it.hydr4.lifes.config.DeathActionSpec;
import it.hydr4.lifes.death.ActionContext;
import it.hydr4.lifes.death.LifesAction;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;

/** Plays a fixed sound at the victim when they are online. */
public final class SoundAction implements LifesAction {
    private final Sound sound;
    private final float volume;
    private final float pitch;

    private SoundAction(Sound sound, float volume, float pitch) {
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
    }

    public static SoundAction from(DeathActionSpec spec) {
        var name = spec.requiredString("sound");
        var sound = resolve(name);
        if (sound == null) {
            throw new ConfigException(spec.path("sound"), "unknown sound '" + name + "' (use a sound key such as entity.wither.spawn)");
        }
        return new SoundAction(
            sound,
            (float) spec.optionalDouble("volume", 1.0, 0.0, 64.0),
            (float) spec.optionalDouble("pitch", 1.0, 0.0, 4.0)
        );
    }

    @Override
    public void execute(ActionContext context) {
        var player = context.player().getPlayer();
        if (player != null) {
            player.playSound(player.getLocation(), sound, SoundCategory.MASTER, volume, pitch);
        }
    }

    private static Sound resolve(String raw) {
        var key = NamespacedKey.fromString(raw.toLowerCase(java.util.Locale.ROOT));
        return key == null ? null : Registry.SOUND_EVENT.get(key);
    }
}
