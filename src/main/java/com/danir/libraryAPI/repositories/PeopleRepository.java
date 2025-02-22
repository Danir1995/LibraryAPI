package com.danir.libraryAPI.repositories;

import com.danir.libraryAPI.models.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PeopleRepository extends JpaRepository<Person, Integer> {
    @Query("SELECT p FROM Person p WHERE LOWER(p.fullName) = LOWER(:fullName)")
    Optional<Person> findByFullNameIgnoreCase(@Param("fullName") String fullName);
    Optional<Person> findByEmail(String email);
    @Query("SELECT p FROM Person p LEFT JOIN FETCH p.bookList WHERE p.personId = :id")
    Optional<Person> findByIdWithBooks(@Param("id") int id);

}
