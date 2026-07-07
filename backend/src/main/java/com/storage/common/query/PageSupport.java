package com.storage.common.query;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.storage.common.dto.PageResult;

import java.util.List;

public final class PageSupport {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;

    private PageSupport() {
    }

    public static PageSpec normalize(Integer page, Integer pageSize) {
        int current = page == null || page < 1 ? DEFAULT_PAGE : page;
        int size = pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : pageSize;
        return new PageSpec(current, size);
    }

    public static <T> Page<T> page(Integer page, Integer pageSize) {
        PageSpec spec = normalize(page, pageSize);
        return new Page<>(spec.page(), spec.pageSize());
    }

    public static <T> PageResult<T> result(IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public static <T> PageResult<T> result(IPage<?> page, List<T> records) {
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public static <T> PageResult<T> empty(PageSpec pageSpec) {
        return new PageResult<>(List.of(), 0L, (long) pageSpec.page(), (long) pageSpec.pageSize());
    }

    public record PageSpec(int page, int pageSize) {
    }
}
