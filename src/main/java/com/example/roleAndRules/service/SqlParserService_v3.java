package com.example.roleAndRules.service;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.*;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.util.TablesNamesFinder;

import java.util.*;

public class SqlParserService_v3 {

    public Statement parse(String sql) throws JSQLParserException {
        return CCJSqlParserUtil.parse(sql);
    }

    public String getOperation(Statement stmt) {
        if (stmt instanceof Select) return "SELECT";
        if (stmt instanceof Insert) return "INSERT";
        if (stmt instanceof Update) return "UPDATE";
        if (stmt instanceof Delete) return "DELETE";
        throw new IllegalArgumentException("Unsupported SQL operation");
    }

    public List<String> getTables(Statement stmt) {
        TablesNamesFinder finder = new TablesNamesFinder();
        List<String> tables = finder.getTableList(stmt);
        return tables.stream().map(String::toUpperCase).toList();
    }

    public List<String> getSelectColumns(Select select) {
        List<String> cols = new ArrayList<>();
        PlainSelect ps = (PlainSelect) select.getSelectBody();

        for (SelectItem item : ps.getSelectItems()) {
            if (item.toString().equals("*")) {
                cols.add("*");
            } else {
                cols.add(item.toString().toUpperCase());
            }
        }
        return cols;
    }

    public List<String> getUpdateColumns(Update update) {
        return update.getColumns()
                .stream()
                .map(c -> c.getColumnName().toUpperCase())
                .toList();
    }

    public List<String> getInsertColumns(Insert insert) {
        if (insert.getColumns() == null) return List.of();
        return insert.getColumns()
                .stream()
                .map(c -> c.getColumnName().toUpperCase())
                .toList();
    }

    public List<Map<String, String>> getInsertRows(Insert insert) {
        if (!(insert.getItemsList() instanceof ExpressionList exprList)) {
            throw new IllegalArgumentException("Only VALUES inserts supported in MVP-2");
        }

        List<Map<String, String>> rows = new ArrayList<>();
        List<Expression> values = exprList.getExpressions();
        List<Column> columns = insert.getColumns();

        Map<String, String> row = new HashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            row.put(
                    columns.get(i).getColumnName().toUpperCase(),
                    values.get(i).toString().replace("'", "")
            );
        }
        rows.add(row);
        return rows;
    }
}

