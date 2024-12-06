package com.danir.libraryAPI.controllers;

import com.danir.libraryAPI.dto.BookDTO;
import com.danir.libraryAPI.models.Book;
import com.danir.libraryAPI.models.Person;
import com.danir.libraryAPI.services.BookService;
import com.danir.libraryAPI.services.PeopleService;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;


@Controller
@RequestMapping("/books")
public class BooksController {
    private static final Logger LOG = LogManager.getLogger(BooksController.class);
    private final BookService bookService;
    private final PeopleService peopleService;


    public BooksController(BookService bookService, PeopleService peopleService) {
        this.bookService = bookService;
        this.peopleService = peopleService;
        // this.modelMapper = modelMapper;
    }

    @GetMapping
    public String index(@RequestParam(required = false, defaultValue = "0") int page,
                        @RequestParam(required = false, defaultValue = "5") int size,
                        @RequestParam(required = false, defaultValue = "false") boolean onlyAvailable,
                        Model model){

        Page<Book> bookPage;
        if (onlyAvailable) {
            bookPage = bookService.getAvailableBooks(PageRequest.of(page, size, Sort.by("year")));
        } else {
            bookPage = bookService.findAll(PageRequest.of(page, size, Sort.by("year")));
        }

        List<Book> bookList = bookPage.getContent();

        List<BookDTO> bookDTOList = bookList.stream()
                .map(book -> {
                    BookDTO bookDTO = bookService.convertToBookDTO(book); // Используем метод конвертации
                    bookDTO.setOverdue(bookService.isOverdue(book)); // Устанавливаем просроченность
                    return bookDTO;
                })
                .toList();

        model.addAttribute("books", bookDTOList);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", bookPage.getTotalPages());
        model.addAttribute("onlyAvailable", onlyAvailable);
        return "book/bookIndex";
    }

    @GetMapping("/{id}")
    public String show(@PathVariable("id")int id, Model model){

        BookDTO bookDTO = bookService.getBookDetails(id);

        model.addAttribute("book", bookDTO);
        model.addAttribute("people", peopleService.findAll());
         model.addAttribute("isOverdue", bookDTO.isOverdue());
        model.addAttribute("isOccupied", bookDTO.getPerson_name() != null);
        model.addAttribute("isReserved", bookDTO.getReserved_by_name() != null);

        return "book/bookShow";
    }

    @GetMapping("/newBook")
    public String newPerson(@ModelAttribute("book") BookDTO bookDTO){
        return "book/newBook";
    }

    @PostMapping
    public String createBook(@ModelAttribute("book") @Valid BookDTO bookDTO,
                             BindingResult bindingResult){

        if (bindingResult.hasErrors()) {
            return "book/newBook";
        }

        bookService.save(bookService.convertToBook(bookDTO));
        return "redirect:/books";
    }

    @GetMapping("/{id}/editBook")
    public String edit(@PathVariable("id") int id, Model model){
        Book book = bookService.findOne(id);
        BookDTO bookDTO = bookService.convertToBookDTO(book);
        model.addAttribute("book", bookDTO);
        return "book/editBook";
    }

    @PatchMapping("/{id}")
    public String updateBook(@ModelAttribute("book") @Valid BookDTO bookDTO, BindingResult bindingResult,
                             @PathVariable("id") int id){
        if (bindingResult.hasErrors()){
            return "book/editBook";
        }

        bookService.update(id, bookService.convertToBook(bookDTO));
        return "redirect:/books";
    }

    @DeleteMapping("/{id}")
    public String deleteBook(@PathVariable("id")int id){
        LOG.info("deleting book");
        bookService.delete(id);
        return "redirect:/books";
    }

    @PostMapping("/assign-book")
    public String assignBook(@RequestParam("personId") int personId, @RequestParam("bookId") int bookId) {
        LOG.info("enter in assign book");
        Person person = peopleService.findOne(personId);
        Book book = bookService.findOne(bookId);
        System.out.println(book.getBookId());

            bookService.savePersonWithBook(person, book);
            LOG.debug("finished assign book");

        return "redirect:/books/"+bookId;
    }

    @PostMapping("/release")
    public String release(@RequestParam("bookId") int bookId){
        Book book = bookService.findOne(bookId);
        bookService.release(book, bookId);
        return "redirect:/books/" + bookId;
    }

    @GetMapping("/search")
    public String search()
    {
        return "book/searching";
    }

    @GetMapping("/result")
    public String searchResult(@RequestParam("name") String name, Model model) {
        List<Book> books = bookService.searchBooksByTitle(name);
        model.addAttribute("books", books);

        return "book/resultsOfSearching";
    }

    @PostMapping("/{bookId}/reserve")
    public String reserveBook(@PathVariable int bookId, @RequestParam int personId,
                              RedirectAttributes redirectAttributes, Model model) {
        String errorMessage = bookService.reserveBook(bookId, personId);
        if (errorMessage != null) {
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
            return "redirect:/books/" + bookId; // Редирект на GET метод
        }
        redirectAttributes.addFlashAttribute("message", "Book reserved successfully");
        return "redirect:/books/" + bookId;  // Название шаблона для отображения
    }

    // Отмена бронирования
    @DeleteMapping("/{bookId}/cancel-reservation")
    public String cancelReservation(@PathVariable int bookId, Model model) {
        bookService.cancelReservation(bookId);
        model.addAttribute("message", "Reservation cancelled successfully");
        return "redirect:/books/" + bookId;  // Название шаблона для отображения
    }




}
