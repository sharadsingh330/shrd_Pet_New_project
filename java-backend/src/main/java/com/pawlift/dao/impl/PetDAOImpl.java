package com.pawlift.dao.impl;

import com.pawlift.dao.BaseDAO;
import com.pawlift.dao.PetDAO;
import com.pawlift.model.ListingStatus;
import com.pawlift.model.Pet;
import com.pawlift.model.PetStatus;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PetDAOImpl extends BaseDAO implements PetDAO {

    private static final String SELECT =
            "SELECT p.id, p.shelter_id, s.name AS shelter_name, p.name, p.species, p.breed, p.age_label, "
                    + "p.size, p.temperament, p.city, p.description, p.image_url, p.image_key, p.match_score, "
                    + "p.adoption_status, p.listing_status, p.created_at "
                    + "FROM pets p JOIN shelters s ON s.id = p.shelter_id ";

    private static Pet map(ResultSet rs) throws SQLException {
        Pet p = new Pet();
        p.id = rs.getString("id");
        p.shelterId = rs.getString("shelter_id");
        p.shelterName = rs.getString("shelter_name");
        p.name = rs.getString("name");
        p.species = rs.getString("species");
        p.breed = rs.getString("breed");
        p.ageLabel = rs.getString("age_label");
        p.size = rs.getString("size");
        p.temperament = rs.getString("temperament");
        p.city = rs.getString("city");
        p.description = rs.getString("description");
        p.imageUrl = rs.getString("image_url");
        p.imageKey = rs.getString("image_key");
        p.matchScore = rs.getInt("match_score");
        p.adoptionStatus = PetStatus.from(rs.getString("adoption_status"));
        p.listingStatus = ListingStatus.from(rs.getString("listing_status"));
        p.createdAt = ts(rs, "created_at");
        return p;
    }

    private static String nz(String v) {
        return v == null ? "" : v;
    }

    @Override
    public List<Pet> findPublic(String q, String species, String city) {
        StringBuilder sql = new StringBuilder(SELECT).append("WHERE p.listing_status = 'approved'");
        List<Object> params = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            sql.append(" AND (p.name LIKE ? OR p.breed LIKE ? OR p.city LIKE ?)");
            String like = "%" + q.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (species != null && !species.isBlank()) {
            sql.append(" AND p.species = ?");
            params.add(species.trim());
        }
        if (city != null && !city.isBlank()) {
            sql.append(" AND p.city LIKE ?");
            params.add("%" + city.trim() + "%");
        }
        sql.append(" ORDER BY p.created_at DESC");
        return list(sql.toString(), PetDAOImpl::map, params.toArray());
    }

    @Override
    public Optional<Pet> findById(String id) {
        return one(SELECT + "WHERE p.id = ?", PetDAOImpl::map, id);
    }

    @Override
    public List<Pet> findByShelter(String shelterId) {
        return list(SELECT + "WHERE p.shelter_id = ? ORDER BY p.created_at DESC", PetDAOImpl::map, shelterId);
    }

    @Override
    public List<Pet> findAll() {
        return list(SELECT + "ORDER BY p.created_at DESC", PetDAOImpl::map);
    }

    @Override
    public Pet create(Pet pet) {
        if (pet.id == null || pet.id.isBlank()) pet.id = UUID.randomUUID().toString();
        PetStatus adoption = pet.adoptionStatus == null ? PetStatus.AVAILABLE : pet.adoptionStatus;
        ListingStatus listing = pet.listingStatus == null ? ListingStatus.PENDING : pet.listingStatus;
        update("INSERT INTO pets (id, shelter_id, name, species, breed, age_label, size, temperament, city, "
                        + "description, image_url, image_key, match_score, adoption_status, listing_status) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                pet.id, pet.shelterId, pet.name, pet.species, nz(pet.breed), nz(pet.ageLabel), nz(pet.size),
                nz(pet.temperament), nz(pet.city), nz(pet.description), pet.imageUrl,
                pet.imageKey == null || pet.imageKey.isBlank() ? "dog" : pet.imageKey,
                pet.matchScore, adoption.value(), listing.value());
        return findById(pet.id).orElse(pet);
    }

    @Override
    public boolean update(Pet pet) {
        // shelter_id is intentionally not updatable: ownership never moves between shelters.
        return update("UPDATE pets SET name = ?, species = ?, breed = ?, age_label = ?, size = ?, temperament = ?, "
                        + "city = ?, description = ?, image_url = ?, image_key = ?, match_score = ?, "
                        + "adoption_status = ?, listing_status = ? WHERE id = ?",
                pet.name, pet.species, nz(pet.breed), nz(pet.ageLabel), nz(pet.size), nz(pet.temperament),
                nz(pet.city), nz(pet.description), pet.imageUrl,
                pet.imageKey == null || pet.imageKey.isBlank() ? "dog" : pet.imageKey,
                pet.matchScore,
                (pet.adoptionStatus == null ? PetStatus.AVAILABLE : pet.adoptionStatus).value(),
                (pet.listingStatus == null ? ListingStatus.PENDING : pet.listingStatus).value(),
                pet.id) > 0;
    }

    @Override
    public boolean delete(String id) {
        // applications and their messages are removed by ON DELETE CASCADE
        return update("DELETE FROM pets WHERE id = ?", id) > 0;
    }

    @Override
    public int count() {
        return count("SELECT COUNT(*) FROM pets");
    }

    @Override
    public int countByListingStatus(ListingStatus status) {
        return count("SELECT COUNT(*) FROM pets WHERE listing_status = ?", status.value());
    }

    @Override
    public int countByAdoptionStatus(PetStatus status) {
        return count("SELECT COUNT(*) FROM pets WHERE adoption_status = ?", status.value());
    }
}
