package com.danir.libraryAPI.services;

import com.danir.libraryAPI.email.EmailService;
import com.danir.libraryAPI.models.Book;
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

    private final EmailService emailService;
    private final BookRepository bookRepository;

    public NotificationService(EmailService emailService, BookRepository bookRepository) {
        this.emailService = emailService;
        this.bookRepository = bookRepository;
    }

    public void notifyBookReleased(Book book) {
        if (book.getReservedBy() != null) {
            String email = book.getReservedBy().getEmail();
            String fullName = book.getReservedBy().getFullName();
            emailService.sendEmail(
                    email,
                    "The book: " + book.getName() + " is free",
                    "Hello, dear " + fullName + ". The book '" + book.getName() + "', reserved by you, is now free."
            );
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
                        emailService.sendEmail(
                                book.getPerson().getEmail(),
                                "Overdue Book Notification",
                                "Hello, dear " + book.getPerson().getFullName() + ". You have an overdue book: " + book.getName() + ". Please return it."
                        );
                        log.info("Notification sent for overdue book: {}", book.getName());
                    } catch (Exception e) {
                        log.error("Failed to send email for book: {}. Error: {}", book.getName(), e.getMessage());
                    }
                }
            }
            pageRequest = pageRequest.next();
            overdueBooksPage = bookRepository.findOverdueBooks(tenDaysAgo, pageRequest);
        }
    }

}

