package it.hydr4.lifes.command.suggest;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Names of the players currently online. Written on the main thread by the join and quit
 * listeners, read by suggestion providers, which Paper runs off the main thread.
 */
public final class PlayerNameIndex {
    private final ConcurrentSkipListSet<String> names = new ConcurrentSkipListSet<>(String.CASE_INSENSITIVE_ORDER);

    /** Records a player as online. Blank names are ignored. */
    public void online(String name) {
        if (name != null && !name.isBlank()) {
            names.add(name);
        }
    }

    /** Records a player as no longer online. */
    public void offline(String name) {
        if (name != null) {
            names.remove(name);
        }
    }

    public boolean isOnline(String name) {
        return name != null && names.contains(name);
    }

    /** A snapshot of the online names, sorted case-insensitively. */
    public Collection<String> onlineNames() {
        return List.copyOf(names);
    }

    /** Replaces the whole index, used when the plugin enables with players already connected. */
    public void replaceAll(Collection<String> current) {
        Objects.requireNonNull(current, "current");
        names.clear();
        for (var name : current) {
            online(name);
        }
    }

    public int size() {
        return names.size();
    }
}
