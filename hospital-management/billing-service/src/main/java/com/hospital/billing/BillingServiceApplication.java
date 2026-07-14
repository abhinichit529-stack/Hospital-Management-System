package com.hospital.billing;

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
public class BillingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BillingServiceApplication.class, args);
    }
}

@Entity @Table(name = "bills")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
class Bill {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotNull private Long appointmentId;
    @NotNull private Long patientId;
    private String patientName;
    private double consultationFee;
    private double medicationFee;
    private double labFee;
    private double totalAmount;
    @Enumerated(EnumType.STRING) private PaymentStatus paymentStatus = PaymentStatus.PENDING;
    @Enumerated(EnumType.STRING) private PaymentMethod paymentMethod;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    @PrePersist public void prePersist() { createdAt = LocalDateTime.now(); }

    public enum PaymentStatus { PENDING, PAID, CANCELLED, REFUNDED }
    public enum PaymentMethod { CASH, CARD, UPI, INSURANCE }
}

@Data @NoArgsConstructor @AllArgsConstructor @Builder
class BillDTO {
    private Long id;
    @NotNull private Long appointmentId;
    @NotNull private Long patientId;
    private String patientName;
    private double consultationFee;
    private double medicationFee;
    private double labFee;
    private double totalAmount;
    private Bill.PaymentStatus paymentStatus;
    private Bill.PaymentMethod paymentMethod;
}

interface BillRepository extends JpaRepository<Bill, Long> {
    List<Bill> findByPatientId(Long patientId);
    List<Bill> findByPaymentStatus(Bill.PaymentStatus status);
    Optional<Bill> findByAppointmentId(Long appointmentId);
}

@Service @RequiredArgsConstructor @Transactional
class BillingService {
    private final BillRepository repository;

    public BillDTO generateBill(BillDTO dto) {
        double total = dto.getConsultationFee() + dto.getMedicationFee() + dto.getLabFee();
        Bill bill = Bill.builder().appointmentId(dto.getAppointmentId()).patientId(dto.getPatientId())
                .patientName(dto.getPatientName()).consultationFee(dto.getConsultationFee())
                .medicationFee(dto.getMedicationFee()).labFee(dto.getLabFee()).totalAmount(total)
                .paymentStatus(Bill.PaymentStatus.PENDING).build();
        return toDTO(repository.save(bill));
    }

    @Transactional(readOnly = true)
    public BillDTO getById(Long id) {
        return toDTO(repository.findById(id).orElseThrow(() -> new RuntimeException("Bill not found: " + id)));
    }

    @Transactional(readOnly = true)
    public List<BillDTO> getByPatient(Long patientId) {
        return repository.findByPatientId(patientId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public BillDTO processPayment(Long id, Bill.PaymentMethod method) {
        Bill bill = repository.findById(id).orElseThrow(() -> new RuntimeException("Bill not found: " + id));
        if (bill.getPaymentStatus() == Bill.PaymentStatus.PAID)
            throw new RuntimeException("Bill already paid");
        bill.setPaymentStatus(Bill.PaymentStatus.PAID);
        bill.setPaymentMethod(method);
        bill.setPaidAt(LocalDateTime.now());
        return toDTO(repository.save(bill));
    }

    private BillDTO toDTO(Bill b) {
        return BillDTO.builder().id(b.getId()).appointmentId(b.getAppointmentId())
                .patientId(b.getPatientId()).patientName(b.getPatientName())
                .consultationFee(b.getConsultationFee()).medicationFee(b.getMedicationFee())
                .labFee(b.getLabFee()).totalAmount(b.getTotalAmount())
                .paymentStatus(b.getPaymentStatus()).paymentMethod(b.getPaymentMethod()).build();
    }
}

@RestController @RequestMapping("/api/billing")
@RequiredArgsConstructor @Tag(name = "Billing Management")
class BillingController {
    private final BillingService service;

    @PostMapping @Operation(summary = "Generate a bill for appointment")
    public ResponseEntity<BillDTO> generate(@Valid @RequestBody BillDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.generateBill(dto));
    }

    @GetMapping("/{id}") @Operation(summary = "Get bill by ID")
    public ResponseEntity<BillDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/patient/{patientId}") @Operation(summary = "Get bills by patient")
    public ResponseEntity<List<BillDTO>> getByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(service.getByPatient(patientId));
    }

    @PatchMapping("/{id}/pay") @Operation(summary = "Process payment for a bill")
    public ResponseEntity<BillDTO> pay(@PathVariable Long id, @RequestParam Bill.PaymentMethod method) {
        return ResponseEntity.ok(service.processPayment(id, method));
    }
}

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handle(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
