package com.hospital.patient.service;

import com.hospital.patient.dto.PatientDTO;
import com.hospital.patient.model.Patient;
import com.hospital.patient.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PatientService {

    private final PatientRepository patientRepository;

    public PatientDTO registerPatient(PatientDTO dto) {
        if (patientRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new RuntimeException("Patient with email already exists: " + dto.getEmail());
        }
        Patient patient = mapToEntity(dto);
        Patient saved = patientRepository.save(patient);
        return mapToDTO(saved);
    }

    @Transactional(readOnly = true)
    public PatientDTO getPatientById(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient not found with id: " + id));
        return mapToDTO(patient);
    }

    @Transactional(readOnly = true)
    public List<PatientDTO> getAllActivePatients() {
        return patientRepository.findByActiveTrue()
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PatientDTO> searchPatients(String keyword) {
        return patientRepository.searchPatients(keyword)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public PatientDTO updatePatient(Long id, PatientDTO dto) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient not found with id: " + id));
        patient.setName(dto.getName());
        patient.setAge(dto.getAge());
        patient.setGender(dto.getGender());
        patient.setPhone(dto.getPhone());
        patient.setAddress(dto.getAddress());
        patient.setBloodGroup(dto.getBloodGroup());
        patient.setMedicalHistory(dto.getMedicalHistory());
        return mapToDTO(patientRepository.save(patient));
    }

    public void softDeletePatient(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient not found with id: " + id));
        patient.setActive(false);
        patientRepository.save(patient);
    }

    private Patient mapToEntity(PatientDTO dto) {
        return Patient.builder()
                .name(dto.getName()).age(dto.getAge()).gender(dto.getGender())
                .phone(dto.getPhone()).email(dto.getEmail()).address(dto.getAddress())
                .bloodGroup(dto.getBloodGroup()).medicalHistory(dto.getMedicalHistory())
                .active(true).build();
    }

    private PatientDTO mapToDTO(Patient p) {
        return PatientDTO.builder()
                .id(p.getId()).name(p.getName()).age(p.getAge()).gender(p.getGender())
                .phone(p.getPhone()).email(p.getEmail()).address(p.getAddress())
                .bloodGroup(p.getBloodGroup()).medicalHistory(p.getMedicalHistory())
                .active(p.isActive()).build();
    }
}
