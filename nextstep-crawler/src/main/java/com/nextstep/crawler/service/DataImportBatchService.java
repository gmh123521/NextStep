package com.nextstep.crawler.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nextstep.common.core.PageResult;
import com.nextstep.common.exception.BizException;
import com.nextstep.crawler.entity.DataImportBatch;
import com.nextstep.crawler.entity.DataRawRecord;
import com.nextstep.crawler.mapper.DataImportBatchMapper;
import com.nextstep.crawler.mapper.DataRawRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class DataImportBatchService {

    private static final int MIN_YEAR = 2000;
    private static final int MAX_YEAR = 2100;
    private final DataImportBatchMapper mapper;
    private final DataRawRecordMapper rawRecordMapper;

    public DataImportBatchService(DataImportBatchMapper mapper) {
        this(mapper, null);
    }

    @Autowired
    public DataImportBatchService(DataImportBatchMapper mapper, DataRawRecordMapper rawRecordMapper) {
        this.mapper = mapper;
        this.rawRecordMapper = rawRecordMapper;
    }

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

    public void attachSnapshot(Long id, String sourceUrl, String snapshotPath) {
        DataImportBatch batch = require(id);
        batch.setSourceUrl(sourceUrl);
        batch.setSnapshotPath(snapshotPath);
        mapper.updateById(batch);
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

    public PageResult<DataImportBatch> page(int pageNum, int pageSize, String sourceCode, String status, Integer dataYear) {
        validatePage(pageNum, pageSize);
        LambdaQueryWrapper<DataImportBatch> query = new LambdaQueryWrapper<DataImportBatch>()
                .eq(sourceCode != null && !sourceCode.isBlank(), DataImportBatch::getSourceCode, normalizeOptional(sourceCode))
                .eq(status != null && !status.isBlank(), DataImportBatch::getStatus, normalizeOptional(status).toUpperCase())
                .eq(dataYear != null, DataImportBatch::getDataYear, dataYear)
                .orderByDesc(DataImportBatch::getId);
        Page<DataImportBatch> page = mapper.selectPage(Page.of(pageNum, pageSize), query);
        return PageResult.of(page.getTotal(), pageNum, pageSize, page.getRecords());
    }

    public DataImportBatch detail(Long id) {
        return require(id);
    }

    public PageResult<DataRawRecord> errors(Long batchId, int pageNum, int pageSize) {
        validatePage(pageNum, pageSize);
        require(batchId);
        if (rawRecordMapper == null) throw new BizException("原始记录查询未配置");
        LambdaQueryWrapper<DataRawRecord> query = new LambdaQueryWrapper<DataRawRecord>()
                .eq(DataRawRecord::getBatchId, batchId)
                .eq(DataRawRecord::getParseStatus, "FAILED")
                .orderByAsc(DataRawRecord::getRecordNo);
        Page<DataRawRecord> page = rawRecordMapper.selectPage(Page.of(pageNum, pageSize), query);
        return PageResult.of(page.getTotal(), pageNum, pageSize, page.getRecords());
    }

    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id) {
        DataImportBatch batch = require(id);
        requireStatus(batch, "SUCCEEDED", "只有已成功解析的批次才能审核通过");
        batch.setStatus("APPROVED");
        mapper.updateById(batch);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, String reason) {
        DataImportBatch batch = require(id);
        if (!java.util.Set.of("SUCCEEDED", "APPROVED").contains(batch.getStatus())) {
            throw new BizException("只有待审核或已审核批次才能驳回");
        }
        batch.setStatus("REJECTED");
        batch.setErrorMessage(truncate(reason));
        mapper.updateById(batch);
    }

    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        DataImportBatch batch = require(id);
        requireStatus(batch, "APPROVED", "只有审核通过的批次才能发布");
        batch.setStatus("PUBLISHED");
        batch.setPublishedAt(LocalDateTime.now());
        mapper.updateById(batch);
    }

    @Transactional(rollbackFor = Exception.class)
    public void rollback(Long id, String reason) {
        DataImportBatch batch = require(id);
        requireStatus(batch, "PUBLISHED", "只有已发布的批次才能回滚");
        batch.setStatus("ROLLED_BACK");
        batch.setErrorMessage(truncate(reason));
        mapper.updateById(batch);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reparse(Long id) {
        DataImportBatch batch = require(id);
        if (batch.getSnapshotPath() == null || batch.getSnapshotPath().isBlank()) {
            throw new BizException("该批次没有可重放的原始快照");
        }
        if (!java.util.Set.of("FAILED", "REJECTED", "ROLLED_BACK").contains(batch.getStatus())) {
            throw new BizException("当前批次状态不允许重新解析");
        }
        batch.setStatus("PENDING");
        batch.setTotalCount(0);
        batch.setSuccessCount(0);
        batch.setSkippedCount(0);
        batch.setFailedCount(0);
        batch.setErrorMessage(null);
        batch.setStartedAt(null);
        batch.setFinishedAt(null);
        batch.setPublishedAt(null);
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

    private String normalizeOptional(String value) {
        return value == null ? null : value.trim();
    }

    private void validatePage(int pageNum, int pageSize) {
        if (pageNum < 1 || pageSize < 1 || pageSize > 200) {
            throw new BizException("分页参数非法：页码必须大于 0，页大小为 1-200");
        }
    }

    private void requireStatus(DataImportBatch batch, String expected, String message) {
        if (!expected.equals(batch.getStatus())) throw new BizException(message);
    }

    private String truncate(String message) {
        if (message == null || message.isBlank()) return "未知错误";
        String value = message.trim();
        return value.length() > 500 ? value.substring(0, 500) : value;
    }
}
