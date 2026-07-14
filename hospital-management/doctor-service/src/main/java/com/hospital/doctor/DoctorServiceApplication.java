package com.hospital.doctor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootApplication
public class DoctorServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DoctorServiceApplication.class, args);
    }
}

@Entity @Table(name = "doctors")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
class Doctor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotBlank private String name;
    @NotBlank private String specialization;
    @Email private String email;
    private String phone;
    private String qualification;
    private int experienceYears;
    private double consultationFee;
    private boolean available = true;
    private LocalDateTime createdAt;
    @PrePersist public void prePersist() { createdAt = LocalDateTime.now(); }
}

@Data @NoArgsConstructor @AllArgsConstructor @Builder
class DoctorDTO {
    private Long id;
    @NotBlank private String name;
    @NotBlank private String specialization;
    @Email private String email;
    private String phone;
    private String qualification;
    private int experienceYears;
    private double consultationFee;
    private boolean available;
}

interface DoctorRepository extends JpaRepository<Doctor, Long> {
    List<Doctor> findByAvailableTrue();
    List<Doctor> findBySpecializationIgnoreCase(String specialization);
    List<Doctor> findByAvailableTrueAndSpecializationIgnoreCase(String specialization);
}

@Service @RequiredArgsConstructor @Transactional
class DoctorService {
    private final DoctorRepository repository;

    public DoctorDTO addDoctor(DoctorDTO dto) {
        Doctor d = Doctor.builder().name(dto.getName()).specialization(dto.getSpecialization())
                .email(dto.getEmail()).phone(dto.getPhone()).qualification(dto.getQualification())
                .experienceYears(dto.getExperienceYears()).consultationFee(dto.getConsultationFee())
                .available(true).build();
        return toDTO(repository.save(d));
    }

    @Transactional(readOnly = true)
    public DoctorDTO getById(Long id) {
        return toDTO(repository.findById(id).orElseThrow(() -> new RuntimeException("Doctor not found: " + id)));
    }

    @Transactional(readOnly = true)
    public List<DoctorDTO> getAllAvailable() {
        return repository.findByAvailableTrue().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DoctorDTO> getBySpecialization(String spec) {
        return repository.findByAvailableTrueAndSpecializationIgnoreCase(spec)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public DoctorDTO updateDoctor(Long id, DoctorDTO dto) {
        Doctor d = repository.findById(id).orElseThrow(() -> new RuntimeException("Doctor not found: " + id));
        d.setName(dto.getName()); d.setSpecialization(dto.getSpecialization());
        d.setPhone(dto.getPhone()); d.setConsultationFee(dto.getConsultationFee());
        d.setExperienceYears(dto.getExperienceYears()); d.setAvailable(dto.isAvailable());
        return toDTO(repository.save(d));
    }

    private DoctorDTO toDTO(Doctor d) {
        return DoctorDTO.builder().id(d.getId()).name(d.getName()).specialization(d.getSpecialization())
                .email(d.getEmail()).phone(d.getPhone()).qualification(d.getQualification())
                .experienceYears(d.getExperienceYears()).consultationFee(d.getConsultationFee())
                .available(d.isAvailable()).build();
    }
}

@RestController @RequestMapping("/api/doctors")
@RequiredArgsConstructor @Tag(name = "Doctor Management")
class DoctorController {
    private final DoctorService service;

    @PostMapping @Operation(summary = "Add a new doctor")
    public ResponseEntity<DoctorDTO> add(@Valid @RequestBody DoctorDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addDoctor(dto));
    }

    @GetMapping("/{id}") @Operation(summary = "Get doctor by ID")
    public ResponseEntity<DoctorDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping @Operation(summary = "Get all available doctors")
    public ResponseEntity<List<DoctorDTO>> getAll() {
        return ResponseEntity.ok(service.getAllAvailable());
    }

    @GetMapping("/specialization/{spec}") @Operation(summary = "Get doctors by specialization")
    public ResponseEntity<List<DoctorDTO>> getBySpec(@PathVariable String spec) {
        return ResponseEntity.ok(service.getBySpecialization(spec));
    }

    @PutMapping("/{id}") @Operation(summary = "Update doctor details")
    public ResponseEntity<DoctorDTO> update(@PathVariable Long id, @Valid @RequestBody DoctorDTO dto) {
        return ResponseEntity.ok(service.updateDoctor(id, dto));
    }
}

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handle(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
