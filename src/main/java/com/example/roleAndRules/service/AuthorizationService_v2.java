package com.example.roleAndRules.service;


import java.util.List;
import java.util.Map;

public class AuthorizationService_v2 {

    // Check if FID has roles for table-level access
    public void checkTableAccess(String fid, List<String> roles, String table, String operation) {
        boolean allowed = roles.stream()
                .anyMatch(role -> RoleTablePermissionRepository.isAllowed(role, table, operation));
        if (!allowed) {
            throw new SecurityException("FID " + fid + " not allowed to perform " + operation + " on table " + table);
        }
    }

    // Column-level enforcement
    public void checkColumnAccess(String fid, List<String> roles, String table, String operation, List<String> columns) {
        for (String col : columns) {
            boolean allowed = roles.stream()
                    .anyMatch(role -> RoleColumnPermissionRepository.isAllowed(role, table, col, operation));
            if (!allowed) {
                throw new SecurityException("FID " + fid + " not allowed to access column " + col + " for operation " + operation);
            }
        }
    }

    // Row-level enforcement for SELECT/UPDATE/DELETE (existing MVP-1)
    public String getRowRuleFilter(String fid, List<String> roles, String table, String operation) {
        // Return SQL WHERE clause to be appended
        return RoleRowRuleRepository.getCombinedRowRule(roles, table, operation);
    }

    // INSERT row-rule validation
    public void validateInsertRowRules(String fid, List<String> roles, String table, List<Map<String, String>> rows) {
        List<String> rules = RoleRowRuleRepository.getRules(roles, table, "INSERT");
        for (Map<String, String> row : rows) {
            for (String rule : rules) {
                if (!evaluateRule(rule, row)) {
                    throw new SecurityException("INSERT violates row rule: " + rule + " for FID: " + fid);
                }
            }
        }
    }

    // Simple evaluator: only supports equality checks e.g., department='HR'
    private boolean evaluateRule(String rule, Map<String, String> row) {
        String[] parts = rule.split("=");
        if (parts.length != 2) return false;
        String col = parts[0].trim().toUpperCase();
        String val = parts[1].trim().replaceAll("'", "");
        return row.containsKey(col) && row.get(col).equalsIgnoreCase(val);
    }
}

