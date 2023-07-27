package com.danir.libraryAPI.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class BookDTO {

    @NotNull(message = "book must has name!")
    @Size(min = 2, max = 64,message = "name of book must be between 2 and 64 characters")
    private String name;

    @NotNull(message = "fill the author")
    @Size(min = 2, max = 100, message = "name of author must be between 2 and 100 symbols")
    private String author;

    private int year;
}
