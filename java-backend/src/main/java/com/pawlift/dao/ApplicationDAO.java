package com.pawlift.dao;

import com.pawlift.model.AdoptionApplication;
import com.pawlift.model.ApplicationStatus;

import java.util.List;
import java.util.Optional;

public interface ApplicationDAO {
    Optional<AdoptionApplication> findById(String id);

    List<AdoptionApplication> findByAdopter(String adopterId);

    List<AdoptionApplication> findByShelter(String shelterId);

    List<AdoptionApplication> findAll();

    boolean exists(String petId, String adopterId);

    AdoptionApplication create(AdoptionApplication application);

    boolean updateStatus(String id, ApplicationStatus status);

    int count();

    int countByStatus(ApplicationStatus status);
}
