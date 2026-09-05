package it.hydr4.lifes.persistence;

import it.hydr4.lifes.api.LivesAccount;

import java.util.Map;
import java.util.UUID;

/** Storage boundary for accounts: load everything, save everything or one. */
public interface LivesRepository {
    Map<UUID, LivesAccount> loadAll();

    void saveAll(Map<UUID, LivesAccount> accounts);

    void saveOne(LivesAccount account);
}
