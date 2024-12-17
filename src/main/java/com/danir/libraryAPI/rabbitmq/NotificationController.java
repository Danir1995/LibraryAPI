package com.danir.libraryAPI.rabbitmq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
@Slf4j
public class NotificationController {

    NotificationPublisher notificationPublisher;

    public NotificationController(NotificationPublisher notificationPublisher) {
        this.notificationPublisher = notificationPublisher;
    }

    @GetMapping("/send-notification")
    public ResponseEntity<String> sendNotification(@RequestParam String message) {
        notificationPublisher.sendOverdueNotification(message);
        log.info("Message was sent to RabbitMQ: {}", message);
        return ResponseEntity.ok("Message sent successfully: " + message);
    }
}
