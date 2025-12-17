package com.example.roleAndRules.service;


import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.delete.Delete;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

public class QueryExecutionService {

    private final SqlParserService sqlParserService;
    private final AuthorizationService authorizationService;
    private final JdbcTemplate jdbcTemplate;

    public QueryExecutionService(SqlParserService parser, AuthorizationService auth, JdbcTemplate jdbc) {
        this.sqlParserService = parser;
        this.authorizationService = auth;
        this.jdbcTemplate = jdbc;
    }

    public Object executeQuery(String fid, String sql, List<String> roles) throws Exception {
        Statement stmt = sqlParserService.parse(sql);
        String operation = sqlParserService.getOperation(stmt);

        List<String> tables = sqlParserService.getTables(stmt);

        // Table-level enforcement
        for (String table : tables) {
            authorizationService.checkTableAccess(fid, roles, table, operation);
        }

        // Column-level enforcement
        if (stmt instanceof Select) {
            List<String> columns = sqlParserService.getSelectColumns((Select) stmt);
            for (String table : tables) {
                authorizationService.checkColumnAccess(fid, roles, table, "SELECT", columns);
            }
        } else if (stmt instanceof Update) {
            List<String> columns = sqlParserService.getUpdateColumns((Update) stmt);
            for (String table : tables) {
                authorizationService.checkColumnAccess(fid, roles, table, "UPDATE", columns);
            }
        } else if (stmt instanceof Insert) {
            List<String> columns = sqlParserService.getInsertColumns((Insert) stmt);
            for (String table : tables) {
                authorizationService.checkColumnAccess(fid, roles, table, "INSERT", columns);
            }
            // INSERT row-rule validation
            List<Map<String, String>> rows = sqlParserService.getInsertRows((Insert) stmt);
            for (String table : tables) {
                authorizationService.validateInsertRowRules(fid, roles, table, rows);
            }
        }

        // Row-level enforcement (existing MVP-1)
        // Here you can enrich SELECT/UPDATE/DELETE with WHERE clauses from getRowRuleFilter
        // For simplicity, we assume direct execution

        return jdbcTemplate.execute(stmt.toString());
    }
}

