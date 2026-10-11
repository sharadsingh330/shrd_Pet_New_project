package com.pawlift.dao;

import com.pawlift.model.ListingStatus;
import com.pawlift.model.Pet;
import com.pawlift.model.PetStatus;

import java.util.List;
import java.util.Optional;

public interface PetDAO {
    /** Approved listings only, optionally filtered. Null/blank filters are ignored. */
    List<Pet> findPublic(String q, String species, String city);

    Optional<Pet> findById(String id);

    List<Pet> findByShelter(String shelterId);

    List<Pet> findAll();

    Pet create(Pet pet);

    boolean update(Pet pet);

    boolean delete(String id);

    int count();

    int countByListingStatus(ListingStatus status);

    int countByAdoptionStatus(PetStatus status);
}
