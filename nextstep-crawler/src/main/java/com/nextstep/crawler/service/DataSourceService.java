package com.nextstep.crawler.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nextstep.common.exception.BizException;
import com.nextstep.crawler.entity.DataSource;
import com.nextstep.crawler.mapper.DataSourceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DataSourceService {

    private final DataSourceMapper mapper;

    public DataSourceService(DataSourceMapper mapper) {
        this.mapper = mapper;
    }

    public List<DataSource> list() {
        return mapper.selectList(new LambdaQueryWrapper<DataSource>().orderByAsc(DataSource::getSourceCode));
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, String sourceUrl, Integer enabled, String parserVersion) {
        if (id == null || id < 1) throw new BizException("数据源 ID 非法");
        DataSource source = mapper.selectById(id);
        if (source == null) throw new BizException("数据源不存在：" + id);
        if (sourceUrl != null && !sourceUrl.isBlank()) {
            String url = sourceUrl.trim();
            if (!(url.startsWith("https://") || url.startsWith("http://"))) {
                throw new BizException("数据源地址必须使用 HTTP 或 HTTPS");
            }
            if (url.length() > 512) throw new BizException("数据源地址长度不能超过 512 个字符");
            source.setSourceUrl(url);
        } else if (sourceUrl != null) {
            source.setSourceUrl(null);
        }
        if (enabled != null) {
            if (enabled != 0 && enabled != 1) throw new BizException("启用状态只能是 0 或 1");
            source.setEnabled(enabled);
        }
        if (parserVersion != null) {
            String parser = parserVersion.trim();
            if (parser.isBlank() || parser.length() > 32) throw new BizException("解析器版本不能为空且不能超过 32 个字符");
            source.setParserVersion(parser);
        }
        mapper.updateById(source);
    }
}
