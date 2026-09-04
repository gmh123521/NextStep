package com.nextstep.crawler.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nextstep.common.exception.BizException;
import com.nextstep.crawler.entity.DataImportBatch;
import com.nextstep.crawler.mapper.DataImportBatchMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DataImportBatchService {

    private static final int MIN_YEAR = 2000;
    private static final int MAX_YEAR = 2100;
    private final DataImportBatchMapper mapper;

    public DataImportBatch createOrReuse(String sourceCode, int dataYear, String contentHash, String parserVersion) {
        String source = normalize(sourceCode, "数据源编码不能为空");
        String hash = normalize(contentHash, "数据内容哈希不能为空");
        String parser = normalize(parserVersion, "解析器版本不能为空");
        if (dataYear < MIN_YEAR || dataYear > MAX_YEAR) {
            throw new BizException("数据年份必须处于 " + MIN_YEAR + "-" + MAX_YEAR + " 之间");
        }

        LambdaQueryWrapper<DataImportBatch> query = new LambdaQueryWrapper<DataImportBatch>()
                .eq(DataImportBatch::getSourceCode, source)
                .eq(DataImportBatch::getDataYear, dataYear)
                .eq(DataImportBatch::getContentHash, hash)
                .last("LIMIT 1");
        DataImportBatch existing = mapper.selectOne(query);
        if (existing != null) return existing;

        DataImportBatch batch = new DataImportBatch();
        batch.setSourceCode(source);
        batch.setDataYear(dataYear);
        batch.setContentHash(hash);
        batch.setParserVersion(parser);
        batch.setStatus("PENDING");
        batch.setTotalCount(0);
        batch.setSuccessCount(0);
        batch.setSkippedCount(0);
        batch.setFailedCount(0);
        mapper.insert(batch);
        return batch;
    }

    public void markRunning(Long id) {
        DataImportBatch batch = require(id);
        batch.setStatus("RUNNING");
        batch.setStartedAt(LocalDateTime.now());
        mapper.updateById(batch);
    }

    public void markSucceeded(Long id, int total, int success, int skipped, int failed) {
        validateCounts(total, success, skipped, failed);
        DataImportBatch batch = require(id);
        batch.setStatus("SUCCEEDED");
        batch.setTotalCount(total);
        batch.setSuccessCount(success);
        batch.setSkippedCount(skipped);
        batch.setFailedCount(failed);
        batch.setFinishedAt(LocalDateTime.now());
        batch.setErrorMessage(null);
        mapper.updateById(batch);
    }

    public void markFailed(Long id, String message) {
        DataImportBatch batch = require(id);
        batch.setStatus("FAILED");
        batch.setErrorMessage(truncate(message));
        batch.setFinishedAt(LocalDateTime.now());
        mapper.updateById(batch);
    }

    private DataImportBatch require(Long id) {
        if (id == null || id < 1) throw new BizException("导入批次 ID 非法");
        DataImportBatch batch = mapper.selectById(id);
        if (batch == null) throw new BizException("导入批次不存在：" + id);
        return batch;
    }

    private void validateCounts(int total, int success, int skipped, int failed) {
        if (total < 0 || success < 0 || skipped < 0 || failed < 0 || success + skipped + failed > total) {
            throw new BizException("导入批次统计数量非法");
        }
    }

    private String normalize(String value, String message) {
        if (value == null || value.isBlank()) throw new BizException(message);
        return value.trim();
    }

    private String truncate(String message) {
        if (message == null || message.isBlank()) return "未知错误";
        String value = message.trim();
        return value.length() > 500 ? value.substring(0, 500) : value;
    }
}
