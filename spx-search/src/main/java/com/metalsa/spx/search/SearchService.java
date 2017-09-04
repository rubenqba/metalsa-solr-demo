package com.metalsa.spx.search;

import lombok.extern.log4j.Log4j;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.FieldStatsInfo;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by IntelliJ Idea
 *
 * @author ruben.bresler
 */
@Log4j
public class SearchService<T> {

    private String collection;

    private SolrClient client;

    private Mapper<T, SolrInputDocument> dataToIndex;
    private Mapper<SolrDocument, T> indexToData;

    public SearchService(String url, String collection,
                         Mapper<T, SolrInputDocument> dataToIndex,
                         Mapper<SolrDocument, T> indexToData) {

        log.debug(MessageFormat.format("creando servicio de indexado para coleccion {1} en servidor {0}", url, collection));
        this.collection = collection;
        this.dataToIndex = dataToIndex;
        this.indexToData = indexToData;
        this.client = new HttpSolrClient.Builder().withBaseSolrUrl(url)
                .build();
    }

    public SolrQuery getParams(String query, long page, long size) {
        log.debug(MessageFormat.format("creando query q={0}, start={1}, rows={2}", query, page, size));
        SolrQuery params = new SolrQuery(query);
        params.setStart((int) page);
        params.setRows((int) size);
        return params;
    }

    public SearchResult<T> search(String query, long page, long size) {
        return search(getParams(query, page, size));
    }

    public SearchResult<T> search(SolrQuery params) {
        log.debug(MessageFormat.format("coleccion={0}, query={1}", collection, params.toQueryString()));
        try {
            QueryResponse response = client.query(collection, params);
            return new SearchResult<>(response, indexToData);
        } catch (SolrServerException | IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public void addItems(List<T> items) {
        List<SolrInputDocument> list = items.stream()
                .map(dataToIndex::convert)
                .collect(Collectors.toList());
        if (!items.isEmpty()) {
            try {
                client.add(collection, list, 5000);
                client.commit(collection);
            } catch (SolrServerException | IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public void deleteItems(List<String> ids) {
        try {
            client.deleteById(collection, ids);
            client.commit(collection);
        } catch (SolrServerException | IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void deleteAll() {
        try {
            client.deleteByQuery(collection, "*:*");
            client.commit(collection);
        } catch (SolrServerException | IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public long countAll() {
        SolrQuery params = getParams("*:*", 0, 0);
        try {
            return client.query(collection, params).getResults().getNumFound();
        } catch (SolrServerException | IOException e) {
            log.error(e.getMessage(), e);
        }
        return 0;
    }

    public FieldStatsInfo statsInfo(String field) {
        SolrQuery params = getParams("*:*", 0, 0);
        params.addGetFieldStatistics(field);

        return search(params).getStats(field);
    }
}
