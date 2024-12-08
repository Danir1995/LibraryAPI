package com.danir.libraryAPI.util;

import com.danir.libraryAPI.dto.BookDTO;
import com.danir.libraryAPI.dto.PersonDTO;
import com.danir.libraryAPI.models.Book;
import com.danir.libraryAPI.services.BookService;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class BookValidator implements Validator {

    private final BookService bookService;

    public BookValidator(BookService bookService) {
        this.bookService = bookService;
    }


    @Override
    public boolean supports(Class<?> clazz) {
        return BookDTO.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        BookDTO bookDTO = (BookDTO) target;
        PersonDTO personDTO = (PersonDTO) target;

//        if (bookService.show(personDTO.getFullName()).isPresent()) {
//            errors.rejectValue("fullName", "", "This name already exists");
//        }

    }
}
