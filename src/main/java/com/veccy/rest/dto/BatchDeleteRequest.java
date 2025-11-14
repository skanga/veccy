package com.veccy.rest.dto;

import java.util.List;

/**
 * Request DTO for batch delete operation.
 */
public class BatchDeleteRequest {
    private List<String> ids;

    public List<String> getIds() {
        return ids;
    }

    public void setIds(List<String> ids) {
        this.ids = ids;
    }
}
