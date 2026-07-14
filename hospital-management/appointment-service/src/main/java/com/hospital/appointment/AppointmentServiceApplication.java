package com.hospital.appointment;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

// ── Main ─────────────────────────────────────────────────────────────────────
@SpringBootApplication
class AppointmentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AppointmentServiceApplication.class, args);
    }
}

// ── Entity ───────────────────────────────────────────────────────────────────
@Entity
@Table(name = "appointments")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
class Appointment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull private Long patientId;
    @NotNull private Long doctorId;

    private String patientName;
    private String doctorName;
    private String doctorSpecialization;

    @NotNull private LocalDate appointmentDate;
    @NotNull private LocalTime appointmentTime;

    private String reason;

    @Enumerated(EnumType.STRING)
    private Status status = Status.SCHEDULED;

    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist public void prePersist() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  public void preUpdate()  { updatedAt = LocalDateTime.now(); }

    public enum Status { SCHEDULED, COMPLETED, CANCELLED, NO_SHOW }
}

// ── DTO ──────────────────────────────────────────────────────────────────────
@Data @NoArgsConstructor @AllArgsConstructor @Builder
class AppointmentDTO {
    private Long id;
    @NotNull private Long patientId;
    @NotNull private Long doctorId;
    private String patientName;
    private String doctorName;
    private String doctorSpecialization;
    @NotNull private LocalDate appointmentDate;
    @NotNull private LocalTime appointmentTime;
    private String reason;
    private Appointment.Status status;
    private String notes;
}

// ── Kafka Event ──────────────────────────────────────────────────────────────
@Data @NoArgsConstructor @AllArgsConstructor @Builder
class AppointmentEvent {
    private Long appointmentId;
    private Long patientId;
    private Long doctorId;
    private String patientName;
    private String doctorName;
    private String appointmentDate;
    private String appointmentTime;
    private String reason;
    private String eventType; // BOOKED or CANCELLED
}

// ── Repository ───────────────────────────────────────────────────────────────
interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatientId(Long patientId);
    List<Appointment> findByDoctorId(Long doctorId);
    List<Appointment> findByAppointmentDate(LocalDate date);
    List<Appointment> findByStatus(Appointment.Status status);
}

// ── Kafka Producer ───────────────────────────────────────────────────────────
@Component
@RequiredArgsConstructor
class AppointmentKafkaProducer {
    private final KafkaTemplate<String, AppointmentEvent> kafkaTemplate;

    @Value("${kafka.topics.appointment-booked}")
    private String bookedTopic;

    @Value("${kafka.topics.appointment-cancelled}")
    private String cancelledTopic;

    public void publishAppointmentBooked(AppointmentEvent event) {
        kafkaTemplate.send(bookedTopic, String.valueOf(event.getAppointmentId()), event);
        System.out.println("📤 Kafka event published [appointment.booked]: " + event.getAppointmentId());
    }

    public void publishAppointmentCancelled(AppointmentEvent event) {
        kafkaTemplate.send(cancelledTopic, String.valueOf(event.getAppointmentId()), event);
        System.out.println("📤 Kafka event published [appointment.cancelled]: " + event.getAppointmentId());
    }
}

// ── Service ──────────────────────────────────────────────────────────────────
@Service
@RequiredArgsConstructor
@Transactional
class AppointmentService {
    private final AppointmentRepository repository;
    private final AppointmentKafkaProducer kafkaProducer;

    public AppointmentDTO bookAppointment(AppointmentDTO dto) {
        Appointment appointment = Appointment.builder()
                .patientId(dto.getPatientId()).doctorId(dto.getDoctorId())
                .patientName(dto.getPatientName()).doctorName(dto.getDoctorName())
                .doctorSpecialization(dto.getDoctorSpecialization())
                .appointmentDate(dto.getAppointmentDate()).appointmentTime(dto.getAppointmentTime())
                .reason(dto.getReason()).status(Appointment.Status.SCHEDULED).build();

        Appointment saved = repository.save(appointment);

        // Publish Kafka event
        kafkaProducer.publishAppointmentBooked(AppointmentEvent.builder()
                .appointmentId(saved.getId()).patientId(saved.getPatientId())
                .doctorId(saved.getDoctorId()).patientName(saved.getPatientName())
                .doctorName(saved.getDoctorName())
                .appointmentDate(saved.getAppointmentDate().toString())
                .appointmentTime(saved.getAppointmentTime().toString())
                .reason(saved.getReason()).eventType("BOOKED").build());

        return mapToDTO(saved);
    }

    @Transactional(readOnly = true)
    public AppointmentDTO getById(Long id) {
        return mapToDTO(repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found: " + id)));
    }

    @Transactional(readOnly = true)
    public List<AppointmentDTO> getByPatient(Long patientId) {
        return repository.findByPatientId(patientId).stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AppointmentDTO> getByDoctor(Long doctorId) {
        return repository.findByDoctorId(doctorId).stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public AppointmentDTO updateStatus(Long id, Appointment.Status status) {
        Appointment a = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found: " + id));
        a.setStatus(status);
        return mapToDTO(repository.save(a));
    }

    public AppointmentDTO cancelAppointment(Long id) {
        Appointment a = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found: " + id));
        a.setStatus(Appointment.Status.CANCELLED);
        Appointment saved = repository.save(a);

        kafkaProducer.publishAppointmentCancelled(AppointmentEvent.builder()
                .appointmentId(saved.getId()).patientId(saved.getPatientId())
                .doctorId(saved.getDoctorId()).eventType("CANCELLED").build());

        return mapToDTO(saved);
    }

    private AppointmentDTO mapToDTO(Appointment a) {
        return AppointmentDTO.builder()
                .id(a.getId()).patientId(a.getPatientId()).doctorId(a.getDoctorId())
                .patientName(a.getPatientName()).doctorName(a.getDoctorName())
                .doctorSpecialization(a.getDoctorSpecialization())
                .appointmentDate(a.getAppointmentDate()).appointmentTime(a.getAppointmentTime())
                .reason(a.getReason()).status(a.getStatus()).notes(a.getNotes()).build();
    }
}

// ── Controller ───────────────────────────────────────────────────────────────
@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointment Management")
class AppointmentController {
    private final AppointmentService service;

    @PostMapping
    @Operation(summary = "Book a new appointment")
    public ResponseEntity<AppointmentDTO> book(@Valid @RequestBody AppointmentDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.bookAppointment(dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get appointment by ID")
    public ResponseEntity<AppointmentDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get appointments by patient")
    public ResponseEntity<List<AppointmentDTO>> getByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(service.getByPatient(patientId));
    }

    @GetMapping("/doctor/{doctorId}")
    @Operation(summary = "Get appointments by doctor")
    public ResponseEntity<List<AppointmentDTO>> getByDoctor(@PathVariable Long doctorId) {
        return ResponseEntity.ok(service.getByDoctor(doctorId));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update appointment status")
    public ResponseEntity<AppointmentDTO> updateStatus(@PathVariable Long id,
                                                        @RequestParam Appointment.Status status) {
        return ResponseEntity.ok(service.updateStatus(id, status));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel appointment")
    public ResponseEntity<AppointmentDTO> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(service.cancelAppointment(id));
    }
}

// ── Global Exception Handler ─────────────────────────────────────────────────
@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handle(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
