package com.danir.libraryAPI.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class PersonDTO {

    @Size(min = 2, max = 32,message = "name must be between 2 and 32 characters")
    @NotNull(message = "full name can not be empty")
    private String fullName;

    @Min(value = 1920, message = "Year can not be less than 1920")
    @NotNull(message = "fill this line")
    private int yearOfBirth;
}
