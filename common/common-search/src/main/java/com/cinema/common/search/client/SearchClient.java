package com.cinema.common.search.client;

import java.util.Optional;

import com.cinema.common.search.document.SearchDocument;
import com.cinema.common.search.model.SearchPage;
import com.cinema.common.search.model.SearchQuery;

public interface SearchClient {

    <T> SearchPage<T> search(
            Class<T> documentClass,
            SearchQuery query);

    <T extends SearchDocument> T index(
            T document);

    <T extends SearchDocument> Iterable<T> indexAll(
            Iterable<T> documents);

    <T> Optional<T> findById(
            Class<T> documentClass,
            String id);

    <T> boolean exists(
            Class<T> documentClass,
            String id);

    <T> void delete(
            Class<T> documentClass,
            String id);

}
