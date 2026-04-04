package com.ring.cloud.core.frame;

import lombok.Data;
import lombok.Builder;
import java.util.Collections;
import java.util.List;

//// 1. 完整写法
//PageResult<SourceIpDomain> result = PageResult.<SourceIpDomain>builder()
//        .total(total)
//        .pageNum(pageNum)
//        .pageSize(pageSize)
//        .list(list)
//        .build();
//
//// 2. 极简写法（缺省值自动补全）
//        PageResult<SourceIpDomain> emptyResult = PageResult.<SourceIpDomain>builder()
//        .build();
// → total=0, pageNum=1, pageSize=10, list=[]
@Data
@Builder
public class PageResult<T> {
    // 总条数（默认 0）
    @Builder.Default
    private long total = 0L;

    // 当前页码（默认 1）
    @Builder.Default
    private int pageNum = 1;

    // 每页条数（默认 10）
    @Builder.Default
    private int pageSize = 10;

    // 数据列表（默认空集合）
    @Builder.Default
    private List<T> list = Collections.emptyList();

    // 计算总页数
    public int getPages() {
        if (pageSize == 0) return 0;
        return (int) Math.ceil((double) total / pageSize);
    }

    // 👇 静态工厂方法：快速构建空分页结果
    public static <T> PageResult<T> empty() {
        return PageResult.<T>builder().build();
    }

    // 👇 静态工厂方法：快速构建带数据的分页（最常用）
    public static <T> PageResult<T> of(long total, int pageNum, int pageSize, List<T> list) {
        return PageResult.<T>builder()
                .total(total)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .list(list)
                .build();
    }
}
