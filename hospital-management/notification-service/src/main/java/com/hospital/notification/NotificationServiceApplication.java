package com.hospital.notification;

import lombok.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}

@Data @NoArgsConstructor @AllArgsConstructor
class AppointmentEvent {
    private Long appointmentId;
    private Long patientId;
    private Long doctorId;
    private String patientName;
    private String doctorName;
    private String appointmentDate;
    private String appointmentTime;
    private String reason;
    private String eventType;
}

@Component
class NotificationConsumer {

    @KafkaListener(topics = "appointment.booked", groupId = "notification-group")
    public void handleAppointmentBooked(AppointmentEvent event) {
        System.out.println("===========================================");
        System.out.println("📧 NOTIFICATION SERVICE — Appointment Booked");
        System.out.println("===========================================");
        System.out.println("✅ Appointment ID  : " + event.getAppointmentId());
        System.out.println("👤 Patient         : " + event.getPatientName());
        System.out.println("👨‍⚕️ Doctor          : " + event.getDoctorName());
        System.out.println("📅 Date            : " + event.getAppointmentDate());
        System.out.println("⏰ Time            : " + event.getAppointmentTime());
        System.out.println("📋 Reason          : " + event.getReason());
        System.out.println("-------------------------------------------");
        System.out.println("📱 SMS sent to patient: Appointment confirmed!");
        System.out.println("📧 Email sent to doctor: New appointment scheduled.");
        System.out.println("===========================================\n");
    }

    @KafkaListener(topics = "appointment.cancelled", groupId = "notification-group")
    public void handleAppointmentCancelled(AppointmentEvent event) {
        System.out.println("===========================================");
        System.out.println("📧 NOTIFICATION SERVICE — Appointment Cancelled");
        System.out.println("===========================================");
        System.out.println("❌ Appointment ID  : " + event.getAppointmentId());
        System.out.println("👤 Patient ID      : " + event.getPatientId());
        System.out.println("👨‍⚕️ Doctor ID       : " + event.getDoctorId());
        System.out.println("-------------------------------------------");
        System.out.println("📱 SMS sent to patient: Your appointment has been cancelled.");
        System.out.println("===========================================\n");
    }
}
