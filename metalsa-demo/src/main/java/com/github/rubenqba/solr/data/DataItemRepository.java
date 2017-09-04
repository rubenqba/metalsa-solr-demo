package com.github.rubenqba.solr.data;

import com.github.rubenqba.solr.model.DataItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * Created by IntelliJ Idea
 *
 * @author ruben.bresler
 */
public interface DataItemRepository extends PagingAndSortingRepository<DataItem, Long> {
    Slice<DataItem> findAllByOrderByIdAsc(Pageable page);
}
