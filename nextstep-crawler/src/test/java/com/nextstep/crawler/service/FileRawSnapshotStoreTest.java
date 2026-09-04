package com.nextstep.crawler.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileRawSnapshotStoreTest {

    @Test
    void writesSnapshotAndReturnsRelativePath() throws Exception {
        Path root = Files.createTempDirectory("nextstep-raw-");
        FileRawSnapshotStore store = new FileRawSnapshotStore(root);

        String relative = store.save("KAOYAN_CATALOG", 2026, "sha256-abc", "{}".getBytes(StandardCharsets.UTF_8));

        Path saved = root.resolve(relative);
        assertTrue(Files.exists(saved));
        assertEquals("{}", Files.readString(saved));
        assertTrue(relative.replace('\\', '/').matches("KAOYAN_CATALOG/2026/sha256-abc\\.json"));
    }

    @Test
    void rejectsUnsafePathParts() throws Exception {
        Path root = Files.createTempDirectory("nextstep-raw-");
        FileRawSnapshotStore store = new FileRawSnapshotStore(root);

        assertThrows(RuntimeException.class, () -> store.save("../escape", 2026, "hash", new byte[0]));
        assertThrows(RuntimeException.class, () -> store.save("KAOYAN", 2026, "../escape", new byte[0]));
    }
}
