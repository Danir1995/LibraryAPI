package com.danir.libraryAPI.util;

import com.danir.libraryAPI.models.Person;
import com.danir.libraryAPI.services.PeopleService;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
@Component
public class PeopleValidator implements Validator {

    private final PeopleService peopleService;

    public PeopleValidator(PeopleService peopleService) {
        this.peopleService = peopleService;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return Person.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Person person = (Person) target;

        if (peopleService.show(person.getFullName()).isPresent()){
            errors.rejectValue("fullName", "", "this name is already exist");
        }
    }
}
