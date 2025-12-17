package com.example.roleAndRules.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EntitlementRepository {

    private final JdbcTemplate jdbc;

    public EntitlementRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<String> roles(String fid) {
        return jdbc.queryForList(
                "SELECT role_name FROM FID_ROLE WHERE fid_name=?",
                String.class, fid
        );
    }

    public boolean tableAllowed(List<String> roles, String table, String perm) {
        String in = String.join(",", roles.stream().map(r -> "'" + r + "'").toList());
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM ROLE_TABLE_PERMISSION WHERE role_name IN (" + in + ") AND table_name=? AND permission=?",
                Integer.class, table, perm
        ) > 0;
    }

    public List<String> rowRules(List<String> roles, String table, String perm) {
        String in = String.join(",", roles.stream().map(r -> "'" + r + "'").toList());
        return jdbc.queryForList(
                "SELECT rule_expression FROM ROLE_ROW_RULE WHERE role_name IN (" + in + ") AND table_name=? AND permission=?",
                String.class, table, perm
        );
    }
}

