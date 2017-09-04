package com.metalsa.spx.search;

import lombok.AccessLevel;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FieldStatsInfo;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.util.NamedList;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by IntelliJ Idea
 *
 * @author ruben.bresler
 */
@Getter
public class SearchResult<T> {
    private List<T> content = new ArrayList<>();
    private Mapper<SolrDocument, T> mapper;
    private long page;
    private long total;
    private long pageSize;
    private long numberOfElement;
    @Getter(AccessLevel.NONE)
    private QueryResponse response;

    public SearchResult(QueryResponse response, Mapper<SolrDocument, T> mapper) {
        this.mapper = mapper;
        this.response = response;
        this.total = response.getResults().getNumFound();
        this.page = response.getResults().getStart();

        if (response.getHeader().get("params") != null) {
            NamedList params = (NamedList) response.getHeader().get("params");
            this.pageSize = StringUtils.isNotBlank((String) params.get("rows")) ? Integer.parseInt((String) params.get("rows")) : 0;
        }

        this.content.addAll(response.getResults().stream()
                .map(this.mapper::convert)
                .collect(Collectors.toList()));

        this.numberOfElement = this.content.size();
    }

    public long getTotalPages() {
        return total/ pageSize;
    }

    boolean isLast() {
        return this.page > getTotalPages();
    }

    public List<FacetField> getFacetFields() {
        return response.getFacetFields();
    }

    public FacetField getFacetResultPage(String name) {
        return response.getFacetField(name);
    }

    public FieldStatsInfo getStats(String fieldName) {
        return response.getFieldStatsInfo().get(fieldName);
    }
}
