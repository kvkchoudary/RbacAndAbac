package com.example.roleAndRules.service;

import com.example.roleAndRules.repository.EntitlementRepository_v2;
import com.example.roleAndRules.security.SqlOperation;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AuthorizationService_v3 {

    private final EntitlementRepository_v2 repo;

    public AuthorizationService_v3(EntitlementRepository_v2 repo) {
        this.repo = repo;
    }

    public void checkTableAccess(String fid, List<String> roles, String table, String op) {
        if (!repo.tableAllowed(roles, table, op)) {
            throw new SecurityException("FID " + fid + " denied table access");
        }
    }

    public void checkColumnAccess(String fid, List<String> roles,
                                  String table, String op, List<String> columns) {

        if (columns.contains("*")) return; // MVP-2 rule: * allowed only if table allowed

        for (String col : columns) {
            if (!repo.columnAllowed(roles, table, col, op)) {
                throw new SecurityException("FID " + fid + " denied column: " + col);
            }
        }
    }

    public void validateInsertRowRules(String fid, List<String> roles,
                                       String table, List<Map<String, String>> rows) {

        List<String> rules = repo.rowRules(roles, table, "INSERT");

        for (Map<String, String> row : rows) {
            for (String rule : rules) {
                if (!evaluate(rule, row)) {
                    throw new SecurityException("INSERT violates rule: " + rule);
                }
            }
        }
    }

    private boolean evaluate(String rule, Map<String, String> row) {
        String[] parts = rule.split("=");
        String col = parts[0].trim().toUpperCase();
        String val = parts[1].trim().replace("'", "");
        return row.containsKey(col) && row.get(col).equalsIgnoreCase(val);
    }

    /* -------------------------
       Shared Helpers
       ------------------------- */

    public List<String> rowRules(
            List<String> roles,
            String table,
            SqlOperation operation
    ) {
        return repo.rowRules(
                roles,
                table.toUpperCase(),
                operation.name()
        );
    }
}

