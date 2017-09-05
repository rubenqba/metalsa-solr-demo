package com.github.rubenqba.solr.data;

import com.github.rubenqba.solr.model.DataItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.time.LocalDateTime;

/**
 * Created by IntelliJ Idea
 *
 * @author ruben.bresler
 */
public interface DataItemRepository extends PagingAndSortingRepository<DataItem, Long> {
    Slice<DataItem> findAllByOrderByIdAsc(Pageable page);

    Slice<DataItem> findAllByFechaCreacionGreaterThanEqualOrderByIdAsc(LocalDateTime last, Pageable page);

    long countByFechaCreacionGreaterThanEqualOrderByIdAsc(LocalDateTime last);
}
