package com.nextstep.data.gov.service;

import com.nextstep.data.gov.mapper.GovPostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GovPostService {

    private final GovPostMapper govPostMapper;

    public List<Map<String,Object>> search(Integer year, String examType, String province, String keyword) {
        return govPostMapper.search(year, examType, province, keyword);
    }

    public Map<String,Object> detail(Long postId) {
        return govPostMapper.selectDetail(postId);
    }
}
