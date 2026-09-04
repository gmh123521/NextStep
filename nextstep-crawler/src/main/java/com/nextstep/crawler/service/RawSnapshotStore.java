package com.nextstep.crawler.service;

public interface RawSnapshotStore {

    String save(String sourceCode, int dataYear, String contentHash, byte[] content);
}
