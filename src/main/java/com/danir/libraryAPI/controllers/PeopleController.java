package com.danir.libraryAPI.controllers;

import com.danir.libraryAPI.dto.PersonDTO;
import com.danir.libraryAPI.models.BorrowedBook;
import com.danir.libraryAPI.models.Person;
import com.danir.libraryAPI.models.Role;
import com.danir.libraryAPI.services.BookService;
import com.danir.libraryAPI.services.BorrowedBookService;
import com.danir.libraryAPI.services.PeopleService;
import com.danir.libraryAPI.util.PeopleValidator;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Set;

import static com.danir.libraryAPI.controllers.BooksController.hasRole;


@Slf4j
@Controller
@RequestMapping("/people")
public class PeopleController {

    private final PeopleService peopleService;
    private final PeopleValidator validator;
    private final BorrowedBookService borrowedBookService;
    private final ModelMapper modelMapper;
    private final BookService bookService;

    public PeopleController(PeopleService peopleService, PeopleValidator validator, BorrowedBookService borrowedBookService, ModelMapper modelMapper, BookService bookService) {
        this.peopleService = peopleService;
        this.validator = validator;
        this.borrowedBookService = borrowedBookService;
        this.modelMapper = modelMapper;
        this.bookService = bookService;
    }

    @GetMapping("")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String index(Model model, Principal principal){
        String userName = principal.getName();
        Person person = peopleService.findByUsername(userName);

        model.addAttribute("people", peopleService.findAll());
        model.addAttribute("currentUser", person.getPersonId());

        log.info("Admin {} accessed the people index page.", userName);
        return "people/peopleIndex";
    }

    @GetMapping("/{id}")
    public String show(@PathVariable("id") int id, Model model) {
        boolean isAdmin = hasRole();
        Person person = peopleService.findOne(id);
        person.getBookList().forEach(book -> book.setDebt(bookService.calculateDebt(book)));
        double totalDebt = bookService.calculateTotalDebt(person);
        Set<BorrowedBook> borrowedBooks = borrowedBookService.findByPerson(person);

        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("thisUserIsAdmin", person.getRoles().contains(Role.ROLE_ADMIN));
        model.addAttribute("person", person);
        model.addAttribute("bookList", person.getBookList());
        model.addAttribute("reservedBookList", person.getReservedBooks());
        model.addAttribute("totalDebt", totalDebt);
        model.addAttribute("borrowedBeforeBooks", borrowedBooks);

        log.info("Displaying details for person with ID: {}", id);
        return "people/show";
    }

    @GetMapping("/new")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String newPerson(@ModelAttribute("person") PersonDTO personDTO){
        log.info("Admin is creating a new person.");
        return "people/new";
    }

    @PostMapping()
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String create(@ModelAttribute("person") @Valid PersonDTO personDTO,
                         BindingResult result) {
        if (result.hasErrors()){
            log.warn("Validation errors occurred while creating a new person.");
            return "people/new";
        }

        if (peopleService.emailExists(personDTO.getEmail())) {
            result.rejectValue("email", "error.person", "Email already exists");
            log.warn("Email already exists.");
        }

        validator.validate(personDTO, result);

        if (personDTO.getPassword() == null || personDTO.getPassword().length() < 6){
            result.rejectValue("password", "error.person", "Password must be at least 6 characters long");
            log.warn("Password for email is too short.");
            return "people/new";
        }

        peopleService.save(convertToPerson(personDTO));

        log.info("New person created successfully.");
        return "redirect:/people";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable("id") int id, Model model, Principal principal){
        // Check if user is editing his profile
        Person person = peopleService.findOne(id);
        boolean isSelf = person.getFullName().equals(principal.getName());

        model.addAttribute("person", person);
        model.addAttribute("isSelf", isSelf);

        log.info("Editing person details for ID: {}", id);
        return "people/personEdit";
    }

    @PatchMapping("/{id}")
    public String update(@ModelAttribute("person") @Valid PersonDTO personDTO, BindingResult result,
                         @PathVariable("id") int id){
        if (result.hasErrors()){
            log.warn("Validation errors occurred while updating person with ID: {}", id);
            return "people/personEdit";
        }

        Person person = peopleService.findOne(id);

        if (peopleService.emailExists(personDTO.getEmail()) && !person.getEmail().equals(personDTO.getEmail())) {
            result.rejectValue("email", "error.person", "Email already exists");
            log.warn("Email already exists!");
        }

        if (!personDTO.getFullName().equals(person.getFullName())){
            validator.validate(personDTO, result);
        }

        if (
                personDTO.getPassword() != null &&
                        personDTO.getPassword().length() < 6 &&
                        !personDTO.getPassword().isEmpty()
        ) {

            result.rejectValue("password", "error.person", "Password must be at least 6 characters long");
            log.warn("Password is too short.");
            return "people/personEdit";
        }

        peopleService.update(id, convertToPerson(personDTO));

        log.info("Updated person details for ID: {}", id);
        return "redirect:/people/" + id;
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable("id") int id, RedirectAttributes redirectAttributes) {
        Person person = peopleService.findOne(id);

        // Check if person has assigned books
        if (!person.getBookList().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot delete user before receiving books");
            log.warn("Cannot delete person with ID {} because they have assigned books.", id);
            return "redirect:/people/" + id;
        }
        peopleService.delete(id);

        log.info("Deleted person with ID: {}", id);
        return "redirect:/people";
    }

    private Person convertToPerson(PersonDTO personDTO){
        return modelMapper.map(personDTO, Person.class);
    }

    private PersonDTO convertToPersonDTO(Person person){
        return modelMapper.map(person, PersonDTO.class);
    }
}
