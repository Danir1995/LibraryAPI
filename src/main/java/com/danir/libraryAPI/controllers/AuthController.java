package com.danir.libraryAPI.controllers;

import com.danir.libraryAPI.models.Person;
import com.danir.libraryAPI.models.Role;
import com.danir.libraryAPI.repositories.PeopleRepository;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final PeopleRepository peopleRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(PeopleRepository peopleRepository, PasswordEncoder passwordEncoder) {
        this.peopleRepository = peopleRepository;
        this.passwordEncoder = passwordEncoder;
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
    public String registerUser(@ModelAttribute("person") @Valid Person person, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "registration";
        }

        if (peopleRepository.findByEmail(person.getEmail()).isPresent()) {
            bindingResult.rejectValue("email", "error.person", "Email already exists!");
            return "registration";
        }

        person.setPassword(passwordEncoder.encode(person.getPassword()));
        person.getRoles().add(Role.ROLE_USER);
        peopleRepository.save(person);

        return "redirect:/login";
    }
}
