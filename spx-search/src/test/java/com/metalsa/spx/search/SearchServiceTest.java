package com.metalsa.spx.search;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.solr.client.solrj.response.FieldStatsInfo;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.metalsa.spx.search.CaseInsensitiveSubstringMatcher.containsIgnoringCase;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Created by IntelliJ Idea
 *
 * @author ruben.bresler
 */
public class SearchServiceTest {

    private SearchService<Pojo> service;

    @Before
    public void setUp() throws Exception {
        service = new SearchService<>("http://localhost:8983/solr", "test",
                from -> {
                    SolrInputDocument doc = new SolrInputDocument();
                    doc.addField("id", from.getId());
                    doc.addField("codigo", from.getCodigo());
                    doc.addField("descripcion", from.getDescripcion());
                    doc.addField("proveedor", from.getProveedor());
                    doc.addField("color", from.getColor());
                    doc.addField("precio", from.getPrecio());
                    doc.addField("nombreUen", from.getNombreUen());
                    doc.addField("tiempoEntrega", from.getTiempoEntrega());
                    doc.addField("fechaCreacion", from.getFechaCreacion()
                            .toInstant(ZoneOffset.from(ZonedDateTime.now(ZoneId.systemDefault())))
                            .toString());
                    return doc;
                },
                from -> {
                    Pojo pojo = new Pojo();
                    pojo.setId(Long.parseLong((String) from.get("id")));
                    pojo.setCodigo((String) from.get("codigo"));
                    pojo.setDescripcion((String) from.get("descripcion"));
                    pojo.setProveedor((String) from.get("proveedor"));
                    pojo.setColor((String) from.get("color"));
                    pojo.setPrecio((Double) from.get("precio"));
                    pojo.setNombreUen((String) from.get("nombreUen"));
                    pojo.setTiempoEntrega((Integer) from.get("tiempoEntrega"));
                    pojo.setFechaCreacion(
                            LocalDateTime.ofInstant(((Date) from.get("fechaCreacion")).toInstant(), ZoneId.systemDefault()));
                    return pojo;
                }
        );
    }

    private List<Pojo> createData() throws IOException {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        TypeReference<List<Pojo>> type = new TypeReference<List<Pojo>>() {
        };
        return mapper.readValue(new File("src/test/resources/data.json"), type);
    }

    @Test
    public void search() throws Exception {

        service.addItems(createData());

        SearchResult<Pojo> chunk = service.search("negra", 1, 2);

        assertThat(chunk.getPage(), is(1L));
        assertThat(chunk.getPageSize(), is(2L));
        assertThat(chunk.getNumberOfElement(), is(2L));
        assertThat(chunk.getTotal(), is(4L));
        assertThat(chunk.getContent(), hasSize(2));
        assertThat(chunk.isLast(), is(false));
        assertThat(chunk.getContent(), everyItem(hasProperty("descripcion",
                anyOf(containsIgnoringCase("negra"), containsIgnoringCase("negras"),
                        containsIgnoringCase("negro"), containsIgnoringCase("negros")))));
    }

    @Test
    public void indexItem() throws Exception {
        List<Pojo> list = createData();
        assertThat(list, hasSize(greaterThan(0)));

        long countAll = service.countAll();

        service.addItems(list);

        assertThat(service.countAll(), is(countAll));
    }

    @Test
    public void countAndDelete() throws Exception {
        if (service.countAll() == 0)
            service.addItems(createData());

        long all = service.countAll();

        service.deleteItems(createData().stream()
                .limit(2)
                .map(p -> p.getId().toString())
                .collect(Collectors.toList()));
        assertThat(service.countAll(), is(all - 2));

        service.deleteAll();
        assertThat(service.countAll(), is(0L));
    }

    @Test
    public void stats() throws Exception {

        List<Pojo> data = createData();
        service.addItems(data);

        FieldStatsInfo stats = service.statsInfo("fechaCreacion");

        assertThat(stats, notNullValue());

        LocalDateTime max = data.stream()
                .map(Pojo::getFechaCreacion)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        LocalDateTime min = data.stream()
                .map(Pojo::getFechaCreacion)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        assertThat(stats.getMax(), allOf(
                notNullValue(),
                instanceOf(Date.class),
                is(Date.from(max.atZone(ZoneId.systemDefault()).toInstant()))));

        assertThat(stats.getMin(), allOf(
                notNullValue(),
                instanceOf(Date.class),
                is(Date.from(min.atZone(ZoneId.systemDefault()).toInstant()))));
    }
}