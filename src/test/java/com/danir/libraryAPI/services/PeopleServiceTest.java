package com.danir.libraryAPI.services;

import com.danir.libraryAPI.models.Person;
import com.danir.libraryAPI.models.Role;
import com.danir.libraryAPI.repositories.BookRepository;
import com.danir.libraryAPI.repositories.PeopleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PeopleServiceTest {

    @Mock
    private PeopleRepository peopleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PeopleService peopleService;

    private Person testPerson;

    @BeforeEach
    void setUp() {
        testPerson = new Person();
        testPerson.setPersonId(1);
        testPerson.setFullName("Danir");
        testPerson.setEmail("danir@example.com");
        testPerson.setPassword("hashedPassword");
        testPerson.setRoles(Collections.singleton(Role.ROLE_USER));
    }

    @Test
    void findAll_ShouldReturnListOfPeople() {
        when(peopleRepository.findAll()).thenReturn(List.of(testPerson));

        List<Person> result = peopleService.findAll();

        assertEquals(1, result.size());
        assertEquals("Danir", result.get(0).getFullName());
        verify(peopleRepository, times(1)).findAll();
    }

    @Test
    void findOne_ShouldReturnPerson_WhenExists() {
        when(peopleRepository.findById(1)).thenReturn(Optional.of(testPerson));

        Person result = peopleService.findOne(1);

        assertNotNull(result);
        assertEquals("Danir", result.getFullName());
    }

    @Test
    void findOne_ShouldReturnNull_WhenNotExists() {
        when(peopleRepository.findById(2)).thenReturn(Optional.empty());

        Person result = peopleService.findOne(2);

        assertNull(result);
    }

    @Test
    void findByUsername_ShouldReturnPerson_WhenExists() {
        when(peopleRepository.findByFullNameIgnoreCase("Danir")).thenReturn(Optional.of(testPerson));

        Person result = peopleService.findByUsername("Danir");

        assertNotNull(result);
        assertEquals("Danir", result.getFullName());
    }

    @Test
    void findByUsername_ShouldThrowException_WhenNotExists() {
        when(peopleRepository.findByFullNameIgnoreCase("Unknown")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> peopleService.findByUsername("Unknown"));
    }

    @Test
    void save_ShouldEncodePasswordAndSavePerson() {
        Person newPerson = new Person();
        newPerson.setFullName("NewUser");
        newPerson.setPassword("rawPassword");

        when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");

        peopleService.save(newPerson);

        assertEquals("encodedPassword", newPerson.getPassword());
        assertTrue(newPerson.getRoles().contains(Role.ROLE_USER));
        verify(peopleRepository, times(1)).save(newPerson);
    }

    @Test
    void update_ShouldUpdatePersonFields() {
        Person updatedPerson = new Person();
        updatedPerson.setPassword("newPassword");

        when(peopleRepository.findById(1)).thenReturn(Optional.of(testPerson));
        when(passwordEncoder.encode("newPassword")).thenReturn("hashedNewPassword");

        peopleService.update(1, updatedPerson);

        assertEquals("hashedNewPassword", testPerson.getPassword());
        verify(peopleRepository, times(1)).save(testPerson);
    }

    @Test
    void delete_ShouldRemovePersonAndReleaseBooks() {
        when(peopleRepository.findById(1)).thenReturn(Optional.of(testPerson));

        peopleService.delete(1);

        verify(peopleRepository, times(1)).deleteById(1);
    }

    @Test
    void delete_ShouldThrowException_WhenPersonNotFound() {
        when(peopleRepository.findById(2)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> peopleService.delete(2));
    }

    @Test
    void emailExists_ShouldReturnTrue_WhenEmailExists() {
        when(peopleRepository.findByEmail("danir@example.com")).thenReturn(Optional.of(testPerson));

        assertTrue(peopleService.emailExists("danir@example.com"));
    }

    @Test
    void emailExists_ShouldReturnFalse_WhenEmailNotExists() {
        when(peopleRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertFalse(peopleService.emailExists("unknown@example.com"));
    }
}
