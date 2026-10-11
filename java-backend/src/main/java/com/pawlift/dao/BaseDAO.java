package com.pawlift.dao;

import com.pawlift.config.DatabaseConfig;
import com.pawlift.exception.DataAccessException;
import com.pawlift.exception.DuplicateKeyException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Small JDBC helper shared by all DAO implementations (PreparedStatement + ResultSet). */
public abstract class BaseDAO {

    @FunctionalInterface
    protected interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    protected DataSource ds() {
        return DatabaseConfig.getDataSource();
    }

    protected static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
    }

    protected <T> List<T> list(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection c = ds().getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> out = new ArrayList<>();
                while (rs.next()) out.add(mapper.map(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Query failed", e);
        }
    }

    protected <T> Optional<T> one(String sql, RowMapper<T> mapper, Object... params) {
        List<T> rows = list(sql, mapper, params);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    protected int count(String sql, Object... params) {
        try (Connection c = ds().getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Count failed", e);
        }
    }

    /** INSERT / UPDATE / DELETE. Returns affected rows. */
    protected int update(String sql, Object... params) {
        try (Connection c = ds().getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            if (e.getErrorCode() == 1062) throw new DuplicateKeyException("Duplicate entry", e);
            throw new DataAccessException("Constraint violation", e);
        } catch (SQLException e) {
            throw new DataAccessException("Update failed", e);
        }
    }

    protected static String ts(ResultSet rs, String column) throws SQLException {
        Timestamp t = rs.getTimestamp(column);
        return t == null ? null : t.toInstant().toString();
    }
}
