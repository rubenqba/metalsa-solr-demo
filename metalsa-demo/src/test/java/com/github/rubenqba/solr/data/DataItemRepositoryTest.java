package com.github.rubenqba.solr.data;

import com.github.rubenqba.solr.model.DataItem;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * Created by IntelliJ Idea
 *
 * @author ruben.bresler
 */
@SpringBootTest
@RunWith(SpringRunner.class)
public class DataItemRepositoryTest {

    @Autowired
    @Qualifier("dataItemRepository")
    private DataItemRepository db;

    @Test
    public void select() throws Exception {
        Slice<DataItem> list = db.findAllByOrderByIdAsc(new PageRequest(0, 10));

        assertThat(list.getContent(), not(empty()));
    }
}