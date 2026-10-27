package com.example.nowme;

import java.util.List;

/** Spring Data page as serialized by the backend: {"content": [...], "page": {...}}. */
public class PageResponse<T> {
    public List<T> content;
    public PageMeta page;

    public boolean hasNext() {
        return page != null && page.number + 1 < page.totalPages;
    }

    public static class PageMeta {
        public int size;
        public int number;
        public long totalElements;
        public int totalPages;
    }
}
