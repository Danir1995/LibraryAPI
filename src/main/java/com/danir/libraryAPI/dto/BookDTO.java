package com.danir.libraryAPI.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BookDTO {

    private int bookId;

    @NotNull(message = "book must has name!")
    @Size(min = 2, max = 64,message = "name of book must be between 2 and 64 characters")
    private String name;

    @NotNull(message = "fill the author")
    @Size(min = 2, max = 100, message = "name of author must be between 2 and 100 symbols")
    private String author;

    @Min(value = 1300, message = "Year can not be less than 1300")
    @NotNull(message = "fill this line")
    private int year;


    private String person_name;

    private String reserved_by_name;

    private boolean isOverdue;

}
