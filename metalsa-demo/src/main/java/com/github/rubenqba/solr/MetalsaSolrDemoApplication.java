package com.github.rubenqba.solr;

import com.github.rubenqba.solr.model.DataItem;
import com.metalsa.spx.search.Mapper;
import com.metalsa.spx.search.SearchService;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

@SpringBootApplication
public class MetalsaSolrDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MetalsaSolrDemoApplication.class, args);
    }

    @Bean
    public SearchService<DataItem> createService(@Value("${solr.host}") String url,
                                                 @Value("${solr.collection}")String collection) {
        return new SearchService<DataItem>(url, collection, indexMapper(), dataMapper());
    }

    @Bean
    public Mapper<SolrDocument, DataItem> dataMapper() {
        return from -> {
            DataItem item = new DataItem();
            item.setId(Long.parseLong((String) from.get("id")));
            item.setCodigo((String) from.get("codigo"));
            item.setDescripcion((String) from.get("descripcion"));
            item.setProveedor((String) from.get("proveedor"));
            item.setColor((String) from.get("color"));
            item.setPrecio((Double) from.get("precio"));
            item.setNombreUen((String) from.get("nombreUen"));
            item.setTiempoEntrega((Integer) from.get("tiempoEntrega"));
            item.setFechaCreacion(
                    LocalDateTime.ofInstant(((Date) from.get("fechaCreacion")).toInstant(), ZoneId.systemDefault()));
            return item;
        };
    }

    @Bean
    public Mapper<DataItem, SolrInputDocument> indexMapper() {
        return from -> {
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
                    .toInstant(ZoneOffset.from(ZonedDateTime.now()))
                    .toString());
            return doc;
        };
    }
}
