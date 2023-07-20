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

import java.util.List;


@Controller
@RequestMapping("/books")
public class BooksController {
    private static final Logger LOG = LogManager.getLogger(BooksController.class);
    private final BookService bookService;
    private final PeopleService peopleService;

    private final ModelMapper modelMapper;

    @Autowired
    public BooksController(BookService bookService, PeopleService peopleService, ModelMapper modelMapper) {
        this.bookService = bookService;
        this.peopleService = peopleService;
        this.modelMapper = modelMapper;
    }

    @GetMapping
    public String index(@RequestParam(required = false, defaultValue = "0") int page,
                        @RequestParam(required = false, defaultValue = "5") int size,
            Model model){

        Page<Book> bookPage = bookService.findAll(PageRequest.of(page, size, Sort.by("year")));
        List<Book> bookList = bookPage.getContent();

        model.addAttribute("books", bookList);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", bookPage.getTotalPages());

        return "book/bookIndex";
    }

    @GetMapping("/{id}")
    public String show(@PathVariable("id")int id, Model model){
        Book book = bookService.findOne(id);
        model.addAttribute("book", book);
        model.addAttribute("people", peopleService.findAll());
        model.addAttribute("isOverdue", book.isOverdue());
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

        bookService.save(convertToBook(bookDTO));
        return "redirect:/books";
    }

    @GetMapping("/{id}/editBook")
    public String edit(@PathVariable("id") int id, Model model){
        model.addAttribute("book", bookService.findOne(id));
        return "book/editBook";
    }

    @PatchMapping("/{id}")
    public String updateBook(@ModelAttribute("book") @Valid BookDTO bookDTO, BindingResult bindingResult,
                             @PathVariable("id") int id){
        if (bindingResult.hasErrors()){
            return "book/editBook";
        }

        bookService.update(id, convertToBook(bookDTO));
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

    private Book convertToBook(BookDTO bookDTO) {
        return modelMapper.map(bookDTO, Book.class);
    }

    private BookDTO convertToBookDTO(Book book) {
        return modelMapper.map(book, BookDTO.class);
    }


}
