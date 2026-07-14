package com.awb.ged.common.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * <h1>PageResponse</h1>
 * <p>
 * A standard, framework-agnostic wrapper for paginated collections of data in the GED-AWB system.
 * This class isolates the domain and application layers from dependency on Spring's {@code Page}
 * implementation, while providing all necessary pagination metadata.
 * </p>
 * <p>
 * It includes detailed sorting parameters (field and direction) and structural page markers
 * (first, last, empty) to support complete frontend rendering and paging operations.
 * </p>
 *
 * @param <T> the type of element contained in the page
 */
@Value
@Builder
public class PageResponse<T> {

    /** The list of items on the current page */
    List<T> content;

    /** The current page number (0-indexed) */
    int pageNumber;

    /** The maximum number of items requested per page */
    int pageSize;

    /** The total number of elements matching the query across all pages */
    long totalElements;

    /** The total number of pages available */
    int totalPages;

    /** The field on which the items are sorted */
    String sortBy;

    /** The sort direction, typically "ASC" or "DESC" */
    String sortDirection;

    /** True if this is the first page of results */
    boolean first;

    /** True if this is the last page of results */
    boolean last;

    /** True if the page contains no items */
    boolean empty;
}
