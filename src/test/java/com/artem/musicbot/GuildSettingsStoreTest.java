package com.artem.musicbot;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GuildSettingsStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void createsDefaultsForMissingGuild() {
        GuildSettingsStore store = new GuildSettingsStore(tempDir.resolve("guild-settings.db"), "!", "en");

        GuildSettings defaults = store.get(42L);

        assertEquals(42L, defaults.guildId());
        assertEquals("!", defaults.prefix());
        assertEquals("en", defaults.language());
        assertEquals(0L, defaults.commandChannelId());
        assertEquals(0L, defaults.blockedRoleId());
        assertTrue(defaults.autoplay());
    }

    @Test
    void persistsAndReadsExtendedSettings() {
        GuildSettingsStore store = new GuildSettingsStore(tempDir.resolve("guild-settings.db"), "!", "en");
        GuildSettings input = new GuildSettings(77L, "?", "ru", 111L, 135, true, 222L, 333L);

        store.upsert(input);
        GuildSettings loaded = store.get(77L);

        assertEquals("?", loaded.prefix());
        assertEquals("ru", loaded.language());
        assertEquals(111L, loaded.djRoleId());
        assertEquals(135, loaded.defaultVolume());
        assertTrue(loaded.autoplay());
        assertEquals(222L, loaded.commandChannelId());
        assertEquals(333L, loaded.blockedRoleId());
    }
}
