package com.danir.libraryAPI.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "person")
@Data
public class Person implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "person_id", nullable = false)
    private int personId;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = new HashSet<>();

    @Column(name = "full_name")
    @Size(min = 2, max = 32, message = "name must be between 2 and 32 characters")
    @NotNull(message = "full name can not be empty")
    private String fullName;

    @Column(nullable = false, unique = true)
    @Email(message = "Invalid email format")
    private String email;

    @Column(name = "year_of_birth")
    @Min(value = 1920, message = "Year can not be less than 1920")
    @Max(value = 2025, message = "Year of birth cannot be in the future")
    @NotNull(message = "please provide year")
    private Integer yearOfBirth;

    @Column(nullable = false)
    private String password;

    @OneToMany(mappedBy = "person")
    private List<Book> bookList;

    @OneToMany(mappedBy = "person", cascade = CascadeType.ALL)
    private List<BorrowedBook> borrowedBeforeBooks;

    @OneToMany(mappedBy = "reservedBy")
    private List<Book> reservedBooks;


    public Person() {
    }

    public Person(String fullName, Integer yearOfBirth, String email, String password) {
        this.email = email;
        this.fullName = fullName;
        this.yearOfBirth = yearOfBirth;
        this.password = password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles;
    }

    @Override
    public String getUsername() {
        return fullName;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
