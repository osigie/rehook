package com.osigie.rehook.dto.response;

import java.util.List;

public record PageResponseDto<T>(
        long totalRecords,
        int pageNo,
        int pageSize,
        List<T> content
) {
}
