package com.danir.libraryAPI.controllers;

import com.danir.libraryAPI.dto.PersonDTO;
import com.danir.libraryAPI.models.Person;
import com.danir.libraryAPI.models.Role;
import com.danir.libraryAPI.services.PeopleService;
import jakarta.validation.Valid;
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
public class AuthController {

    private final PeopleService peopleService;
    private final ModelMapper modelMapper;

    public AuthController(PeopleService peopleService, ModelMapper modelMapper) {
        this.peopleService = peopleService;
        this.modelMapper = modelMapper;
    }

    @GetMapping("/login")
    public String login(){
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("person", new Person());
        return "registration";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("person") @Valid PersonDTO personDTO, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "registration";
        }

        if (peopleService.emailExists(personDTO.getEmail())) {
            bindingResult.rejectValue("email", "error.person", "Email already exists!");
            return "registration";
        }

        if (personDTO.getPassword() == null || personDTO.getPassword().length() < 6){
            bindingResult.rejectValue("password", "error.person", "Password must be at least 6 characters long");
            return "registration";
        }

        peopleService.save(convertToPerson(personDTO));

        return "redirect:/login";
    }

    @PostMapping("/people/{id}/toggle-admin")
    public String toggleAdmin(@PathVariable("id") int id) {
        Optional<Person> personOptional = Optional.of((peopleService.findOne(id)));
        Person person = personOptional.get();

        if (person.getRoles().contains(Role.ROLE_ADMIN)) {
            person.getRoles().remove(Role.ROLE_ADMIN);
        } else {
            person.getRoles().add(Role.ROLE_ADMIN);
        }

        peopleService.update(id, person);

        return "redirect:/people/" + id;
    }

    private Person convertToPerson(PersonDTO personDTO){
        return modelMapper.map(personDTO, Person.class);
    }

}
