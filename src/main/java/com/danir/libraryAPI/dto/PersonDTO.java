package com.danir.libraryAPI.dto;
import com.danir.libraryAPI.models.Book;
import com.danir.libraryAPI.models.Role;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
public class PersonDTO {

    @Size(min = 2, max = 32,message = "name must be between 2 and 32 characters")
    @NotNull(message = "full name can not be empty")
    private String fullName;

    @Min(value = 1920, message = "Year can not be less than 1920")
    @Max(value = 2025, message = "Year of birth cannot be in the future")
    @NotNull(message = "please provide year")
    private Integer yearOfBirth;

    @Email(message = "Invalid email address")
    @NotBlank(message = "Email is required")
    private String email;

    private List<Book> bookList;

    private Set<Role> roles;

    private String password;


}
