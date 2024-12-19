package com.danir.libraryAPI.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.OffsetDateTime;


@Entity
@Table(name = "book")
@Data
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_id", nullable = false)
    private int bookId;

    @Column
    @NotNull(message = "book name is required!")
    @Size(min = 2, max = 64,message = "name of book must be between 2 and 64 characters")
    private String name;

    @Column
    @NotNull(message = "author's name is required!")
    @Size(min = 2, max = 100, message = "name of author must be between 2 and 100 symbols")
    private String author;

    @Column
    @Min(value = 1300, message = "Year can not be less than 1300")
    @NotNull(message = "fill this line")
    private int year;

    @Column(name = "borrowed_date")
    private OffsetDateTime borrowedDate;

    @ManyToOne
    @JoinColumn(name = "person_id")
    private Person person;

    @ManyToOne
    @JoinColumn(name = "reserved_by_id")
    private Person reservedBy;

    @Column
    public boolean isOverdue;

    public Book() {
    }

    public Book(String name, int year, String author) {
        this.author = author;
        this.name = name;
        this.year = year;
    }
}
