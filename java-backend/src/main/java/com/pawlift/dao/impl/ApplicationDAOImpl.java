package com.pawlift.dao.impl;

import com.pawlift.dao.ApplicationDAO;
import com.pawlift.dao.BaseDAO;
import com.pawlift.exception.DataAccessException;
import com.pawlift.model.AdoptionApplication;
import com.pawlift.model.ApplicationStatus;
import com.pawlift.model.Pet;
import com.pawlift.model.PetStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ApplicationDAOImpl extends BaseDAO implements ApplicationDAO {

    private static final String SELECT =
            "SELECT a.id, a.pet_id, a.adopter_id, u.name AS adopter_name, u.email AS adopter_email, "
                    + "a.housing, a.experience, a.note, a.status, a.created_at, "
                    + "p.name AS pet_name, p.breed AS pet_breed, p.species AS pet_species, "
                    + "p.image_url AS pet_image_url, p.image_key AS pet_image_key, "
                    + "p.adoption_status AS pet_adoption_status, p.shelter_id AS pet_shelter_id, "
                    + "s.name AS shelter_name "
                    + "FROM adoption_applications a "
                    + "JOIN users u ON u.id = a.adopter_id "
                    + "JOIN pets p ON p.id = a.pet_id "
                    + "JOIN shelters s ON s.id = p.shelter_id ";

    private static AdoptionApplication map(ResultSet rs) throws SQLException {
        AdoptionApplication a = new AdoptionApplication();
        a.id = rs.getString("id");
        a.petId = rs.getString("pet_id");
        a.adopterId = rs.getString("adopter_id");
        a.adopterName = rs.getString("adopter_name");
        a.adopterEmail = rs.getString("adopter_email");
        a.housing = rs.getString("housing");
        a.experience = rs.getString("experience");
        a.note = rs.getString("note");
        a.status = ApplicationStatus.from(rs.getString("status"));
        a.createdAt = ts(rs, "created_at");
        Pet p = new Pet();
        p.id = a.petId;
        p.name = rs.getString("pet_name");
        p.breed = rs.getString("pet_breed");
        p.species = rs.getString("pet_species");
        p.imageUrl = rs.getString("pet_image_url");
        p.imageKey = rs.getString("pet_image_key");
        p.adoptionStatus = PetStatus.from(rs.getString("pet_adoption_status"));
        p.shelterId = rs.getString("pet_shelter_id");
        p.shelterName = rs.getString("shelter_name");
        a.pet = p;
        return a;
    }

    @Override
    public Optional<AdoptionApplication> findById(String id) {
        return one(SELECT + "WHERE a.id = ?", ApplicationDAOImpl::map, id);
    }

    @Override
    public List<AdoptionApplication> findByAdopter(String adopterId) {
        return list(SELECT + "WHERE a.adopter_id = ? ORDER BY a.created_at DESC", ApplicationDAOImpl::map, adopterId);
    }

    @Override
    public List<AdoptionApplication> findByShelter(String shelterId) {
        return list(SELECT + "WHERE p.shelter_id = ? ORDER BY a.created_at DESC", ApplicationDAOImpl::map, shelterId);
    }

    @Override
    public List<AdoptionApplication> findAll() {
        return list(SELECT + "ORDER BY a.created_at DESC", ApplicationDAOImpl::map);
    }

    @Override
    public boolean exists(String petId, String adopterId) {
        return count("SELECT COUNT(*) FROM adoption_applications WHERE pet_id = ? AND adopter_id = ?",
                petId, adopterId) > 0;
    }

    @Override
    public AdoptionApplication create(AdoptionApplication app) {
        if (app.id == null || app.id.isBlank()) app.id = UUID.randomUUID().toString();
        ApplicationStatus status = app.status == null ? ApplicationStatus.PENDING : app.status;
        // UNIQUE (pet_id, adopter_id) -> DuplicateKeyException via BaseDAO.update
        update("INSERT INTO adoption_applications (id, pet_id, adopter_id, housing, experience, note, status) "
                        + "VALUES (?,?,?,?,?,?,?)",
                app.id, app.petId, app.adopterId, app.housing,
                app.experience == null ? "" : app.experience,
                app.note == null ? "" : app.note, status.value());
        return findById(app.id).orElse(app);
    }

    /**
     * Updates the application status in a transaction. Approving an application
     * also moves an available pet to "pending" so both rows change together.
     */
    @Override
    public boolean updateStatus(String id, ApplicationStatus status) {
        try (Connection c = ds().getConnection()) {
            boolean auto = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                int rows;
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE adoption_applications SET status = ? WHERE id = ?")) {
                    bind(ps, status.value(), id);
                    rows = ps.executeUpdate();
                }
                if (rows > 0 && status == ApplicationStatus.APPROVED) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE pets SET adoption_status = 'pending' WHERE adoption_status = 'available' "
                                    + "AND id = (SELECT pet_id FROM adoption_applications WHERE id = ?)")) {
                        bind(ps, id);
                        ps.executeUpdate();
                    }
                }
                c.commit();
                return rows > 0;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(auto);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Status update failed", e);
        }
    }

    @Override
    public int count() {
        return count("SELECT COUNT(*) FROM adoption_applications");
    }

    @Override
    public int countByStatus(ApplicationStatus status) {
        return count("SELECT COUNT(*) FROM adoption_applications WHERE status = ?", status.value());
    }
}
