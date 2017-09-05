package com.github.rubenqba.solr.ui;

import com.github.rubenqba.solr.data.DataItemRepository;
import com.github.rubenqba.solr.model.DataItem;
import com.metalsa.spx.search.SearchResult;
import com.metalsa.spx.search.SearchService;
import com.vaadin.annotations.Theme;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.*;
import com.vaadin.ui.renderers.NumberRenderer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.log4j.Log4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FieldStatsInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.vaadin.addons.searchbox.SearchBox;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;

import java.text.MessageFormat;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by IntelliJ Idea
 *
 * @author ruben.bresler
 */
@SpringUI
@Theme("valo")
@Log4j
public class SearchUI extends UI {

    private DataItemRepository db;

    private SearchService<DataItem> service;

    private Grid<DataItem> grid;

    private SearchBox searchBox;

    private MVerticalLayout facets;

    private SolrQuery params;
    private Label sStatus;
    private ProgressBar bar;
    private Label status;
    private Button indexButton;
    private Button reIndexButton;

    @Autowired
    public SearchUI(DataItemRepository db, SearchService<DataItem> service) {
        this.db = db;
        this.service = service;
        this.grid = new Grid<>(DataItem.class);
        grid.removeAllColumns();
        grid.addColumn(DataItem::getId)
                .setCaption("Id")
                .setWidth(80);
        grid.addColumn(DataItem::getCodigo)
                .setCaption("Codigo")
                .setWidth(120);
        grid.addColumn(DataItem::getDescripcion)
                .setCaption("Descripcion")
                .setExpandRatio(1);
        grid.addColumn(DataItem::getProveedor)
                .setCaption("Proveedor")
                .setWidth(200);
        grid.addColumn(DataItem::getColor)
                .setCaption("Color")
                .setWidth(120);
        NumberFormat poundformat = NumberFormat.getCurrencyInstance(Locale.US);
        grid.addColumn(DataItem::getPrecio, new NumberRenderer(poundformat))
                .setCaption("Precio")
                .setWidth(80);
        grid.addColumn(DataItem::getNombreUen)
                .setCaption("UEN")
                .setWidth(200);
        grid.setSizeFull();
        grid.setVisible(false);

        bar = new ProgressBar(0.0f);
        bar.setWidth("100%");
        status = new Label("not running");
        indexButton = new Button("Indexar todo");
        indexButton.setWidth("130px");
        reIndexButton = new Button("Actualizar");
        reIndexButton.setWidth("130px");


        sStatus = new Label("");
        sStatus.setVisible(true);

        facets = new MVerticalLayout()
                .withSizeUndefined()
                .withMargin(false)
                .withVisible(false);
    }

    @Override
    protected void init(VaadinRequest vaadinRequest) {

        indexButton.addClickListener(event -> {
            new Thread(() -> indexAll()).start();
            startIndex(event);
        });

        reIndexButton.addClickListener(event -> {
            new Thread(() -> reIndex()).start();
            startIndex(event);
        });

        MVerticalLayout progress = new MVerticalLayout(bar, status)
                .withFullSize()
                .withMargin(false);
        MHorizontalLayout header = new MHorizontalLayout(new MVerticalLayout(indexButton, reIndexButton)
                .withMargin(false)
                .withSizeUndefined(), progress)
                .withFullWidth()
                .withExpand(progress, 1.0f);


        searchBox = new SearchBox(VaadinIcons.SEARCH, SearchBox.ButtonPosition.LEFT);
        searchBox.setButtonJoined(false);
        searchBox.setSearchMode(SearchBox.SearchMode.EXPLICIT);
        searchBox.setWidth("100%");
        searchBox.addSearchListener(e -> {
            if (params == null) {
                params = service.getParams(e.getSearchTerm(), 0, 15);
                params.addFacetField("proveedor", "nombreUen");
            }
            SearchResult<DataItem> result = service.search(params);

            facets.removeAllComponents();
            facets.setVisible(true);
            facets.alignAll(Alignment.TOP_LEFT);
            facets.add(getFacets(result));

            sStatus.setValue(MessageFormat.format("Page {0} de {1}, mostrando {2} items de {3}",
                    result.getPage(), result.getTotalPages(),
                    result.getNumberOfElement() * (result.getPage() + 1), result.getTotal()));
            grid.setItems(result.getContent());
            grid.setVisible(true);
        });

        MHorizontalLayout searchResult = new MHorizontalLayout()
                .withFullSize()
                .withSpacing(true)
                .withMargin(false);

        MVerticalLayout searchStatus = new MVerticalLayout(sStatus, grid)
                .withFullSize()
                .withSpacing(true)
                .withMargin(false)
                .withExpand(grid, 1.0f);

        searchResult.addComponents(facets);
        searchResult.addComponentsAndExpand(searchStatus);

        MVerticalLayout search = new MVerticalLayout(new MHorizontalLayout(searchBox)
                .withFullWidth()
                .withExpand(searchBox, 1.0f), searchResult)
                .withFullSize()
                .withMargin(false)
                .withExpand(searchResult, 1.0f);

        setContent(new MVerticalLayout(header, search)
                .withFullSize()
                .withExpand(search, 1.0f));
    }

    private void startIndex(Button.ClickEvent event) {
        getUI().getCurrent().setPollInterval(1500);

        // Disable the button until the work is done
        event.getButton().setEnabled(false);
        bar.setEnabled(true);
        status.setValue("running...");
    }

    protected void indexAll() {
        long current = 0;
        final long total = db.count();

        service.deleteAll();
        log.debug(MessageFormat.format("obteniendo {0} items...", total));

        getUI().access(() -> status.setValue(MessageFormat.format("indexados {0} de {1} -- {2}% done",
                0, total, 0f)));

        int page = 0;
        Slice<DataItem> slice;
        StopWatch watch = new StopWatch();
        watch.start();
        do {
            slice = db.findAllByOrderByIdAsc(new PageRequest(page++, 50));
            service.addItems(slice.getContent());
            current += slice.getNumberOfElements();
            log.debug(MessageFormat.format("indexados {0} items...", current));

            long finalCurrent = current;
            float percent = ((float) finalCurrent / total * 100);
            long eta = watch.getTime(TimeUnit.SECONDS) * total / current;
            getUI().access(() -> {
                bar.setValue((float) finalCurrent / total);
                status.setValue(MessageFormat.format("indexados {0} de {1}; {2}% done, ETA: {3}",
                        finalCurrent, total, percent,
                        Duration.ofSeconds(eta).toString()
                                .substring(2)
                                .replaceAll("(\\d[HMS])(?!$)", "$1 ")
                                .toLowerCase()));
            });

        } while (!slice.isLast());
        watch.stop();

        getUI().access(() -> indexButton.setEnabled(true));
    }

    protected void reIndex() {
        long current = 0;
        FieldStatsInfo stats = service.statsInfo("fechaCreacion");
        LocalDateTime last = LocalDateTime.ofInstant(((Date)service.statsInfo("fechaCreacion").getMax()).toInstant(),
                ZoneId.systemDefault());
        final long total = db.countByFechaCreacionGreaterThanEqualOrderByIdAsc(last);

        log.debug(MessageFormat.format("obteniendo {0} items...", total));

        getUI().access(() -> status.setValue(MessageFormat.format("indexados {0} de {1} -- {2}% done",
                0, total, 0f)));

        int page = 0;
        Slice<DataItem> slice;
        StopWatch watch = new StopWatch();
        watch.start();
        do {
            slice = db.findAllByFechaCreacionGreaterThanEqualOrderByIdAsc(last, new PageRequest(page++, 50));
            service.addItems(slice.getContent());
            current += slice.getNumberOfElements();
            log.debug(MessageFormat.format("reindexados {0} items...", current));

            long finalCurrent = current;
            float percent = ((float) finalCurrent / total * 100);
            long eta = watch.getTime(TimeUnit.SECONDS) * total / current;
            getUI().access(() -> {
                bar.setValue((float) finalCurrent / total);
                status.setValue(MessageFormat.format("reindexados {0} de {1}; {2}% done, ETA: {3}",
                        finalCurrent, total, percent,
                        Duration.ofSeconds(eta).toString()
                                .substring(2)
                                .replaceAll("(\\d[HMS])(?!$)", "$1 ")
                                .toLowerCase()));
            });

        } while (!slice.isLast());
        watch.stop();

        getUI().access(() -> indexButton.setEnabled(true));
    }



    @Data
    @AllArgsConstructor
    static class FacetItem {

        private String value;
        private Long count;
        private String filterQuery;

        @Override
        public String toString() {
            return MessageFormat.format("{0} ({1})", this.getValue(), this.getCount());
        }
    }

    private Collection<Component> getFacets(SearchResult<DataItem> response) {
        List<Component> facet = new ArrayList<>();

        response.getFacetFields()
                .forEach(f -> {
                    CheckBoxGroup<FacetItem> g = new CheckBoxGroup(
                            StringUtils.join(
                                    StringUtils.splitByCharacterTypeCamelCase(f.getName()),
                                    ' '
                            ),
                            f.getValues().stream()
                                    .filter(i -> i.getCount() > 0)
                                    .map(c -> new FacetItem(c.getName(), c.getCount(), c.getAsFilterQuery()))
                                    .collect(Collectors.toList())
                    );
                    g.setHeightUndefined();
                    g.setWidth("100%");
                    g.addSelectionListener(evt -> {
                        evt.getAllSelectedItems().forEach(p -> {
                            log.debug(p.getFilterQuery());
                            params.addFilterQuery(p.getFilterQuery());
                        });
                        searchBox.search();
                    });
                    facet.add(g);
                });

        return facet;
    }
}
