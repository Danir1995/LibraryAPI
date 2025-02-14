package com.danir.libraryAPI.services;

import com.danir.libraryAPI.models.Person;
import com.danir.libraryAPI.models.Role;
import com.danir.libraryAPI.repositories.PeopleRepository;
import com.danir.libraryAPI.util.PersonDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PersonDetailsServiceTest {

    @Mock
    private PeopleRepository peopleRepository;

    @InjectMocks
    private PersonDetailsService personDetailsService;

    @Test
    void loadUserByUsername_ShouldReturnPersonDetails_WhenExists() {
        // Arrange
        Person testPerson = new Person();
        Set<Role> roles = new HashSet<>();
        roles.add(Role.ROLE_ADMIN);
        testPerson.setFullName("Danir");
        testPerson.setRoles(roles);
        when(peopleRepository.findByFullNameIgnoreCase("Danir")).thenReturn(Optional.of(testPerson));

        // Act
        UserDetails result = personDetailsService.loadUserByUsername("Danir");

        // Assert
        assertNotNull(result);
        assertInstanceOf(PersonDetails.class, result);
        PersonDetails personDetails = (PersonDetails) result;
        assertEquals("Danir", personDetails.getUsername());
    }


}
