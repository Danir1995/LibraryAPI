package com.danir.libraryAPI.services;

import com.danir.libraryAPI.models.Book;
import com.danir.libraryAPI.models.Person;
import com.danir.libraryAPI.rabbitmq.NotificationMessage;
import com.danir.libraryAPI.rabbitmq.NotificationPublisher;
import com.danir.libraryAPI.repositories.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    private NotificationPublisher notificationPublisher;

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void notifyBookReleased_ShouldSendNotification_WhenBookIsReserved() {
        // Arrange
        Person reservedBy = new Person();
        reservedBy.setEmail("test@example.com");
        reservedBy.setFullName("Test User");

        Book book = new Book();
        book.setName("Test Book");
        book.setReservedBy(reservedBy);

        // Mocks
        doNothing().when(notificationPublisher).sendNotification(any(NotificationMessage.class));        // Act
        notificationService.notifyBookReleased(book);

        verify(notificationPublisher, times(1)).sendNotification(any(NotificationMessage.class));
        assertNotNull(reservedBy.getEmail());
        assertNotNull(reservedBy.getFullName());
    }

    @Test
    void sendOverdueNotifications_ShouldSendNotifications_WhenBooksAreOverdue() {
        // Arrange
        Person person = new Person();
        person.setEmail("test@example.com");
        person.setFullName("Test User");

        Book overdueBook = new Book();
        overdueBook.setName("Overdue Book");
        overdueBook.setPerson(person);

        Page<Book> overdueBooksPage = new PageImpl<>(List.of(overdueBook));

        when(bookRepository.findOverdueBooks(any(), any()))
                .thenReturn(overdueBooksPage)  // Первый вызов вернёт страницу с книгами
                .thenReturn(Page.empty());
        doNothing().when(notificationPublisher).sendNotification(any(NotificationMessage.class));
        // Act
        notificationService.sendOverdueNotifications();

        verify(bookRepository, atLeast(1)).findOverdueBooks(any(), any());
        verify(notificationPublisher, times(1)).sendNotification(any(NotificationMessage.class));
    }

}
