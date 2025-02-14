package com.danir.libraryAPI.services;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.danir.libraryAPI.models.Book;
import com.danir.libraryAPI.models.Person;
import com.danir.libraryAPI.repositories.BookRepository;
import com.danir.libraryAPI.repositories.PeopleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private PeopleRepository peopleRepository;

    @Mock
    private BorrowedBookService borrowedBookService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BookService bookService;

    private Book book;

    private Person person;

    @BeforeEach
    void setUp() {
        book = new Book();
        book.setBookId(1);
        book.setName("Test Book");
        book.setBorrowedDate(OffsetDateTime.now().minusDays(5));

        person = new Person();
        person.setPersonId(1);
        person.setFullName("John Doe");
    }

    @Test
    void testFindAll_ShouldReturnAllBooks() {
        List<Book> books = List.of(new Book(), new Book());
        when(bookRepository.findAll()).thenReturn(books);

        List<Book> result = bookService.findAll();
        assertEquals(2, result.size());
    }

    @Test
    void testFindOne_ShouldReturnBook() {
        when(bookRepository.findById(1)).thenReturn(Optional.of(book));
        Book foundBook = bookService.findOne(1);

        assertNotNull(foundBook);
        assertEquals("Test Book", foundBook.getName());
    }

    @Test
    void testFindOne_ShouldReturnNull_WhenBookNotFound() {
        when(bookRepository.findById(1)).thenReturn(Optional.empty());
        Book foundBook = bookService.findOne(1);

        assertNull(foundBook);
    }

    @Test
    void testSave_ShouldInvokeRepositorySave() {
        bookService.save(book);
        verify(bookRepository, times(1)).save(book);
    }

    @Test
    void testDelete_ShouldInvokeRepositoryDelete() {
        bookService.delete(1);
        verify(bookRepository, times(1)).deleteById(1);
    }

    @Test
    void testSavePersonWithBook_ShouldAssignBookToPerson() {
        when(bookRepository.save(book)).thenReturn(book);
        bookService.savePersonWithBook(person, book);

        assertEquals("John Doe" ,book.getPerson().getFullName());
        verify(bookRepository, times(1)).save(book);
    }

    @Test
    void testRelease_ShouldReleaseBookFromPerson(){
        when(bookRepository.save(book)).thenReturn(book);
        bookService.savePersonWithBook(person, book);
        bookService.release(book, 1);

        assertNull(book.getPerson(),"Book's person should be null after release");
        assertNull(book.getBorrowedDate());
        assertEquals("Test Book", person.getBorrowedBeforeBooks().get(0).getBook().getName());
    }

    @Test
    void testSearchBookByTitle_ShouldReturnFoundBooks(){
        List<Book> foundBooks = List.of(new Book(), book);
        when(bookRepository.findByTitleStartingWith("T")).thenReturn(foundBooks);
        List<Book> result = bookService.searchBooksByTitle("T");

        assertEquals(2, result.size(), "Expected 2 books in result");
        assertEquals(foundBooks, result, "Expected the same books in result");

        verify(bookRepository, times(1)).findByTitleStartingWith("T");
    }

    @Test
    void testReserveBook_ShouldReserveBookForPerson(){
        when(bookRepository.findById(1)).thenReturn(Optional.of(book));
        when(peopleRepository.findById(1)).thenReturn(Optional.of(person));

        bookService.reserveBook(1, 1);

        assertEquals(person, book.getReservedBy());
        verify(bookRepository, times(1)).save(book);
    }

    @Test
    void testReserveBook_ShouldReturnMessageIfAlreadyReserved(){

        when(bookRepository.findById(1)).thenReturn(Optional.of(book));
        book.setReservedBy(new Person());

        String result = bookService.reserveBook(1, 1);

        assertEquals("Book is already reserved", result);
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void testReserveBook_BookNotFound() {
        when(bookRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> bookService.reserveBook(99, 1));
    }

    @Test
    void testReserveBook_PersonNotFound() {
        when(bookRepository.findById(1)).thenReturn(Optional.of(book));
        when(peopleRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> bookService.reserveBook(1, 99));
    }

    @Test
    void testCancelReservation(){
        when(bookRepository.findById(1)).thenReturn(Optional.of(book));

        book.setReservedBy(new Person());
        bookService.cancelReservation(1);

        assertNull(book.getReservedBy());
        verify(bookRepository, times(1)).save(book);
    }

    @Test
    void testGetAvailableBooks_ShouldReturnAvailableBooks() {
        Book book1 = new Book();
        book1.setPerson(person);
        Book book2 = new Book();
        Book book3 = new Book();

        List<Book> books = List.of(book2, book3);
        Page<Book> bookPage = new PageImpl<>(books);
        PageRequest pageRequest = PageRequest.of(0, 20, Sort.by("year"));

        when(bookRepository.findByPersonIsNullAndReservedByIsNull(pageRequest))
                .thenReturn(bookPage);

        Page<Book> result = bookService.getAvailableBooks(pageRequest);
        assertEquals(2, result.getSize());
    }

    @Test
    void testIsOverdue_ShouldChangeFlagOfOverdueToTrueIfBorrowedMoreThan10DaysAgo(){
        book.setBorrowedDate(OffsetDateTime.now().minusDays(11));

        assertTrue(bookService.isOverdue(book));
    }

    @Test
    void testCalculateDebt_ShouldCalculateDebtOfBook(){
        book.setIsDebtPaid(false);
        assertEquals(5.0, bookService.calculateDebt(book));

        book.setBorrowedDate(OffsetDateTime.now());
        assertEquals(0.0, bookService.calculateDebt(book));

        book.setBorrowedDate(OffsetDateTime.now().minusDays(12));
        assertEquals(20.0, bookService.calculateDebt(book));
    }

    @Test
    void testCalculateTotalDebt(){
        Book book2 = new Book();
        book2.setPerson(person);
        book2.setBorrowedDate(OffsetDateTime.now().minusDays(14));
        book2.setIsDebtPaid(false);
        book.setIsDebtPaid(false);
        book.setPerson(person);
        person.setBookList(List.of(book, book2));

        assertEquals(35.0, bookService.calculateTotalDebt(person));
    }

}