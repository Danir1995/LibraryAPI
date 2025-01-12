package com.danir.libraryAPI.repositories;

import com.danir.libraryAPI.models.BorrowedBook;
import com.danir.libraryAPI.models.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BorrowedBookRepository extends JpaRepository<BorrowedBook, Long> {

    List<BorrowedBook> findByPerson(Person person);
}

