package com.danir.libraryAPI.services;

import com.danir.libraryAPI.dto.BookDTO;
import com.danir.libraryAPI.models.Book;
import com.danir.libraryAPI.models.BorrowedBook;
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

import java.time.Duration;
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
    private final NotificationService notificationService;
    private final BorrowedBookService borrowedBookService;

    @Autowired
    public BookService(BookRepository bookRepository, PeopleRepository peopleRepository, ModelMapper modelMapper, NotificationService notificationService, BorrowedBookService borrowedBookService) {
        this.bookRepository = bookRepository;
        this.peopleRepository = peopleRepository;
        this.modelMapper = modelMapper;
        this.notificationService = notificationService;
        this.borrowedBookService = borrowedBookService;
    }

    public List<Book> findAll() {
        log.info("Fetching all books");
        return bookRepository.findAll();
    }

    public Book findOne(int id) {
        log.info("Fetching book with ID: {}", id);
        return bookRepository.findById(id).orElse(null);
    }

    @Transactional
    public void save(Book book) {
        log.info("Saving new book: {}", book.getName());
        bookRepository.save(book);
    }

    @Transactional
    public void update(int id, Book book) {
        log.info("Updating book with ID: {}", id);
        Book existingBook = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Book with id " + id + " not found"));

        existingBook.setName(book.getName());
        existingBook.setAuthor(book.getAuthor());
        existingBook.setYear(book.getYear());

        bookRepository.save(existingBook);
    }

    @Transactional
    public void delete(long id) {
        log.warn("Deleting book with ID: {}", id);
        bookRepository.deleteById((int) id);
    }

    @Transactional
    public void savePersonWithBook(Person person, Book book) {
        log.info("Assigning book '{}' to person '{}'", book.getName(), person.getFullName());
        book.setPerson(person);
        book.setBorrowedDate(OffsetDateTime.now());

        bookRepository.save(book);
    }

    @Transactional
    public void release(Book book, int id) {
        log.info("Releasing book with ID: {}", id);
        if (book == null) {
            log.error("Attempted to release a null book");
            throw new IllegalArgumentException("Book cannot be null");
        }

        if (book.getBookId() != id) {
            log.error("Mismatch between provided ID and book's ID");
            throw new IllegalArgumentException("The provided ID does not match the book's ID");
        }

        Person person = book.getPerson();

        if (person != null) {
            // add book like borrowed before
            BorrowedBook borrowedBook = new BorrowedBook();
            borrowedBook.setPerson(person);
            borrowedBook.setBook(book);

            // save the borrowed book to the repository
            borrowedBookService.save(borrowedBook);

            // add book to user's borrowing history
            person.getBorrowedBeforeBooks().add(borrowedBook);
        }

        book.setPerson(null);
        book.setBorrowedDate(null);
        book.setOverdue(false);
        book.setIsDebtPaid(false);
        book.setPaymentDate(null);

        // save changes
        bookRepository.save(book);

        notificationService.notifyBookReleased(book);
    }


    public Page<Book> findAll(PageRequest pageRequest) {
        log.info("Fetching all books from dataBase");
        return bookRepository.findAll(pageRequest);
    }

    public List<Book> searchBooksByTitle(String title) {
        log.info("Searching book with title {}", title);
        return bookRepository.findByTitleStartingWith(title);
    }

    @Transactional
    public String reserveBook(int bookId, int personId) {
        log.info("Reserving book ID {} for person ID {}", bookId, personId);
        Book book = findBookById(bookId);

        if (book.getReservedBy() != null) {
            log.error("Book is already reserved");
            return "Book is already reserved";
        }

        Person person = peopleRepository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("Person not found"));

        book.setReservedBy(person);
        bookRepository.save(book);

        log.info("Reserved book successfully for person with id: {}", personId);
        return null;
    }

    @Transactional
    public void cancelReservation(int bookId) {
        log.info("Canceling reservation for book ID: {}", bookId);
        Book book = findBookById(bookId);
        book.setReservedBy(null);

        bookRepository.save(book);
    }

    public Page<Book> getAvailableBooks(PageRequest pageRequest) {
        log.info("Fetching only available books");
        return bookRepository.findByPersonIsNullAndReservedByIsNull(pageRequest);
    }

    public BookDTO getBookDetails(int id) {
        log.info("Getting details of the book with id: {}", id);
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

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void updateOverdueBooks() {
        log.info("Updating overdue books");

        List<Book> books = bookRepository.findAll()
                .stream()
                .filter(book -> book.getBorrowedDate() != null)
                .toList();

        for (Book book: books) {
            book.setDebt(calculateDebt(book));
        }

        bookRepository.updateOverdueBooks();
    }

    public boolean isOverdue(Book book) {
        log.info("Checking if the book with id: {} is overdue and changing flag if needs", book.getBookId());
        OffsetDateTime date = book.getBorrowedDate();
        return date != null && date.isBefore(OffsetDateTime.now().minusDays(10));
    }

    @Transactional
    public double calculateDebt(Book book) {
        log.info("Calculating debt for book: {}", book.getName());
        if (book.getIsDebtPaid() == null){
            return 0.0;
        }

        if (Boolean.TRUE.equals(book.getIsDebtPaid())) {
            return 0.0;
        }

        OffsetDateTime borrowedDate = book.getBorrowedDate();

        if (borrowedDate == null || borrowedDate.isAfter(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Invalid borrowed date.");
        }

        long overdueDays = Duration.between(borrowedDate, OffsetDateTime.now()).toDays();

        double amount;
        amount = overdueDays > 10
                ? 10 + (overdueDays - 10) * 5.0
                : overdueDays * 1.0;
        book.setDebt(amount);

        return amount;
    }

    @Transactional
    public double calculateTotalDebt(Person person) {
        return person.getBookList().stream()
                .mapToDouble(this::calculateDebt)
                .sum();
    }

    private Book findBookById(int bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found"));
    }

    public Book convertToBook(BookDTO bookDTO) {
        return modelMapper.map(bookDTO, Book.class);
    }

    public BookDTO convertToBookDTO(Book book) {
        return modelMapper.map(book, BookDTO.class);
    }
}
