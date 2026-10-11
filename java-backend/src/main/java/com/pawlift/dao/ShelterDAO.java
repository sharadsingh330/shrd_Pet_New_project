package com.pawlift.dao;

import com.pawlift.model.Shelter;

import java.util.List;
import java.util.Optional;

public interface ShelterDAO {
    Optional<Shelter> findById(String id);

    Optional<Shelter> findByOwnerId(String ownerId);

    List<Shelter> findAll();

    Shelter create(Shelter shelter);

    int count();
}
