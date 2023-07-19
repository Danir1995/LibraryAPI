package com.danir.libraryAPI.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;


@Entity
@Table(name = "book")
@Getter
@Setter
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
    private int year;

    @Column(name = "borrowed_date")
    private OffsetDateTime borrowedDate;

    @ManyToOne
    @JoinColumn(name = "person_id")
    private Person person;

    public Book() {
    }

    public Book(String name, int year, String author) {
        this.author = author;
        this.name = name;
        this.year = year;
    }

    public boolean isOverdue() {
        return borrowedDate != null && borrowedDate.isBefore(OffsetDateTime.now().minusDays(10));
    }

}
