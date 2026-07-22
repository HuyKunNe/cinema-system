package com.cinema.common.search.client;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;

import com.cinema.common.search.document.SearchDocument;
import com.cinema.common.search.exception.SearchException;
import com.cinema.common.search.model.SearchDirection;
import com.cinema.common.search.model.SearchHit;
import com.cinema.common.search.model.SearchPage;
import com.cinema.common.search.model.SearchQuery;
import com.cinema.common.search.model.SearchSort;

import co.elastic.clients.elasticsearch._types.SortOrder;

public class ElasticsearchSearchClient
        implements SearchClient {

    private final ElasticsearchOperations operations;

    public ElasticsearchSearchClient(
            ElasticsearchOperations operations) {

        this.operations = operations;

    }

    @Override
    public <T> SearchPage<T> search(
            Class<T> documentClass,
            SearchQuery query) {

        try {

            NativeQuery nativeQuery = buildQuery(
                    query);

            SearchHits<T> searchHits = operations.search(
                    nativeQuery,
                    documentClass);

            List<SearchHit<T>> hits = searchHits
                    .getSearchHits()
                    .stream()
                    .map(hit -> new SearchHit<>(
                            hit.getContent(),
                            hit.getScore()))
                    .toList();

            long totalElements = searchHits
                    .getTotalHits();

            int totalPages = calculateTotalPages(
                    totalElements,
                    query.size());

            return new SearchPage<>(
                    hits,
                    totalElements,
                    totalPages,
                    query.page(),
                    query.size());

        } catch (Exception exception) {

            throw new SearchException(
                    "Failed to search documents of type "
                            + documentClass.getName(),
                    exception);

        }

    }

    @Override
    public <T extends SearchDocument> T index(
            T document) {

        try {

            return operations.save(
                    document);

        } catch (Exception exception) {

            throw new SearchException(
                    "Failed to index document with id "
                            + document.getId(),
                    exception);

        }

    }

    @Override
    public <T extends SearchDocument> Iterable<T> indexAll(
            Iterable<T> documents) {

        try {

            return operations.save(
                    documents);

        } catch (Exception exception) {

            throw new SearchException(
                    "Failed to index documents",
                    exception);

        }

    }

    @Override
    public <T> Optional<T> findById(
            Class<T> documentClass,
            String id) {

        try {

            return Optional.ofNullable(
                    operations.get(
                            id,
                            documentClass));

        } catch (Exception exception) {

            throw new SearchException(
                    "Failed to find document with id "
                            + id,
                    exception);

        }

    }

    @Override
    public <T> boolean exists(
            Class<T> documentClass,
            String id) {

        try {

            return operations.exists(
                    id,
                    documentClass);

        } catch (Exception exception) {

            throw new SearchException(
                    "Failed to check document existence with id "
                            + id,
                    exception);

        }

    }

    @Override
    public <T> void delete(
            Class<T> documentClass,
            String id) {

        try {

            operations.delete(
                    id,
                    documentClass);

        } catch (Exception exception) {

            throw new SearchException(
                    "Failed to delete document with id "
                            + id,
                    exception);

        }

    }

    private NativeQuery buildQuery(
            SearchQuery query) {

        NativeQueryBuilder builder = NativeQuery.builder()
                .withPageable(
                        PageRequest.of(
                                query.page(),
                                query.size()));

        if (query.hasKeyword()
                && query.hasFields()) {

            builder.withQuery(
                    elasticsearchQuery -> elasticsearchQuery
                            .multiMatch(
                                    multiMatch -> multiMatch
                                            .query(
                                                    query.keyword())
                                            .fields(
                                                    query.fields())));

        } else {

            builder.withQuery(
                    elasticsearchQuery -> elasticsearchQuery
                            .matchAll(
                                    matchAll -> matchAll));

        }

        for (SearchSort sort : query.sorts()) {

            builder.withSort(
                    elasticsearchSort -> elasticsearchSort
                            .field(
                                    fieldSort -> fieldSort
                                            .field(
                                                    sort.field())
                                            .order(
                                                    resolveSortOrder(
                                                            sort.direction()))));

        }

        return builder.build();

    }

    private SortOrder resolveSortOrder(
            SearchDirection direction) {

        if (direction == SearchDirection.DESC) {

            return SortOrder.Desc;

        }

        return SortOrder.Asc;

    }

    private int calculateTotalPages(
            long totalElements,
            int size) {

        if (totalElements == 0) {

            return 0;

        }

        return (int) Math.ceil(
                (double) totalElements / size);

    }

}
