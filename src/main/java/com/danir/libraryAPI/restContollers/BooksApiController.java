package com.danir.libraryAPI.restContollers;

import com.danir.libraryAPI.dto.BookDTO;
import com.danir.libraryAPI.models.Book;
import com.danir.libraryAPI.models.Person;
import com.danir.libraryAPI.services.BookService;
import com.danir.libraryAPI.services.PeopleService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@Slf4j
public class BooksApiController {

    private final BookService bookService;
    private final PeopleService peopleService;

    @Autowired
    public BooksApiController(BookService bookService, PeopleService peopleService) {
        this.bookService = bookService;
        this.peopleService = peopleService;
    }

    @GetMapping
    public ResponseEntity<List<BookDTO>> getAllBooks() {
        List<BookDTO> bookDTOList = bookService.findAll()
                .stream()
                .map(bookService::convertToBookDTO)
                .toList();

        return ResponseEntity.ok(bookDTOList);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookDTO> getBookById(@PathVariable int id) {
        BookDTO bookDTO = bookService.convertToBookDTO(bookService.findOne(id));
        return new ResponseEntity<>(bookDTO, HttpStatus.OK);
    }

    @PostMapping("/assignBook")
    public ResponseEntity<BookDTO> assignBook(@RequestParam("personId") Integer personId,
                                              @RequestParam("bookId") Integer bookId) {
        Person person = peopleService.findOne(personId);
        Book book = bookService.findOne(bookId);

        bookService.savePersonWithBook(person, book);

        BookDTO bookDTO = bookService.convertToBookDTO(book);

        return ResponseEntity.ok(bookDTO);
    }

    @PostMapping("/{bookId}/reserve")
    public ResponseEntity<?> reserveBook(@PathVariable int bookId, @RequestParam int personId) {
        log.info("Attempting to reserve book with ID: {} for person with ID: {}", bookId, personId);

        // Try to reserve book
        String errorMessage = bookService.reserveBook(bookId, personId);
        if (errorMessage != null) {
            log.warn("Failed to reserve book: {}", errorMessage);
            // http stat 400 (Bad Request)
            return ResponseEntity.badRequest().body(errorMessage);
        }

        BookDTO bookDTO = bookService.convertToBookDTO(bookService.findOne(bookId));

        log.info("Book reserved successfully. Book ID: {}, Person ID: {}", bookId, personId);
        // response with http status 200 ok and bookdto's body
        return ResponseEntity.ok(bookDTO);
    }

    @DeleteMapping("/{bookId}/cancel-reservation")
    public ResponseEntity<?> cancelReservation(@PathVariable int bookId) {
        log.info("Attempting to cancel reservation for book with ID: {}", bookId);

        try {
            bookService.cancelReservation(bookId);

            // Receive new info about the book
            Book updatedBook = bookService.findOne(bookId);
            BookDTO bookDTO = bookService.convertToBookDTO(updatedBook);

            log.info("Reservation cancelled successfully for book with ID: {}. Updated data: {}", bookId, bookDTO);
            return ResponseEntity.ok(bookDTO); // 200 OK
        } catch (EntityNotFoundException e) {
            log.warn("Book not found with ID: {}", bookId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Book not found"); // 404 Not Found
        } catch (IllegalStateException e) {
            log.warn("Cannot cancel reservation for book with ID: {}. Reason: {}", bookId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage()); // 400 Bad Request
        } catch (Exception e) {
            log.error("Error cancelling reservation for book with ID: {}", bookId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error"); // 500 Internal Server Error
        }
    }

    @PostMapping("/release")
    public ResponseEntity<?> release(@RequestParam("bookId") int bookId) {
        log.info("Attempting to release book with ID: {}", bookId);
        Book book = bookService.findOne(bookId);
        bookService.release(book, bookId);
        BookDTO bookDTO = bookService.convertToBookDTO(book);
        log.info("Book released successfully with ID: {}", bookId);
        return ResponseEntity.ok(bookDTO);
    }

    @PostMapping("/add")
    public ResponseEntity<BookDTO> addBook(@RequestBody @Valid BookDTO book) {

        log.info("Attempting to create a new book with title: {}", book.getName());

        Book savedBook = bookService.save(bookService.convertToBook(book));
        BookDTO bookDTO = bookService.convertToBookDTO(savedBook);

        // Response with Http status 201 and bookDto's body
        return ResponseEntity.status(HttpStatus.CREATED).body(bookDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateBook(@PathVariable int id, @RequestBody @Valid BookDTO book) {

        bookService.update(id, bookService.convertToBook(book));
        log.info("Book updated successfully with ID: {}", id);

        Book updatedBook = bookService.findOne(id);
        BookDTO bookDTO = bookService.convertToBookDTO(updatedBook);

        return ResponseEntity.status(HttpStatus.OK).body(bookDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        log.info("Attempting to delete book with ID: {}", id);

        try {
            bookService.delete(id);
            log.info("Book deleted successfully with ID: {}", id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT); // 204 No Content
        } catch (EntityNotFoundException e) {
            log.warn("Book not found with ID: {}", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND); // 404 Not Found
        } catch (Exception e) {
            log.error("Error deleting book with ID: {}", id, e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR); // 500 Internal Server Error
        }
    }
}
