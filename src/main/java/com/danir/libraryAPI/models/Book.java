package com.danir.libraryAPI.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.List;


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
    @Max(value = 2025, message = "Year can not be bigger than this year")
    private int year;

    @Column(name = "borrowed_date")
    private OffsetDateTime borrowedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    private Person person;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserved_by_id")
    private Person reservedBy;

    @Column
    private boolean isOverdue;

    @Column
    private OffsetDateTime paymentDate;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<BorrowedBook> borrowHistory;

    @Column(nullable = false, columnDefinition = "double default 0.0")
    private Double debt = 0.0;

    @Column
    private Boolean isDebtPaid;

    public Book() {
    }

    public Book(String name, int year, String author) {
        this.author = author;
        this.name = name;
        this.year = year;
    }
}
