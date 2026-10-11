package com.pawlift.dao.impl;

import com.pawlift.dao.BaseDAO;
import com.pawlift.dao.UserDAO;
import com.pawlift.model.User;
import com.pawlift.model.UserRole;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class UserDAOImpl extends BaseDAO implements UserDAO {

    private static final String COLS =
            "id, name, email, password_hash, role, phone, city, bio, created_at";

    private static User map(ResultSet rs) throws SQLException {
        User u = User.forRole(UserRole.from(rs.getString("role")));
        u.id = rs.getString("id");
        u.fullName = rs.getString("name");
        u.email = rs.getString("email");
        u.passwordHash = rs.getString("password_hash");
        u.phone = rs.getString("phone");
        u.city = rs.getString("city");
        u.bio = rs.getString("bio");
        u.createdAt = ts(rs, "created_at");
        return u;
    }

    @Override
    public Optional<User> findById(String id) {
        return one("SELECT " + COLS + " FROM users WHERE id = ?", UserDAOImpl::map, id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return one("SELECT " + COLS + " FROM users WHERE LOWER(email) = LOWER(?)", UserDAOImpl::map, email);
    }

    @Override
    public List<User> findAll() {
        return list("SELECT " + COLS + " FROM users ORDER BY created_at DESC", UserDAOImpl::map);
    }

    @Override
    public User create(User user) {
        if (user.id == null || user.id.isBlank()) user.id = UUID.randomUUID().toString();
        if (user.role == null) user.role = UserRole.ADOPTER;
        update("INSERT INTO users (id, name, email, password_hash, role, phone, city, bio) VALUES (?,?,?,?,?,?,?,?)",
                user.id, user.fullName, user.email, user.passwordHash, user.role.value(),
                user.phone, user.city, user.bio);
        return findById(user.id).orElse(user);
    }

    @Override
    public boolean updateRole(String id, UserRole role) {
        return update("UPDATE users SET role = ? WHERE id = ?", role.value(), id) > 0;
    }

    @Override
    public boolean delete(String id) {
        // shelters, pets, applications and messages are removed by ON DELETE CASCADE
        return update("DELETE FROM users WHERE id = ?", id) > 0;
    }

    @Override
    public int count() {
        return count("SELECT COUNT(*) FROM users");
    }
}
