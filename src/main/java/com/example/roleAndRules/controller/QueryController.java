package com.example.roleAndRules.controller;

import com.example.roleAndRules.model.QueryRequest;
import com.example.roleAndRules.service.QueryExecutionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/query")
public class QueryController {

    private final QueryExecutionService service;

    public QueryController(QueryExecutionService service) {
        this.service = service;
    }

    @PostMapping
    public Object execute(@RequestBody QueryRequest req) throws Exception {
        return service.execute(req.fid, req.sql);
    }
}

