package com.danir.libraryAPI.dto;
import jakarta.persistence.Column;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class PersonDTO {

    @Size(min = 2, max = 32,message = "name must be between 2 and 32 characters")
    @NotNull(message = "full name can not be empty")
    private String fullName;

    @Column(name = "year_of_birth")
    @Min(value = 1920, message = "Year can not be less than 1920")
    @Max(value = 2025, message = "Year of birth cannot be in the future")
    @NotNull(message = "please provide year")
    private Integer yearOfBirth;

    @Email(message = "Invalid email address")
    @NotBlank(message = "Email is required")
    private String email;
}
