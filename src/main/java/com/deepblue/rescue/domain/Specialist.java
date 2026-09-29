package com.deepblue.rescue.domain;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "specialists")
public class Specialist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String professionalCode;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private Boolean active;

    @ManyToMany
    @JoinTable(
        name = "specialist_expertise",
        joinColumns = @JoinColumn(name = "specialist_id"),
        inverseJoinColumns = @JoinColumn(name = "expertise_id")
    )
    private Set<Expertise> expertiseAreas = new HashSet<>();

    @OneToMany(mappedBy = "specialist", fetch = FetchType.LAZY)
    private List<Treatment> treatments = new ArrayList<>();

    public Specialist() {
    }

    public Long getId() {
        return id;
    }

    public String getProfessionalCode() {
        return professionalCode;
    }

    public void setProfessionalCode(String professionalCode) {
        this.professionalCode = professionalCode;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Set<Expertise> getExpertiseAreas() {
        return expertiseAreas;
    }

    public void setExpertiseAreas(Set<Expertise> expertiseAreas) {
        this.expertiseAreas = expertiseAreas;
    }

    public List<Treatment> getTreatments() {
        return treatments;
    }

    public void setTreatments(List<Treatment> treatments) {
        this.treatments = treatments;
    }

    public void addExpertise(Expertise expertise) {
        if (expertise != null && !expertiseAreas.contains(expertise)) {
            expertiseAreas.add(expertise);

            if (!expertise.getSpecialists().contains(this)) {
                expertise.getSpecialists().add(this);
            }
        }
    }
}