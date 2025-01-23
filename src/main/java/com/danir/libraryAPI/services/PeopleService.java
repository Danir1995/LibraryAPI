package com.danir.libraryAPI.services;

import com.danir.libraryAPI.models.Book;
import com.danir.libraryAPI.models.Person;
import com.danir.libraryAPI.models.Role;
import com.danir.libraryAPI.repositories.BookRepository;
import com.danir.libraryAPI.repositories.PeopleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
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

    public List<Person> findAll(){
        return peopleRepository.findAll();
    }

    public Person findOne(int id){
        Optional<Person> person = peopleRepository.findById(id);
        return person.orElse(null);
    }

    public Person findByUsername(String username) {
        return peopleRepository.findByFullNameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    @Transactional
    public void save(Person person){
        person.setPassword(passwordEncoder.encode(person.getPassword()));
        if (!person.getRoles().contains(Role.ROLE_USER)) {
            person.getRoles().add(Role.ROLE_USER);
        }
        peopleRepository.save(person);
    }

    @Transactional
    public void update(int id, Person person){
        person.setPersonId(id);
        Person existingPerson = peopleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Person not found"));

        // If password didn't change
        if (person.getPassword() == null || person.getPassword().isEmpty() || person.getPassword().equals(existingPerson.getPassword())) {
            person.setPassword(existingPerson.getPassword());
        } else {
            //if password changed - encrypt it
            person.setPassword(passwordEncoder.encode(person.getPassword()));
        }

        //copy role from existing object if it didn't change
        if (person.getRoles() == null || person.getRoles().isEmpty()) {
            person.setRoles(existingPerson.getRoles());
        }

        peopleRepository.save(person);
    }

    @Transactional
    public void delete(int id){
        Person person = peopleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Person not found"));

        for (Book book : person.getReservedBooks()) {
            book.setReservedBy(null);
            bookRepository.save(book); // save changes
        }

        peopleRepository.deleteById(id);
    }

    public Optional<Person> show(String fullName) {
       return peopleRepository.findByFullNameIgnoreCase(fullName);
    }

    public boolean emailExists(String email) {
        return peopleRepository.findByEmail(email).isPresent();
    }

}
