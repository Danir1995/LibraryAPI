package com.danir.libraryAPI.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "borrowed_book")
@Data
public class BorrowedBook {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "person_id")
    private Person person;

    @ManyToOne
    @JoinColumn(name = "book_id")
    private Book book;
}
