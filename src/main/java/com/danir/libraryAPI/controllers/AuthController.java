package com.danir.libraryAPI.controllers;

import com.danir.libraryAPI.dto.PersonDTO;
import com.danir.libraryAPI.models.Person;
import com.danir.libraryAPI.models.Role;
import com.danir.libraryAPI.services.PeopleService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Optional;

@Controller
@Slf4j
public class AuthController {

    private final PeopleService peopleService;
    private final ModelMapper modelMapper;

    public AuthController(PeopleService peopleService, ModelMapper modelMapper) {
        this.peopleService = peopleService;
        this.modelMapper = modelMapper;
    }

    @GetMapping("/login")
    public String login(){
        log.info("Login page accessed");
        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        log.info("Registration form accessed");
        model.addAttribute("person", new Person());
        return "auth/registration";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("person") @Valid PersonDTO personDTO, BindingResult bindingResult) {
        log.info("Attempting to register user");

        if (bindingResult.hasErrors()) {
            log.warn("Registration form has errors: {}", bindingResult.getAllErrors());
            return "auth/registration";
        }

        if (peopleService.emailExists(personDTO.getEmail())) {
            log.warn("Email already exists");
            bindingResult.rejectValue("email", "error.person", "Email already exists!");
            return "auth/registration";
        }

        if (personDTO.getPassword() == null || personDTO.getPassword().length() < 6){
            log.warn("Password is too short for email");
            bindingResult.rejectValue("password", "error.person", "Password must be at least 6 characters long");
            return "auth/registration";
        }

        peopleService.save(convertToPerson(personDTO));
        log.info("User registered successfully with email");

        return "redirect:/login";
    }

    @PostMapping("/people/{id}/toggle-admin")
    public String toggleAdmin(@PathVariable("id") int id) {
        log.info("Attempting to toggle admin role for user with id: {}", id);

        Optional<Person> personOptional = Optional.of((peopleService.findOne(id)));
        Person person = personOptional.get();

        if (person.getRoles().contains(Role.ROLE_ADMIN)) {
            person.getRoles().remove(Role.ROLE_ADMIN);
            log.info("Admin role removed for user with id: {}", id);
        } else {
            person.getRoles().add(Role.ROLE_ADMIN);
            log.info("Admin role added for user with id: {}", id);
        }

        peopleService.update(id, person);

        return "redirect:/people/" + id;
    }

    private Person convertToPerson(PersonDTO personDTO){
        return modelMapper.map(personDTO, Person.class);
    }
}