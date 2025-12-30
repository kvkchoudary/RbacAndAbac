package com.example.roleAndRules.service;

import com.example.roleAndRules.repository.EntitlementRepository;
import com.example.roleAndRules.repository.EntitlementRepository_v2;
import com.example.roleAndRules.security.SqlOperation;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class QueryExecutionService_v4 {

    private final SqlParserService_v3 parser;
    private final AuthorizationService_v3 auth;
    private final EntitlementRepository_v2 repo;
    private final JdbcTemplate jdbc;

    public QueryExecutionService_v4(
            SqlParserService_v3 parser,
            AuthorizationService_v3 auth,
            EntitlementRepository_v2 repo,
            JdbcTemplate jdbc
    ) {
        this.parser = parser;
        this.auth = auth;
        this.repo = repo;
        this.jdbc = jdbc;
    }

    public Object execute(String fid, String sql) throws Exception {

        // Get roles for FID
        List<String> roles = repo.roles(fid);

        // Parse SQL
        Statement stmt = parser.parse(sql);
        String op = parser.getOperation(stmt);
        List<String> tables = parser.getTables(stmt);

        // 1️⃣ Table-level enforcement
        for (String table : tables) {
            auth.checkTableAccess(fid, roles, table, op);
        }

        // 2️⃣ Column-level enforcement
        if (stmt instanceof Select s) {
            auth.checkColumnAccess(fid, roles, tables.get(0), "SELECT",
                    parser.getSelectColumns(s));
            return handleSelect(fid, roles, s); // MVP-1 row-level enforcement preserved
        }

        if (stmt instanceof Update u) {
            auth.checkColumnAccess(fid, roles, tables.get(0), "UPDATE",
                    parser.getUpdateColumns(u));
            return handleUpdate(fid, roles, u); // MVP-1 row-level enforcement preserved
        }

        if (stmt instanceof Delete d) {
            return handleDelete(fid, roles, d); // MVP-1 row-level enforcement preserved
        }

        if (stmt instanceof Insert i) {
            auth.checkColumnAccess(fid, roles, tables.get(0), "INSERT",
                    parser.getInsertColumns(i));

            // MVP-2: INSERT row-rule validation
            auth.validateInsertRowRules(fid, roles, tables.get(0),
                    parser.getInsertRows(i));

            return handleInsert(fid, roles, i);
        }

        throw new IllegalArgumentException("Unsupported SQL operation");
    }

    // -------------------------------
    // MVP-1 row-level enforcement
    // -------------------------------

    private Object handleSelect(String fid, List<String> roles, Select select) throws Exception {
        PlainSelect ps = (PlainSelect) select.getSelectBody();
        Expression where = ps.getWhere();

        for (String table : new TablesNamesFinder().getTableList(select)) {
            for (String rule : auth.rowRules(roles, table, SqlOperation.SELECT)) {
                where = (where == null)
                        ? CCJSqlParserUtil.parseCondExpression(rule)
                        : CCJSqlParserUtil.parseCondExpression(where + " AND " + rule);
            }
        }

        ps.setWhere(where);
        return jdbc.queryForList(select.toString());
    }

    private Object handleUpdate(String fid, List<String> roles, Update update) throws Exception {
        String table = update.getTable().getName();
        Expression where = update.getWhere();

        for (String rule : auth.rowRules(roles, table, SqlOperation.UPDATE)) {
            where = CCJSqlParserUtil.parseCondExpression(
                    (where == null ? rule : where + " AND " + rule)
            );
        }

        update.setWhere(where);
        jdbc.update(update.toString());
        return Map.of("status", "UPDATED");
    }

    private Object handleDelete(String fid, List<String> roles, Delete delete) throws Exception {
        String table = delete.getTable().getName();
        Expression where = delete.getWhere();

        for (String rule : auth.rowRules(roles, table, SqlOperation.DELETE)) {
            where = CCJSqlParserUtil.parseCondExpression(
                    (where == null ? rule : where + " AND " + rule)
            );
        }

        delete.setWhere(where);
        jdbc.update(delete.toString());
        return Map.of("status", "DELETED");
    }

    private Object handleInsert(String fid, List<String> roles, Insert insert) {
        jdbc.update(insert.toString());
        return Map.of("status", "INSERTED");
    }
}

