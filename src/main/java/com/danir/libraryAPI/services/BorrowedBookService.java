package com.danir.libraryAPI.services;
import com.danir.libraryAPI.models.BorrowedBook;
import com.danir.libraryAPI.models.Person;
import com.danir.libraryAPI.repositories.BorrowedBookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class BorrowedBookService {

    private final BorrowedBookRepository borrowedBookRepository;

    @Autowired
    public BorrowedBookService(BorrowedBookRepository borrowedBookRepository) {
        this.borrowedBookRepository = borrowedBookRepository;
    }


    public List<BorrowedBook> findAll() {
        return borrowedBookRepository.findAll();
    }


    public Optional<BorrowedBook> findById(long id) {
        return borrowedBookRepository.findById(id);
    }

    //receiving all borrowed books of person
    public List<BorrowedBook> findByPerson(Person person) {
        return borrowedBookRepository.findByPerson(person);
    }

    @Transactional
    public void save(BorrowedBook borrowedBook) {
        borrowedBookRepository.save(borrowedBook);
    }

    @Transactional
    public void deleteById(long id) {
        borrowedBookRepository.deleteById((long) id);
    }


}

