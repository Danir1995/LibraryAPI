package com.danir.libraryAPI.services;

import com.danir.libraryAPI.dto.BookDTO;
import com.danir.libraryAPI.email.EmailService;
import com.danir.libraryAPI.models.Book;
import com.danir.libraryAPI.models.Person;
import com.danir.libraryAPI.repositories.BookRepository;
import com.danir.libraryAPI.repositories.PeopleRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
@Slf4j
@EnableScheduling
public class BookService {

    private final BookRepository bookRepository;
    private final PeopleRepository peopleRepository;
    private final ModelMapper modelMapper;
    private final EmailService emailService;

    @Autowired
    public BookService(BookRepository bookRepository, PeopleRepository peopleRepository, ModelMapper modelMapper, EmailService emailService) {
        this.bookRepository = bookRepository;
        this.peopleRepository = peopleRepository;
        this.modelMapper = modelMapper;
        this.emailService = emailService;
    }

    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    public Book findOne(int id) {
        return bookRepository.findById(id).orElse(null);
    }

    @Transactional
    public void save(Book book) {
        bookRepository.save(book);
    }

    @Transactional
    public void update(int id, Book book) {
        book.setBookId(id);
        bookRepository.save(book);
    }

    @Transactional
    public void delete(long id) {
        bookRepository.deleteById((int) id);
    }

    @Transactional
    public void savePersonWithBook(Person person, Book book) {
        book.setPerson(person);
        book.setBorrowedDate(OffsetDateTime.now());
        bookRepository.save(book);
    }

    @Transactional
    public void release(Book book, int id) {
        book.setBookId(id);
        book.setPerson(null);
        book.setBorrowedDate(null);
        book.setOverdue(false);
        bookRepository.save(book);
    }

    public Page<Book> findAll(PageRequest pageRequest) {
        return bookRepository.findAll(pageRequest);
    }

    public List<Book> searchBooksByTitle(String title) {
        return bookRepository.findByTitleStartingWith(title);
    }

    @Transactional
    public String reserveBook(int bookId, int personId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found"));

        if (book.getReservedBy() != null) {
            return "Book is already reserved";
        }

        Person person = peopleRepository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("Person not found"));

        book.setReservedBy(person);
        bookRepository.save(book);
        return null;
    }

    @Transactional
    public void cancelReservation(int bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found"));

        book.setReservedBy(null);
        bookRepository.save(book);
    }

    public Page<Book> getAvailableBooks(PageRequest pageRequest) {
        return bookRepository.findByPersonIsNullAndReservedByIsNull(pageRequest);
    }

    public BookDTO getBookDetails(int id) {
        Book book = findOne(id);
        BookDTO bookDTO = convertToBookDTO(book);
        bookDTO.setOverdue(isOverdue(book));
        if (book.getPerson() != null) {
            bookDTO.setPerson_name(book.getPerson().getFullName());
        }
        if (book.getReservedBy() != null) {
            bookDTO.setReserved_by_name(book.getReservedBy().getFullName());
        }

        return bookDTO;
    }

    @Scheduled(cron = "0 1 0 * * ?")
    @Transactional
    public void updateOverdueBooks() {
        bookRepository.updateOverdueBooks();
    }

    public boolean isOverdue(Book book) {
        OffsetDateTime date = book.getBorrowedDate();
        return date != null && date.isBefore(OffsetDateTime.now().minusDays(10));
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

    public Book convertToBook(BookDTO bookDTO) {
        return modelMapper.map(bookDTO, Book.class);
    }

    public BookDTO convertToBookDTO(Book book) {
        return modelMapper.map(book, BookDTO.class);
    }
}
