package com.danir.libraryAPI.controllers;

import com.danir.libraryAPI.dto.PersonDTO;
import com.danir.libraryAPI.models.BorrowedBook;
import com.danir.libraryAPI.models.Person;
import com.danir.libraryAPI.models.Role;
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
import java.util.List;

import static com.danir.libraryAPI.controllers.BooksController.hasRole;


@Slf4j
@Controller
@RequestMapping("/people")
public class PeopleController {

    private final PeopleService peopleService;
    private final PeopleValidator validator;
    private final BorrowedBookService borrowedBookService;
    private final ModelMapper modelMapper;

    public PeopleController(PeopleService peopleService, PeopleValidator validator, BorrowedBookService borrowedBookService, ModelMapper modelMapper) {
        this.peopleService = peopleService;
        this.validator = validator;
        this.borrowedBookService = borrowedBookService;
        this.modelMapper = modelMapper;
    }

    @GetMapping("")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String index(Model model){
        model.addAttribute("people", peopleService.findAll());
        return "people/index";
    }

    @GetMapping("/{id}")
    public String show(@PathVariable("id") int id, Model model) {
        boolean isAdmin = hasRole();
        Person person = peopleService.findOne(id);
        List<BorrowedBook> borrowedBooks = borrowedBookService.findByPerson(person);

        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("thisUserIsAdmin", person.getRoles().contains(Role.ROLE_ADMIN));
        model.addAttribute("person", person);
        model.addAttribute("bookList", person.getBookList());
        model.addAttribute("borrowedBeforeBooks", borrowedBooks);
        return "people/show";
    }

    @GetMapping("/new")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String newPerson(@ModelAttribute("person") PersonDTO personDTO){
        return "people/new";
    }

    @PostMapping()
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String create(@ModelAttribute("person") @Valid PersonDTO personDTO,
                         BindingResult result) {
        if (result.hasErrors()){
            return "people/new";
        }

        if (peopleService.emailExists(personDTO.getEmail())) {
            result.rejectValue("email", "error.person", "Email already exists");
        }

        validator.validate(personDTO, result);

        if (personDTO.getPassword() == null || personDTO.getPassword().length() < 6){
            result.rejectValue("password", "error.person", "Password must be at least 6 characters long");
            return "people/new";
        }

        peopleService.save(convertToPerson(personDTO));
        return "redirect:/people";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable("id") int id, Model model, Principal principal){
        // Check if user is editing his profile
        Person person = peopleService.findOne(id);
        boolean isSelf = person.getFullName().equals(principal.getName());

        model.addAttribute("person", peopleService.findOne(id));
        model.addAttribute("isSelf", isSelf);
        return "people/edit";
    }

    @PatchMapping("/{id}")
    public String update(@ModelAttribute("person") @Valid PersonDTO personDTO, BindingResult result,
                         @PathVariable("id") int id){
        if (result.hasErrors()){
            return "people/edit";
        }

        Person person = peopleService.findOne(id);

        if (peopleService.emailExists(personDTO.getEmail()) && !person.getEmail().equals(personDTO.getEmail())) {
            result.rejectValue("email", "error.person", "Email already exists");
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
            return "people/edit";
        }

        peopleService.update(id, convertToPerson(personDTO));
        return "redirect:/people/" + id;
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable("id") int id, RedirectAttributes redirectAttributes) {
        Person person = peopleService.findOne(id);

        // Check if person has assigned books
        if (!person.getBookList().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot delete user before receiving books");
            return "redirect:/people/" + id;
        }
        peopleService.delete(id);
        return "redirect:/people";
    }

    private Person convertToPerson(PersonDTO personDTO){
        return modelMapper.map(personDTO, Person.class);
    }

    private PersonDTO convertToPersonDTO(Person person){
        return modelMapper.map(person, PersonDTO.class);
    }
}
