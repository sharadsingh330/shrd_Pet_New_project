package com.pawlift.dao.impl;

import com.pawlift.dao.BaseDAO;
import com.pawlift.dao.ShelterDAO;
import com.pawlift.model.Shelter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ShelterDAOImpl extends BaseDAO implements ShelterDAO {

    private static final String COLS = "id, owner_id, name, city, email, verified, created_at";

    private static Shelter map(ResultSet rs) throws SQLException {
        Shelter s = new Shelter();
        s.id = rs.getString("id");
        s.ownerId = rs.getString("owner_id");
        s.name = rs.getString("name");
        s.city = rs.getString("city");
        s.email = rs.getString("email");
        s.verified = rs.getBoolean("verified");
        s.createdAt = ts(rs, "created_at");
        return s;
    }

    @Override
    public Optional<Shelter> findById(String id) {
        return one("SELECT " + COLS + " FROM shelters WHERE id = ?", ShelterDAOImpl::map, id);
    }

    @Override
    public Optional<Shelter> findByOwnerId(String ownerId) {
        return one("SELECT " + COLS + " FROM shelters WHERE owner_id = ?", ShelterDAOImpl::map, ownerId);
    }

    @Override
    public List<Shelter> findAll() {
        return list("SELECT " + COLS + " FROM shelters ORDER BY name", ShelterDAOImpl::map);
    }

    @Override
    public Shelter create(Shelter shelter) {
        if (shelter.id == null || shelter.id.isBlank()) shelter.id = UUID.randomUUID().toString();
        update("INSERT INTO shelters (id, owner_id, name, city, email, verified) VALUES (?,?,?,?,?,?)",
                shelter.id, shelter.ownerId, shelter.name,
                shelter.city == null ? "" : shelter.city, shelter.email, shelter.verified);
        return findById(shelter.id).orElse(shelter);
    }

    @Override
    public int count() {
        return count("SELECT COUNT(*) FROM shelters");
    }
}
