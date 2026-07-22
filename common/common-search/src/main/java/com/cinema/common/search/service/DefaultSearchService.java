package com.cinema.common.search.service;

import java.util.Objects;
import java.util.Optional;

import com.cinema.common.search.client.SearchClient;
import com.cinema.common.search.document.SearchDocument;
import com.cinema.common.search.model.SearchPage;
import com.cinema.common.search.model.SearchQuery;

public class DefaultSearchService
        implements SearchService {

    private final SearchClient searchClient;

    public DefaultSearchService(
            SearchClient searchClient) {

        this.searchClient = searchClient;

    }

    @Override
    public <T> SearchPage<T> search(
            Class<T> documentClass,
            SearchQuery query) {

        Objects.requireNonNull(
                documentClass,
                "Document class must not be null");

        Objects.requireNonNull(
                query,
                "Search query must not be null");

        return searchClient.search(
                documentClass,
                query);

    }

    @Override
    public <T extends SearchDocument> T index(
            T document) {

        Objects.requireNonNull(
                document,
                "Search document must not be null");

        if (document.getId() == null
                || document.getId().isBlank()) {

            throw new IllegalArgumentException(
                    "Search document id must not be blank");

        }

        return searchClient.index(
                document);

    }

    @Override
    public <T extends SearchDocument> Iterable<T> indexAll(
            Iterable<T> documents) {

        Objects.requireNonNull(
                documents,
                "Search documents must not be null");

        return searchClient.indexAll(
                documents);

    }

    @Override
    public <T> Optional<T> findById(
            Class<T> documentClass,
            String id) {

        validateDocumentIdentity(
                documentClass,
                id);

        return searchClient.findById(
                documentClass,
                id);

    }

    @Override
    public <T> boolean exists(
            Class<T> documentClass,
            String id) {

        validateDocumentIdentity(
                documentClass,
                id);

        return searchClient.exists(
                documentClass,
                id);

    }

    @Override
    public <T> void delete(
            Class<T> documentClass,
            String id) {

        validateDocumentIdentity(
                documentClass,
                id);

        searchClient.delete(
                documentClass,
                id);

    }

    private void validateDocumentIdentity(
            Class<?> documentClass,
            String id) {

        Objects.requireNonNull(
                documentClass,
                "Document class must not be null");

        if (id == null || id.isBlank()) {

            throw new IllegalArgumentException(
                    "Search document id must not be blank");

        }

    }

}
