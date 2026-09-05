package it.hydr4.lifes.config;

import it.hydr4.lifes.ConfigException;
import it.hydr4.lifes.text.MessageKey;
import it.hydr4.lifes.text.MessageTemplates;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Parses settings.yml; any deviation fails with the exact config path. */
public final class SettingsParser {
    private static final Set<String> ROOT_KEYS = Set.of("version", "lives", "death", "exhaustion", "persistence", "messages");
    private static final Set<String> LIVES_KEYS = Set.of("default", "maximum");
    private static final Set<String> DEATH_KEYS = Set.of("cost-per-death", "ignored-causes", "actions");
    private static final Set<String> EXHAUSTION_KEYS = Set.of("actions", "on-zero-lives-join");
    private static final Set<String> PERSISTENCE_KEYS = Set.of("save-interval-seconds", "save-off-thread");
    private static final int FORMAT_VERSION = 1;

    private SettingsParser() {
    }

    public static LivesSettings parse(Path file) {
        var filePath = file.toString();
        Map<?, ?> root = load(file);
        expectKeys(root, ROOT_KEYS, filePath);
        requireVersion(root, filePath);

        var lives = section(root, "lives", filePath);
        expectKeys(lives, LIVES_KEYS, path(filePath, "lives"));
        var defaultLives = integer(lives, "default", 1, 100_000, path(filePath, "lives"));
        var maximumLives = integer(lives, "maximum", 1, 100_000, path(filePath, "lives"));
        if (maximumLives < defaultLives) {
            throw new ConfigException(path(filePath, "lives.maximum"), "must be >= lives.default (" + defaultLives + ")");
        }

        var death = section(root, "death", filePath);
        expectKeys(death, DEATH_KEYS, path(filePath, "death"));
        var deathCost = integer(death, "cost-per-death", 0, 1_000, path(filePath, "death"));
        var ignoredCauses = causeNames(death, path(filePath, "death"));
        var deathActions = actions(death, "actions", path(filePath, "death"));

        var exhaustion = section(root, "exhaustion", filePath);
        expectKeys(exhaustion, EXHAUSTION_KEYS, path(filePath, "exhaustion"));
        var exhaustionActions = actions(exhaustion, "actions", path(filePath, "exhaustion"));
        var zeroLivesJoin = optionalEnum(exhaustion, "on-zero-lives-join", ZeroLivesJoin.class, ZeroLivesJoin.REAPPLY, path(filePath, "exhaustion"));

        var persistence = section(root, "persistence", filePath);
        expectKeys(persistence, PERSISTENCE_KEYS, path(filePath, "persistence"));
        var saveInterval = integer(persistence, "save-interval-seconds", 0, 86_400, path(filePath, "persistence"));
        var saveOffThread = bool(persistence, "save-off-thread", path(filePath, "persistence"));

        var messages = messages(root, filePath);
        return new LivesSettings(
            defaultLives,
            maximumLives,
            deathCost,
            ignoredCauses,
            deathActions,
            exhaustionActions,
            zeroLivesJoin,
            saveInterval,
            saveOffThread,
            messages
        );
    }

    private static Map<?, ?> load(Path file) {
        try {
            var options = new LoaderOptions();
            options.setMaxAliasesForCollections(50);
            options.setCodePointLimit(3 * 1024 * 1024);
            var yaml = new Yaml(new SafeConstructor(options));
            var loaded = yaml.load(Files.readString(file));
            if (!(loaded instanceof Map<?, ?> map)) {
                throw new ConfigException(file.toString(), "expected a YAML mapping at the document root");
            }
            return map;
        } catch (IOException | RuntimeException exception) {
            throw new ConfigException(file.toString(), "unreadable: " + exception.getMessage());
        }
    }

    private static void requireVersion(Map<?, ?> root, String filePath) {
        var value = root.get("version");
        if (!(value instanceof Integer version)) {
            throw new ConfigException(path(filePath, "version"), "expected an integer");
        }
        if (version != FORMAT_VERSION) {
            throw new ConfigException(path(filePath, "version"), "unsupported version " + version + "; expected " + FORMAT_VERSION);
        }
    }

    private static int integer(Map<?, ?> section, String key, int minimum, int maximum, String sectionPath) {
        var value = section.get(key);
        if (!(value instanceof Integer number)) {
            throw new ConfigException(path(sectionPath, key), "expected an integer, got " + typeName(value));
        }
        if (number < minimum || number > maximum) {
            throw new ConfigException(path(sectionPath, key), "must be between " + minimum + " and " + maximum);
        }
        return number;
    }

    private static boolean bool(Map<?, ?> section, String key, String sectionPath) {
        var value = section.get(key);
        if (!(value instanceof Boolean flag)) {
            throw new ConfigException(path(sectionPath, key), "expected true or false, got " + typeName(value));
        }
        return flag;
    }

    /**
     * Reads an optional enum key. An absent key takes the documented default; a present key must
     * name one of the constants, so a typo still fails with the exact path.
     */
    private static <E extends Enum<E>> E optionalEnum(Map<?, ?> section, String key, Class<E> type, E fallback, String sectionPath) {
        var value = section.get(key);
        if (value == null) {
            return fallback;
        }
        var options = java.util.Arrays.stream(type.getEnumConstants()).map(Enum::name).sorted().toList();
        if (!(value instanceof String text) || text.isBlank()) {
            throw new ConfigException(path(sectionPath, key), "expected one of " + options + ", got " + typeName(value));
        }
        try {
            return Enum.valueOf(type, text.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ConfigException(path(sectionPath, key), "expected one of " + options + ", got '" + text + "'");
        }
    }

    private static Set<String> causeNames(Map<?, ?> death, String sectionPath) {
        var value = death.get("ignored-causes");
        if (value == null) {
            return Set.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new ConfigException(path(sectionPath, "ignored-causes"), "expected a list of strings");
        }
        var causes = new LinkedHashSet<String>();
        for (var entry : list) {
            if (!(entry instanceof String name)) {
                throw new ConfigException(path(sectionPath, "ignored-causes"), "expected a list of strings, found " + typeName(entry));
            }
            if (!name.matches("[A-Z][A-Z_]*")) {
                throw new ConfigException(path(sectionPath, "ignored-causes"), "invalid damage cause name '" + name + "'");
            }
            causes.add(name);
        }
        return Set.copyOf(causes);
    }

    private static List<DeathActionSpec> actions(Map<?, ?> section, String key, String sectionPath) {
        var value = section.get(key);
        if (!(value instanceof List<?> list)) {
            throw new ConfigException(path(sectionPath, key), "expected a list of actions");
        }
        var basePath = path(sectionPath, key);
        var actions = new ArrayList<DeathActionSpec>(list.size());
        for (var index = 0; index < list.size(); index++) {
            var entry = list.get(index);
            var entryPath = basePath + "[" + index + "]";
            if (!(entry instanceof Map<?, ?> map)) {
                throw new ConfigException(entryPath, "expected an action mapping");
            }
            var type = map.get("type");
            if (!(type instanceof String actionType) || actionType.isBlank()) {
                throw new ConfigException(entryPath + ".type", "expected a non-blank string");
            }
            var options = new LinkedHashMap<String, Object>();
            for (var option : map.entrySet()) {
                if (!(option.getKey() instanceof String optionKey)) {
                    throw new ConfigException(entryPath, "action option keys must be strings");
                }
                if (optionKey.equals("type")) {
                    continue;
                }
                if (!(option.getValue() instanceof String
                    || option.getValue() instanceof Number
                    || option.getValue() instanceof Boolean)) {
                    throw new ConfigException(entryPath + "." + optionKey, "expected a string, number or boolean");
                }
                options.put(optionKey, option.getValue());
            }
            actions.add(new DeathActionSpec(actionType.toUpperCase(Locale.ROOT), options, entryPath));
        }
        return List.copyOf(actions);
    }

    private static MessageTemplates messages(Map<?, ?> root, String filePath) {
        var value = root.get("messages");
        if (!(value instanceof Map<?, ?> map)) {
            throw new ConfigException(path(filePath, "messages"), "expected a mapping of message keys");
        }
        var overrides = new LinkedHashMap<String, String>();
        for (var entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new ConfigException(path(filePath, "messages"), "message keys must be strings");
            }
            if (MessageKey.byKey(key).isEmpty()) {
                throw new ConfigException(path(filePath, "messages") + "." + key, "unknown message key");
            }
            if (!(entry.getValue() instanceof String template)) {
                throw new ConfigException(path(filePath, "messages") + "." + key, "expected a string template");
            }
            overrides.put(key, template);
        }
        return MessageTemplates.withOverrides(overrides);
    }

    private static Map<?, ?> section(Map<?, ?> root, String key, String filePath) {
        var value = root.get(key);
        if (!(value instanceof Map<?, ?> map)) {
            throw new ConfigException(path(filePath, key), "expected a mapping section");
        }
        return map;
    }

    private static void expectKeys(Map<?, ?> section, Set<String> allowed, String sectionPath) {
        for (var key : section.keySet()) {
            if (!(key instanceof String name)) {
                throw new ConfigException(sectionPath, "keys must be strings");
            }
            if (!allowed.contains(name)) {
                throw new ConfigException(sectionPath + "." + name, "unknown key; expected one of " + allowed);
            }
        }
    }

    private static String path(String parent, String child) {
        return parent + "." + child;
    }

    private static String typeName(Object value) {
        return value == null ? "null" : value.getClass().getSimpleName();
    }
}
