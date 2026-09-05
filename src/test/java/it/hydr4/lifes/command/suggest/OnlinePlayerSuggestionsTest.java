package it.hydr4.lifes.command.suggest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.hydr4.lifes.core.AccountDirectory;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * The provider runs on Paper's suggestion threads, so these tests also assert the contract that
 * matters there: no Bukkit access, only the index and the account directory.
 */
class OnlinePlayerSuggestionsTest {
    private final PlayerNameIndex online = new PlayerNameIndex();
    private final AccountDirectory accounts = new AccountDirectory();
    private final OnlinePlayerSuggestions provider = new OnlinePlayerSuggestions(online, accounts);

    @Test
    void onlinePlayersComeBeforeKnownOfflineAccounts() {
        accounts.create(UUID.randomUUID(), "Zara", 3);
        accounts.create(UUID.randomUUID(), "Adam", 3);
        online.online("Adam");
        assertEquals(List.of("Adam", "Zara"), provider.suggest(null, "lives check ", ""));
    }

    @Test
    void prefixFilteringIgnoresCase() {
        online.online("Hydr4");
        online.online("Hazel");
        assertEquals(List.of("Hazel", "Hydr4"), provider.suggest(null, "lives check h", "h"));
    }

    @Test
    void offlineAccountsAreSuggestedSoAdminsCanFixThem() {
        accounts.create(UUID.randomUUID(), "Ghost", 0);
        assertEquals(List.of("Ghost"), provider.suggest(null, "lives check G", "G"));
    }

    @Test
    void anAccountThatIsOnlineIsNotListedTwice() {
        accounts.create(UUID.randomUUID(), "Hydr4", 3);
        online.online("Hydr4");
        assertEquals(List.of("Hydr4"), provider.suggest(null, "lives check ", ""));
    }

    @Test
    void responsesAreCapped() {
        for (var index = 0; index < 200; index++) {
            online.online("Player" + index);
        }
        assertEquals(OnlinePlayerSuggestions.LIMIT, provider.suggest(null, "lives check ", "").size());
    }

    @Test
    void aNullPrefixBehavesLikeAnEmptyOne() {
        online.online("Hydr4");
        assertEquals(List.of("Hydr4"), provider.suggest(null, "lives check ", null));
    }

    @Test
    void indexIgnoresBlankAndMissingNames() {
        online.online(null);
        online.online("  ");
        assertEquals(0, online.size());
    }

    @Test
    void replaceAllSwapsTheWholeIndex() {
        online.online("Stale");
        online.replaceAll(List.of("Fresh", "Second"));
        online.offline("Fresh");
        assertEquals(List.of("Second"), provider.suggest(null, "lives check ", ""));
    }
}
