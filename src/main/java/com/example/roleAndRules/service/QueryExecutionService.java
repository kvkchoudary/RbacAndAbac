package com.example.roleAndRules.service;

import com.example.roleAndRules.repository.EntitlementRepository;
import com.example.roleAndRules.security.ParsedSql;
import com.example.roleAndRules.security.SqlOperation;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.delete.Delete;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class QueryExecutionService {

    private final EntitlementRepository repo;
    private final SqlParserService parser;
    private final JdbcTemplate jdbc;
    private final AuthorizationService auth;

    public QueryExecutionService(
            EntitlementRepository repo,
            SqlParserService parser,
            AuthorizationService auth,
            JdbcTemplate jdbc
    ) {
        this.repo = repo;
        this.parser = parser;
        this.auth = auth;
        this.jdbc = jdbc;
    }

    public Object execute(String fid, String sql) throws Exception {

        List<String> roles = auth.rolesForFid(fid);
        ParsedSql parsed = parser.parse(sql);

        return switch (parsed.operation) {

            case SELECT -> handleSelect(roles, (Select) parsed.statement);
            case INSERT -> handleInsert(roles, (Insert) parsed.statement);
            case UPDATE -> handleUpdate(roles, (Update) parsed.statement);
            case DELETE -> handleDelete(roles, (Delete) parsed.statement);
        };
    }



    private List<Map<String, Object>> handleSelect(
            List<String> roles,
            Select select
    ) throws Exception {

        auth.authorizeSelect(roles, select);

        PlainSelect ps = (PlainSelect) select.getSelectBody();
        Expression where = ps.getWhere();

        for (String table : new TablesNamesFinder().getTableList(select)) {
            for (String rule : auth.rowRules(
                    roles,
                    table,
                    SqlOperation.SELECT)) {

                where = (where == null)
                        ? CCJSqlParserUtil.parseCondExpression(rule)
                        : CCJSqlParserUtil.parseCondExpression(
                        where + " AND " + rule
                );
            }
        }

        ps.setWhere(where);
        return jdbc.queryForList(select.toString());
    }


    private Object handleUpdate(
            List<String> roles,
            Update update
    ) throws Exception {

        auth.authorizeUpdate(roles, update);

        String table = update.getTable().getName();

        Expression where = update.getWhere();

        for (String rule : auth.rowRules(
                roles,
                table,
                SqlOperation.UPDATE)) {

            where = CCJSqlParserUtil.parseCondExpression(
                    where + " AND " + rule
            );
        }

        update.setWhere(where);
        jdbc.update(update.toString());

        return Map.of("status", "UPDATED");
    }

    private Object handleInsert(
            List<String> roles,
            Insert insert
    ) {

        auth.authorizeInsert(roles, insert);

        jdbc.update(insert.toString());
        return Map.of("status", "INSERTED");
    }

    private Object handleDelete(
            List<String> roles,
            Delete delete
    ) throws Exception {

        auth.authorizeDelete(roles, delete);

        String table = delete.getTable().getName();

        Expression where = delete.getWhere();

        for (String rule : auth.rowRules(
                roles,
                table,
                SqlOperation.DELETE)) {

            where = CCJSqlParserUtil.parseCondExpression(
                    where + " AND " + rule
            );
        }

        delete.setWhere(where);
        jdbc.update(delete.toString());

        return Map.of("status", "DELETED");
    }


}

