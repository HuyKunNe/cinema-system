package com.cinema.common.search.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import com.cinema.common.search.client.ElasticsearchSearchClient;
import com.cinema.common.search.client.SearchClient;
import com.cinema.common.search.service.DefaultSearchService;
import com.cinema.common.search.service.SearchService;

@AutoConfiguration
@ConditionalOnBean(ElasticsearchOperations.class)
public class SearchConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SearchClient searchClient(
            ElasticsearchOperations operations) {

        return new ElasticsearchSearchClient(
                operations);

    }

    @Bean
    @ConditionalOnMissingBean
    public SearchService searchService(
            SearchClient searchClient) {

        return new DefaultSearchService(
                searchClient);

    }

}
