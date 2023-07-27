package com.danir.libraryAPI.controllers;

import com.danir.libraryAPI.dto.PersonDTO;
import com.danir.libraryAPI.models.Person;
import com.danir.libraryAPI.services.PeopleService;
import com.danir.libraryAPI.util.PeopleValidator;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;



@Controller
@RequestMapping("/people")
public class PeopleController {
    private final PeopleService peopleService;
    private final PeopleValidator validator;

    private final ModelMapper modelMapper;

    public PeopleController(PeopleService peopleService, PeopleValidator validator, ModelMapper modelMapper) {
        this.peopleService = peopleService;
        this.validator = validator;
        this.modelMapper = modelMapper;
    }

    @GetMapping("")
    public String index(Model model){
        model.addAttribute("people", peopleService.findAll());
        return "people/index";
    }

    @GetMapping("/{id}")
    public String show(@PathVariable("id") int id, Model model) {
        Person person = peopleService.findOne(id);
        model.addAttribute("person", person);
        model.addAttribute("bookList", person.getBookList());
        return "people/show";
    }

    @GetMapping("/new")
    public String newPerson(@ModelAttribute("person") PersonDTO personDTO){
        return "people/new";
    }

    @PostMapping()
    public String create(@ModelAttribute("person") @Valid PersonDTO personDTO,
                         BindingResult result) {
        validator.validate(convertToPerson(personDTO), result);
        if (result.hasErrors()){
            return "people/new";
        }
        peopleService.save(convertToPerson(personDTO));
        return "redirect:/people";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable("id") int id, Model model){
       model.addAttribute("person", peopleService.findOne(id));
        return "people/edit";
    }

    @PatchMapping("/{id}")
    public String update(@ModelAttribute("person") @Valid PersonDTO personDTO, BindingResult result,
                         @PathVariable("id") int id){
        validator.validate(convertToPerson(personDTO), result);
        if (result.hasErrors()){
            return "people/edit";
        }
        peopleService.update(id, convertToPerson(personDTO));
        return "redirect:/people";
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable("id") int id) {
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
