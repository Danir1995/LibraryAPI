package com.danir.libraryAPI.services;

import com.danir.libraryAPI.rabbitmq.NotificationMessage;
import com.danir.libraryAPI.models.Book;
import com.danir.libraryAPI.rabbitmq.NotificationPublisher;
import com.danir.libraryAPI.repositories.BookRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;


import java.time.OffsetDateTime;

@Service
@Slf4j
public class NotificationService {

    private final NotificationPublisher notificationPublisher;
    private final BookRepository bookRepository;

    public NotificationService(BookRepository bookRepository, NotificationPublisher notificationPublisher) {
        this.bookRepository = bookRepository;
        this.notificationPublisher = notificationPublisher;
    }

    public void notifyBookReleased(Book book) {
        if (book.getReservedBy() != null) {
            String email = book.getReservedBy().getEmail();
            String fullName = book.getReservedBy().getFullName();
            String subject = "The book: " + book.getName() + " is free";
            String message = "Hello, dear " + fullName + ". The book '" + book.getName() + "', reserved by you, is now free.";

            NotificationMessage notificationMessage = new NotificationMessage(email, subject, message);
            notificationPublisher.sendOverdueNotification(notificationMessage);
        }
    }


    @Scheduled(cron = "0 0 12 * * ?")
    public void sendOverdueNotifications() {
        OffsetDateTime tenDaysAgo = OffsetDateTime.now().minusDays(10);
        PageRequest pageRequest = PageRequest.of(0, 50);

        Page<Book> overdueBooksPage = bookRepository.findOverdueBooks(tenDaysAgo, pageRequest);
        while (overdueBooksPage.hasContent()) {
            for (Book book : overdueBooksPage.getContent()) {
                if (book.getPerson() != null && book.getPerson().getEmail() != null) {
                    try {
                        String email = book.getPerson().getEmail();
                        String fullName = book.getPerson().getFullName();
                        String subject = "Overdue Book Notification";
                        String message = "Hello, dear " + fullName + ". You have an overdue book: " + book.getName() + ". Please return it.";

                        NotificationMessage notificationMessage = new NotificationMessage(email, subject, message);
                        notificationPublisher.sendOverdueNotification(notificationMessage);

                        log.info("Notification sent to RabbitMQ for overdue book: {}", book.getName());
                    } catch (Exception e) {
                        log.error("Failed to send message to RabbitMQ for book: {}. Error: {}", book.getName(), e.getMessage());
                    }
                }
            }
            pageRequest = pageRequest.next();
            overdueBooksPage = bookRepository.findOverdueBooks(tenDaysAgo, pageRequest);
        }
    }
}

