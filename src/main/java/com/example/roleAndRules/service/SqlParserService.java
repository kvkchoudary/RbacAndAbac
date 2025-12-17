package com.example.roleAndRules.service;

import com.example.roleAndRules.security.*;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.*;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import org.springframework.stereotype.Service;

@Service
public class SqlParserService {

    public ParsedSql parse(String sql) throws Exception {
        Statement stmt = CCJSqlParserUtil.parse(sql);

        ParsedSql ps = new ParsedSql();
        ps.statement = stmt;

        if (stmt instanceof Select) ps.operation = SqlOperation.SELECT;
        else if (stmt instanceof Insert) ps.operation = SqlOperation.INSERT;
        else if (stmt instanceof Update) ps.operation = SqlOperation.UPDATE;
        else if (stmt instanceof Delete) ps.operation = SqlOperation.DELETE;
        else throw new SecurityException("Unsupported SQL");

        return ps;
    }
}

