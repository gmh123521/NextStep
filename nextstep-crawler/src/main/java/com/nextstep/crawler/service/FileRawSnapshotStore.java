package com.nextstep.crawler.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;

@Component
public class FileRawSnapshotStore implements RawSnapshotStore {

    private static final Pattern SAFE_PART = Pattern.compile("[A-Za-z0-9_-]+");
    private final Path root;

    @Autowired
    public FileRawSnapshotStore(@Value("${nextstep.crawler.snapshot-dir:data/crawler-raw}") String rootDir) {
        this(Path.of(rootDir));
    }

    public FileRawSnapshotStore(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public String save(String sourceCode, int dataYear, String contentHash, byte[] content) {
        validatePart(sourceCode, "数据源编码");
        validatePart(contentHash, "内容哈希");
        if (dataYear < 2000 || dataYear > 2100) throw new IllegalArgumentException("数据年份非法");
        if (content == null) throw new IllegalArgumentException("快照内容不能为空");

        Path directory = root.resolve(sourceCode).resolve(String.valueOf(dataYear)).normalize();
        Path target = directory.resolve(contentHash + ".json").normalize();
        if (!target.startsWith(root)) throw new IllegalArgumentException("快照路径非法");
        try {
            Files.createDirectories(directory);
            Path temp = Files.createTempFile(directory, ".snapshot-", ".tmp");
            try {
                Files.write(temp, content);
                try {
                    Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temp);
            }
            return root.relativize(target).toString().replace('\\', '/');
        } catch (IOException e) {
            throw new IllegalStateException("保存原始快照失败", e);
        }
    }

    private void validatePart(String value, String name) {
        if (value == null || !SAFE_PART.matcher(value).matches()) {
            throw new IllegalArgumentException(name + "包含非法字符");
        }
    }
}
