package com.hospital.patient.repository;

import com.hospital.patient.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    List<Patient> findByActiveTrue();

    Optional<Patient> findByEmail(String email);

    Optional<Patient> findByPhone(String phone);

    @Query("SELECT p FROM Patient p WHERE p.active = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Patient> searchPatients(String keyword);

    List<Patient> findByBloodGroup(String bloodGroup);
}
