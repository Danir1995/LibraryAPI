package com.danir.libraryAPI.services;

import com.danir.libraryAPI.models.Person;
import com.danir.libraryAPI.repositories.PeopleRepository;
import com.danir.libraryAPI.util.PersonDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PersonDetailsService implements UserDetailsService {

    private final PeopleRepository peopleRepository;

    public PersonDetailsService(PeopleRepository peopleRepository) {
        this.peopleRepository = peopleRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Person person = peopleRepository.findByFullNameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        log.info("User loaded: {}, roles: {}", person.getFullName(), person.getRoles());
        return new PersonDetails(person);
    }
}
