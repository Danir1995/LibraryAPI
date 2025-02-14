package com.danir.libraryAPI.controllers;

import com.danir.libraryAPI.dto.BookDTO;
import com.danir.libraryAPI.models.Book;
import com.danir.libraryAPI.models.Person;
import com.danir.libraryAPI.services.BookService;
import com.danir.libraryAPI.services.PeopleService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/books")
@Slf4j
public class BooksController {

    private final BookService bookService;
    private final PeopleService peopleService;

    public BooksController(BookService bookService, PeopleService peopleService) {
        this.bookService = bookService;
        this.peopleService = peopleService;
    }

    @GetMapping
    public String index(@RequestParam(required = false, defaultValue = "0") int page,
                        @RequestParam(required = false, defaultValue = "20") int size,
                        @RequestParam(required = false, defaultValue = "false") boolean onlyAvailable,
                        Model model, Principal principal) {
        log.info("Accessing book index page. Page: {}, Size: {}, Only Available: {}", page, size, onlyAvailable);

        String username = principal.getName();
        Person person = peopleService.findByUsername(username);
        boolean isAdmin = hasRole();

        Page<Book> bookPage;
        if (onlyAvailable) {
            bookPage = bookService.getAvailableBooks(PageRequest.of(page, size, Sort.by("year")));
        } else {
            bookPage = bookService.findAll(PageRequest.of(page, size, Sort.by("year")));
        }

        List<Book> bookList = bookPage.getContent();

        List<BookDTO> bookDTOList = bookList.stream()
                .map(book -> {
                    BookDTO bookDTO = bookService.convertToBookDTO(book);
                    bookDTO.setOverdue(bookService.isOverdue(book));
                    return bookDTO;
                })
                .toList();

        model.addAttribute("currentUser", person.getPersonId());
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("books", bookDTOList);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", bookPage.getTotalPages());
        model.addAttribute("onlyAvailable", onlyAvailable);

        log.info("Book index page loaded successfully.");
        return "book/bookIndex";
    }

    @GetMapping("/{id}")
    public String show(@PathVariable("id") int id, Model model, Principal principal) {
        log.info("Accessing book details page for book ID: {}", id);

        BookDTO bookDTO = bookService.getBookDetails(id);

        String username = principal.getName();
        Person person = peopleService.findByUsername(username);
        boolean isAdmin = hasRole();

        model.addAttribute("currentUser", person);
        model.addAttribute("book", bookDTO);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("people", isAdmin ? peopleService.findAll() : null);
        model.addAttribute("isOverdue", bookDTO.isOverdue());
        model.addAttribute("isOccupied", bookDTO.getPerson_name() != null);
        model.addAttribute("isReserved", bookDTO.getReserved_by_name() != null);

        log.info("Book details page loaded successfully for book ID: {}", id);
        return "book/bookShow";
    }

    @GetMapping("/newBook")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String newBook(@ModelAttribute("book") BookDTO bookDTO) {
        log.info("Accessing new book creation page.");
        return "book/newBook";
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String createBook(@ModelAttribute("book") @Valid BookDTO bookDTO,
                             BindingResult bindingResult) {
        log.info("Attempting to create a new book with title: {}", bookDTO.getName());

        if (bindingResult.hasErrors()) {
            log.warn("Validation errors occurred while creating a new book: {}", bindingResult.getAllErrors());
            return "book/newBook";
        }

        bookService.save(bookService.convertToBook(bookDTO));
        log.info("Book created successfully with title: {}", bookDTO.getName());
        return "redirect:/books";
    }

    @GetMapping("/{id}/editBook")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String edit(@PathVariable("id") int id, Model model) {
        log.info("Accessing edit page for book ID: {}", id);

        Book book = bookService.findOne(id);
        BookDTO bookDTO = bookService.convertToBookDTO(book);
        model.addAttribute("book", bookDTO);

        log.info("Edit page loaded successfully for book ID: {}", id);
        return "book/editBook";
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String updateBook(@ModelAttribute("book") @Valid BookDTO bookDTO, BindingResult bindingResult,
                             @PathVariable("id") int id) {
        log.info("Attempting to update book with ID: {}", id);

        if (bindingResult.hasErrors()) {
            log.warn("Validation errors occurred while updating book with ID: {}", id);
            return "book/editBook";
        }

        bookService.update(id, bookService.convertToBook(bookDTO));
        log.info("Book updated successfully with ID: {}", id);
        return "redirect:/books";
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String deleteBook(@PathVariable("id") int id) {
        log.info("Attempting to delete book with ID: {}", id);
        bookService.delete(id);
        log.info("Book deleted successfully with ID: {}", id);
        return "redirect:/books";
    }

    @PostMapping("/assign-book")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String assignBook(@RequestParam(value = "personId", required = false) Integer personId,
                             @RequestParam(value = "bookId", required = false) Integer bookId) {
        log.info("Attempting to assign book with ID: {} to person with ID: {}", bookId, personId);

        if (personId == null || bookId == null) {
            log.warn("Missing required parameters: personId or bookId");
            return "redirect:/books/" + bookId;
        }

        Person person = peopleService.findOne(personId);
        Book book = bookService.findOne(bookId);
        bookService.savePersonWithBook(person, book);

        log.info("Book assigned successfully. Book ID: {}, Person ID: {}", bookId, personId);
        return "redirect:/books/" + bookId;
    }

    @PostMapping("/release")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String release(@RequestParam("bookId") int bookId) {
        log.info("Attempting to release book with ID: {}", bookId);
        Book book = bookService.findOne(bookId);
        bookService.release(book, bookId);
        log.info("Book released successfully with ID: {}", bookId);
        return "redirect:/books/" + bookId;
    }

    @GetMapping("/search")
    public String search() {
        log.info("Accessing book search page.");
        return "book/searching";
    }

    @GetMapping("/result")
    public String searchResult(@RequestParam("name") String name, Model model) {
        log.info("Searching for books with title containing: {}", name);
        List<Book> books = bookService.searchBooksByTitle(name);
        model.addAttribute("books", books);

        log.info("Found {} books matching the search criteria.", books.size());
        return "book/resultsOfSearching";
    }

    @PostMapping("/{bookId}/reserve")
    public String reserveBook(@PathVariable int bookId, @RequestParam int personId,
                              RedirectAttributes redirectAttributes) {
        log.info("Attempting to reserve book with ID: {} for person with ID: {}", bookId, personId);
        String errorMessage = bookService.reserveBook(bookId, personId);
        if (errorMessage != null) {
            log.warn("Failed to reserve book: {}", errorMessage);
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
            return "redirect:/books/" + bookId;
        }
        log.info("Book reserved successfully. Book ID: {}, Person ID: {}", bookId, personId);
        redirectAttributes.addFlashAttribute("message", "Book reserved successfully");
        return "redirect:/books/" + bookId;
    }

    @DeleteMapping("/{bookId}/cancel-reservation")
    public String cancelReservation(@PathVariable int bookId, Model model) {
        log.info("Attempting to cancel reservation for book with ID: {}", bookId);
        bookService.cancelReservation(bookId);
        log.info("Reservation cancelled successfully for book with ID: {}", bookId);
        model.addAttribute("message", "Reservation cancelled successfully");
        return "redirect:/books/" + bookId;
    }

    protected static boolean hasRole() {
        return SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}