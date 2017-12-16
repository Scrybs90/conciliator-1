package com.codefork.refine.controllers;

import com.codefork.refine.datasource.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/reconcile/solr")
public class SolrController extends DataSourceController {

    @Autowired
    @Qualifier("solr")
    @Override
    public void setDataSource(DataSource dataSource) {
        super.setDataSource(dataSource);
    }

}
