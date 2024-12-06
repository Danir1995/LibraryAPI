package com.danir.libraryAPI.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "person")
@Data
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "person_id", nullable = false)
    private int personId;

    @Column(name = "full_name")
    @Size(min = 2, max = 32,message = "name must be between 2 and 32 characters")
    @NotNull(message = "full name can not be empty")
    private String fullName;

    @Column(name = "year_of_birth")
    @Min(value = 1920, message = "Year can not be less than 1920")
    @NotNull(message = "fill this line")
    private int yearOfBirth;

    @OneToMany(mappedBy = "person")
    private List<Book> bookList;

    @OneToMany(mappedBy = "reservedBy")
    private List<Book> reservedBooks;

    public Person() {
    }

    public Person(String fullName, int yearOfBirth) {
        this.fullName = fullName;
        this.yearOfBirth = yearOfBirth;
    }

}
