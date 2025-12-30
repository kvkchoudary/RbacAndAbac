package com.example.roleAndRules.service;


import com.example.roleAndRules.repository.EntitlementRepository_v2;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.delete.Delete;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class QueryExecutionService_v3 {

    private final SqlParserService_v3 parser;
    private final AuthorizationService_v3 auth;
    private final EntitlementRepository_v2 repo;
    private final JdbcTemplate jdbc;

    public QueryExecutionService_v3(
            SqlParserService_v3 parser,
            AuthorizationService_v3 auth,
            EntitlementRepository_v2 repo,
            JdbcTemplate jdbc) {
        this.parser = parser;
        this.auth = auth;
        this.repo = repo;
        this.jdbc = jdbc;
    }

    public Object execute(String fid, String sql) throws Exception {

        List<String> roles = repo.roles(fid);

        Statement stmt = parser.parse(sql);
        String op = parser.getOperation(stmt);
        List<String> tables = parser.getTables(stmt);

        for (String table : tables) {
            auth.checkTableAccess(fid, roles, table, op);
        }

        if (stmt instanceof Select s) {
            auth.checkColumnAccess(fid, roles, tables.get(0), "SELECT",
                    parser.getSelectColumns(s));
        }

        if (stmt instanceof Update u) {
            auth.checkColumnAccess(fid, roles, tables.get(0), "UPDATE",
                    parser.getUpdateColumns(u));
        }

        if (stmt instanceof Insert i) {
            auth.checkColumnAccess(fid, roles, tables.get(0), "INSERT",
                    parser.getInsertColumns(i));
            auth.validateInsertRowRules(fid, roles, tables.get(0),
                    parser.getInsertRows(i));
        }

        return jdbc.execute(sql); // execute original SQL safely
    }
}

