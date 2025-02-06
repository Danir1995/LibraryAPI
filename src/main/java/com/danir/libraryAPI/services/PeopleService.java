package com.danir.libraryAPI.services;

import com.danir.libraryAPI.models.Book;
import com.danir.libraryAPI.models.Person;
import com.danir.libraryAPI.models.Role;
import com.danir.libraryAPI.repositories.BookRepository;
import com.danir.libraryAPI.repositories.PeopleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@Slf4j
public class PeopleService {

    private final PeopleRepository peopleRepository;
    private final BookRepository bookRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public PeopleService(PeopleRepository peopleRepository, BookRepository bookRepository, PasswordEncoder passwordEncoder) {
        this.peopleRepository = peopleRepository;
        this.bookRepository = bookRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Person> findAll() {
        log.info("Fetching all people from the database");
        return peopleRepository.findAll();
    }

    public Person findOne(int id) {
        log.info("Fetching person with id: {}", id);
        Optional<Person> person = peopleRepository.findById(id);
        return person.orElse(null);
    }

    public Person findByUsername(String username) {
        log.info("Searching for user by username: {}", username);
        return peopleRepository.findByFullNameIgnoreCase(username)
                .orElseThrow(() -> {
                    log.warn("User not found with username: {}", username);
                    return new UsernameNotFoundException("User not found with username: " + username);
                });
    }

    @Transactional
    public void save(Person person) {
        log.info("Saving new person with username: {}", person.getFullName());
        person.setPassword(passwordEncoder.encode(person.getPassword()));
        person.getRoles().add(Role.ROLE_USER);
        peopleRepository.save(person);
        log.info("Person saved successfully: {}", person.getFullName());
    }

    @Transactional
    public void update(int id, Person person) {
        log.info("Updating person with id: {}", id);
        person.setPersonId(id);
        Person existingPerson = peopleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Person not found"));

        if (person.getPassword() == null || person.getPassword().isEmpty() || person.getPassword().equals(existingPerson.getPassword())) {
            person.setPassword(existingPerson.getPassword());
        } else {
            person.setPassword(passwordEncoder.encode(person.getPassword()));
        }

        if (person.getRoles() == null || person.getRoles().isEmpty()) {
            person.setRoles(existingPerson.getRoles());
        }

        peopleRepository.save(person);
        log.info("Person updated successfully: {}", id);
    }

    @Transactional
    public void delete(int id) {
        log.info("Deleting person with id: {}", id);
        Person person = peopleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Person not found"));

        for (Book book : person.getReservedBooks()) {
            book.setReservedBy(null);
            bookRepository.save(book);
        }

        peopleRepository.deleteById(id);
        log.info("Person deleted successfully: {}", id);
    }

    public Optional<Person> show(String fullName) {
        log.info("Fetching person by full name: {}", fullName);
        return peopleRepository.findByFullNameIgnoreCase(fullName);
    }

    public boolean emailExists(String email) {
        log.info("Checking if email exists");
        return peopleRepository.findByEmail(email).isPresent();
    }
}
