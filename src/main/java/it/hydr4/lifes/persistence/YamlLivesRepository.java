package it.hydr4.lifes.persistence;

import it.hydr4.lifes.ConfigException;
import it.hydr4.lifes.api.LivesAccount;
import it.hydr4.lifes.util.Uuids;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** YAML storage: atomic writes, corrupt files preserved before aborting. */
public final class YamlLivesRepository implements LivesRepository {
    private static final int FORMAT_VERSION = 1;

    private final Path file;
    private final Yaml yaml;

    public YamlLivesRepository(Path file) {
        this.file = file;
        var dumper = new DumperOptions();
        dumper.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumper.setIndent(2);
        dumper.setPrettyFlow(true);
        var representer = new Representer(dumper);
        representer.getPropertyUtils().setSkipMissingProperties(true);
        this.yaml = new Yaml(new SafeConstructor(new LoaderOptions()), representer, dumper);
    }

    @Override
    public Map<UUID, LivesAccount> loadAll() {
        if (!Files.isRegularFile(file)) {
            return Map.of();
        }
        Object document = read();
        if (document == null) {
            return Map.of();
        }
        if (!(document instanceof Map<?, ?> root)) {
            throw corrupt("root is not a mapping");
        }
        var version = root.get("version");
        if (!(version instanceof Integer parsedVersion)) {
            throw corrupt("version is missing or is not an integer");
        }
        if (parsedVersion != FORMAT_VERSION) {
            throw new ConfigException(file.toString(), "unsupported saves version " + parsedVersion);
        }
        var users = root.get("users");
        if (!(users instanceof Map<?, ?> usersMap)) {
            if (users == null) {
                return Map.of();
            }
            throw corrupt("users is not a mapping");
        }
        var loaded = new LinkedHashMap<UUID, LivesAccount>();
        for (var entry : usersMap.entrySet()) {
            var rawKey = entry.getKey();
            var id = rawKey instanceof String key ? Uuids.parseOrNull(key) : null;
            if (id == null) {
                throw corrupt("user key '" + rawKey + "' is not a valid UUID");
            }
            if (!(entry.getValue() instanceof Map<?, ?> fields)) {
                throw corrupt("user " + id + " is not a mapping");
            }
            var account = AccountCodec.decode(id, fields);
            if (account == null) {
                throw corrupt("user " + id + " has missing or invalid fields");
            }
            loaded.put(id, account);
        }
        return loaded;
    }

    @Override
    public void saveAll(Map<UUID, LivesAccount> accounts) {
        var users = accounts.entrySet().stream()
            .collect(Collectors.toMap(
                entry -> entry.getKey().toString(),
                entry -> AccountCodec.encode(entry.getValue()),
                (left, right) -> left,
                LinkedHashMap::new
            ));
        var root = new LinkedHashMap<String, Object>();
        root.put("version", FORMAT_VERSION);
        root.put("users", users);
        var document = yaml.dump(root);
        writeAtomically(document);
    }

    @Override
    public void saveOne(LivesAccount account) {
        var current = new LinkedHashMap<>(loadAll());
        current.put(account.uuid(), account);
        saveAll(current);
    }

    private Object read() {
        try {
            return yaml.load(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException exception) {
            preserveCorrupt();
            throw new ConfigException(file.toString(), "saves file is corrupt (" + exception.getMessage() + "); preserved a copy and aborted startup");
        }
    }

    private void preserveCorrupt() {
        try {
            var kept = file.resolveSibling(file.getFileName() + ".broken-" + System.currentTimeMillis());
            Files.copy(file, kept, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // The startup failure below still names the corrupt file.
        }
    }

    /**
     * Preserves the file and builds the failure to throw. A damaged save is never loaded partly:
     * an entry dropped here would be erased for good by the next full snapshot write.
     */
    private ConfigException corrupt(String detail) {
        preserveCorrupt();
        return new ConfigException(file.toString(), "saves file is corrupt (" + detail + "); preserved a copy and aborted startup");
    }

    private void writeAtomically(String document) {
        var parent = file.toAbsolutePath().getParent();
        var temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(parent);
            Files.writeString(temporary, document, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | UncheckedIOException exception) {
            throw new UncheckedIOException("Failed to write " + file, exception instanceof IOException io ? io : new IOException(exception));
        }
    }
}
