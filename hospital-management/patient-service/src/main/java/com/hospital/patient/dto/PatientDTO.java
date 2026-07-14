package com.hospital.patient.dto;

import com.hospital.patient.model.Patient.Gender;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientDTO {
    private Long id;

    @NotBlank(message = "Name is required")
    private String name;

    @Min(0) @Max(150)
    private int age;

    private Gender gender;

    @Pattern(regexp = "^[6-9][0-9]{9}$", message = "Invalid phone number")
    private String phone;

    @Email
    private String email;

    private String address;
    private String bloodGroup;
    private String medicalHistory;
    private boolean active;
}
