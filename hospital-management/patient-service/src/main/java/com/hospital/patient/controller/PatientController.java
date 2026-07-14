package com.hospital.patient.controller;

import com.hospital.patient.dto.PatientDTO;
import com.hospital.patient.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@Tag(name = "Patient Management", description = "APIs for managing hospital patients")
public class PatientController {

    private final PatientService patientService;

    @PostMapping
    @Operation(summary = "Register a new patient")
    public ResponseEntity<PatientDTO> registerPatient(@Valid @RequestBody PatientDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(patientService.registerPatient(dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get patient by ID")
    public ResponseEntity<PatientDTO> getPatient(@PathVariable Long id) {
        return ResponseEntity.ok(patientService.getPatientById(id));
    }

    @GetMapping
    @Operation(summary = "Get all active patients")
    public ResponseEntity<List<PatientDTO>> getAllPatients() {
        return ResponseEntity.ok(patientService.getAllActivePatients());
    }

    @GetMapping("/search")
    @Operation(summary = "Search patients by name or email")
    public ResponseEntity<List<PatientDTO>> searchPatients(@RequestParam String keyword) {
        return ResponseEntity.ok(patientService.searchPatients(keyword));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update patient details")
    public ResponseEntity<PatientDTO> updatePatient(@PathVariable Long id,
                                                     @Valid @RequestBody PatientDTO dto) {
        return ResponseEntity.ok(patientService.updatePatient(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete patient")
    public ResponseEntity<String> deletePatient(@PathVariable Long id) {
        patientService.softDeletePatient(id);
        return ResponseEntity.ok("Patient deactivated successfully");
    }
}
