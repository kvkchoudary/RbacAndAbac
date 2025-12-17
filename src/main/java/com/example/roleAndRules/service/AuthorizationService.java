package com.example.roleAndRules.service;

import com.example.roleAndRules.repository.EntitlementRepository;
import com.example.roleAndRules.security.SqlOperation;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthorizationService {

    private final EntitlementRepository repo;

    public AuthorizationService(EntitlementRepository repo) {
        this.repo = repo;
    }

    /* -------------------------
       Role Resolution
       ------------------------- */

    public List<String> rolesForFid(String fid) {
        List<String> roles = repo.roles(fid);
        if (roles.isEmpty()) {
            throw new SecurityException("FID has no roles");
        }
        return roles;
    }

    /* -------------------------
       SELECT Authorization
       ------------------------- */

    public void authorizeSelect(List<String> roles, Select select) {

        List<String> tables =
                new TablesNamesFinder().getTableList(select);

        for (String table : tables) {
            if (!repo.tableAllowed(roles, table.toUpperCase(), SqlOperation.SELECT.name())) {
                throw new SecurityException(
                        "SELECT not allowed on table: " + table
                );
            }
        }
    }

    /* -------------------------
       UPDATE Authorization
       ------------------------- */

    public void authorizeUpdate(List<String> roles, Update update) {

        String table = update.getTable().getName().toUpperCase();

        if (!repo.tableAllowed(roles, table, SqlOperation.UPDATE.name())) {
            throw new SecurityException(
                    "UPDATE not allowed on table: " + table
            );
        }

        if (update.getWhere() == null) {
            throw new SecurityException(
                    "UPDATE without WHERE is forbidden"
            );
        }
    }

    public void authorizeInsert(List<String> roles, Insert insert) {

        String table = insert.getTable().getName().toUpperCase();

        if (!repo.tableAllowed(roles, table, SqlOperation.INSERT.name())) {
            throw new SecurityException("INSERT not allowed on table: " + table);
        }

        // MVP safety
        if (insert.getItemsList() == null) {
            throw new SecurityException("INSERT ... SELECT not allowed");
        }
    }


    public void authorizeDelete(List<String> roles, Delete delete) {

        String table = delete.getTable().getName().toUpperCase();

        if (!repo.tableAllowed(roles, table, SqlOperation.DELETE.name())) {
            throw new SecurityException("DELETE not allowed on table: " + table);
        }

        if (delete.getWhere() == null) {
            throw new SecurityException("DELETE without WHERE is forbidden");
        }
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

