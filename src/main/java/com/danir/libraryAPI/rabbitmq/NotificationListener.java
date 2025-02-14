package com.danir.libraryAPI.rabbitmq;

import com.danir.libraryAPI.email.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationListener {

    private final EmailService emailService;

    public NotificationListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void receiveMessage(NotificationMessage notificationMessage) {
        log.info("Received message from RabbitMQ: {}", notificationMessage);

        // Отправка email
        emailService.sendEmail(
                notificationMessage.getToEmail(),
                notificationMessage.getSubject(),
                notificationMessage.getMessage()
        );
    }
}
