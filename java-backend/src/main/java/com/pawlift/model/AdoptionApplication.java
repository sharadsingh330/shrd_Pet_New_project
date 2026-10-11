package com.pawlift.model;

public class AdoptionApplication {
    public String id;
    public String petId;
    public String adopterId;
    public String adopterName;
    public String adopterEmail;
    public String housing;
    public String experience;
    public String note;
    public ApplicationStatus status;
    public String createdAt;
    /** Summary of the pet (id, name, breed, species, image, adoption status, shelter). */
    public Pet pet;
}
