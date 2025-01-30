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
        Book existingBook = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Book with id " + id + " not found"));

        existingBook.setName(book.getName());
        existingBook.setAuthor(book.getAuthor());
        existingBook.setYear(book.getYear());
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
        if (book == null) {
            throw new IllegalArgumentException("Book cannot be null");
        }

        if (book.getBookId() != id) {
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

    @Transactional
    public double calculateDebt(Book book) {
        if (book.getIsDebtPaid() == null){
            return 0.0;
        }
        if (Boolean.TRUE.equals(book.getIsDebtPaid())) {
            return 0.0; // Если долг уже оплачен, возвращаем 0
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

    public Book convertToBook(BookDTO bookDTO) {
        return modelMapper.map(bookDTO, Book.class);
    }

    public BookDTO convertToBookDTO(Book book) {
        return modelMapper.map(book, BookDTO.class);
    }
}
