package com.danir.libraryAPI.services;
import com.danir.libraryAPI.models.BorrowedBook;
import com.danir.libraryAPI.models.Person;
import com.danir.libraryAPI.repositories.BorrowedBookRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Transactional(readOnly = true)
@Slf4j
public class BorrowedBookService {

    private final BorrowedBookRepository borrowedBookRepository;

    @Autowired
    public BorrowedBookService(BorrowedBookRepository borrowedBookRepository) {
        this.borrowedBookRepository = borrowedBookRepository;
    }

    //receiving all borrowed books of person
    public Set<BorrowedBook> findByPerson(Person person) {
        log.info("Fetching borrowed book by person : {}", person.getFullName());
        return borrowedBookRepository.findByPerson(person);
    }

    @Transactional
    public void save(BorrowedBook borrowedBook) {
        log.info("Saving borrowed before book");
        borrowedBookRepository.save(borrowedBook);
    }
}