package com.deepblue.rescue.domain;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "animals")
public class Animal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String animalCode;

    @Column(nullable = false, length = 100)
    private String commonName;

    @Column(nullable = false, length = 150)
    private String scientificName;

    @Column(name = "tracking_device_code", length = 50, unique = true) //temporal para error
private String trackingDeviceCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnimalSex sex;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rescue_case_id", nullable = false, unique = true)
    private RescueCase rescueCase;

    @OneToOne(
        mappedBy = "animal",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    private MedicalRecord medicalRecord;

    @OneToMany(mappedBy = "animal", fetch = FetchType.LAZY)
    private List<Treatment> treatments = new ArrayList<>();

    public Animal() {
    }

    public Long getId() {
        return id;
    }

    public String getAnimalCode() {
        return animalCode;
    }

    public void setAnimalCode(String animalCode) {
        this.animalCode = animalCode;
    }

    public String getCommonName() {
        return commonName;
    }

    public String getTrackingDeviceCode() {
    return trackingDeviceCode;
}

public void setTrackingDeviceCode(String trackingDeviceCode) {
    this.trackingDeviceCode = trackingDeviceCode;
}

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public String getScientificName() {
        return scientificName;
    }

    public void setScientificName(String scientificName) {
        this.scientificName = scientificName;
    }

    public AnimalSex getSex() {
        return sex;
    }

    public void setSex(AnimalSex sex) {
        this.sex = sex;
    }

    public RescueCase getRescueCase() {
        return rescueCase;
    }

    public void setRescueCase(RescueCase rescueCase) {
        this.rescueCase = rescueCase;
    }

    public MedicalRecord getMedicalRecord() {
        return medicalRecord;
    }

    public void setMedicalRecord(MedicalRecord medicalRecord) {
        this.medicalRecord = medicalRecord;
    }

    public void assignMedicalRecord(MedicalRecord medicalRecord) {
        this.medicalRecord = medicalRecord;

        if (medicalRecord != null && medicalRecord.getAnimal() != this) {
            medicalRecord.setAnimal(this);
        }
    }

    public List<Treatment> getTreatments() {
        return treatments;
    }

    public void setTreatments(List<Treatment> treatments) {
        this.treatments = treatments;
    }
}