package com.example.roleAndRules.service;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.statement.values.ValuesStatement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SqlParserService_v2 {

    // Parse SQL string into AST Statement
    public Statement parse(String sql) throws JSQLParserException {
        return CCJSqlParserUtil.parse(sql);
    }

    // Get main operation type
    public String getOperation(Statement stmt) {
        if (stmt instanceof Select) return "SELECT";
        if (stmt instanceof Insert) return "INSERT";
        if (stmt instanceof Update) return "UPDATE";
        if (stmt instanceof Delete) return "DELETE";
        throw new IllegalArgumentException("Unsupported SQL operation");
    }

    // Extract all table names used in statement
    public List<String> getTables(Statement stmt) {
        List<String> tables = new ArrayList<>();
        if (stmt instanceof Select) {
            PlainSelect ps = (PlainSelect) ((Select) stmt).getSelectBody();
            ps.getFromItem().accept(new TablesNamesFinderVisitor(tables));
            if (ps.getJoins() != null) {
                ps.getJoins().forEach(join -> join.getRightItem().accept(new TablesNamesFinderVisitor(tables)));
            }
        } else if (stmt instanceof Insert) {
            tables.add(((Insert) stmt).getTable().getName().toUpperCase());
        } else if (stmt instanceof Update) {
            ((Update) stmt).getTables().forEach(t -> tables.add(t.getName().toUpperCase()));
        } else if (stmt instanceof Delete) {
            tables.add(((Delete) stmt).getTable().getName().toUpperCase());
        }
        return tables;
    }

    // Extract SELECT columns for enforcement
    public List<String> getSelectColumns(Select select) {
        List<String> columns = new ArrayList<>();
        PlainSelect ps = (PlainSelect) select.getSelectBody();
        for (SelectItem item : ps.getSelectItems()) {
            columns.add(item.toString().toUpperCase());
        }
        return columns;
    }

    // Extract UPDATE columns
    public List<String> getUpdateColumns(Update update) {
        List<String> columns = new ArrayList<>();
        update.getColumns().forEach(c -> columns.add(c.getColumnName().toUpperCase()));
        return columns;
    }

    // Extract INSERT columns
    public List<String> getInsertColumns(Insert insert) {
        List<String> columns = new ArrayList<>();
        if (insert.getColumns() != null) {
            insert.getColumns().forEach(c -> columns.add(c.getColumnName().toUpperCase()));
        }
        return columns;
    }

    // Extract values for INSERT row-rule validation
    public List<Map<String, String>> getInsertRows(Insert insert) {
        List<Map<String, String>> rows = new ArrayList<>();
        if (insert.getItemsList() instanceof ExpressionList) {
            List<Expression> values = ((ExpressionList) insert.getItemsList()).getExpressions();
            Map<String, String> rowMap = new HashMap<>();
            List<Column> columns = insert.getColumns();
            for (int i = 0; i < columns.size(); i++) {
                rowMap.put(columns.get(i).getColumnName().toUpperCase(), values.get(i).toString().replaceAll("'", ""));
            }
            rows.add(rowMap);
        } else if (insert.getItemsList() instanceof net.sf.jsqlparser.statement.select.SubSelect) {
            throw new IllegalArgumentException("INSERT ... SELECT not allowed in MVP-2");
        }
        return rows;
    }
}

