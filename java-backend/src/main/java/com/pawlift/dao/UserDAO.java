package com.pawlift.dao;

import com.pawlift.model.User;
import com.pawlift.model.UserRole;

import java.util.List;
import java.util.Optional;

public interface UserDAO {
    Optional<User> findById(String id);

    Optional<User> findByEmail(String email);

    List<User> findAll();

    User create(User user);

    boolean updateRole(String id, UserRole role);

    boolean delete(String id);

    int count();
}
