package com.danir.libraryAPI.util;

import com.danir.libraryAPI.models.Person;
import com.danir.libraryAPI.models.Role;
import com.danir.libraryAPI.repositories.PeopleRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.boot.CommandLineRunner;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig{

     @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/resources/**", "/css/**").permitAll() // access to static resources
                        .anyRequest().authenticated() // All queries needs authentication
                )
                .formLogin(form -> form
                        .defaultSuccessUrl("/books", true) // redirect after successful login
                )
                .logout(logout -> logout
                        .logoutUrl("/logout") // URL for logout
                        .logoutSuccessUrl("/login") // redirect after successful logout
                )
                .build();
    }

    @Bean
    public AuthenticationManager authManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CommandLineRunner dataInitializer(PeopleRepository peopleRepository) {
        return args -> {
            // Check if exists admin with this email
            if (peopleRepository.findByEmail("admin@example.com").isEmpty()) {
                Person admin = new Person();
                admin.setFullName("Admin");
                admin.setEmail("admin@example.com");
                admin.setYearOfBirth(1995);
                admin.setPassword(new BCryptPasswordEncoder().encode("password")); // Encrypt password
                admin.getRoles().add(Role.ROLE_ADMIN);
                peopleRepository.save(admin); // Save admin in db
            }

            if (peopleRepository.findByEmail("user@example.com").isEmpty()) {
                Person user = new Person();
                user.setFullName("Bambi");
                user.setEmail("user@example.com");
                user.setYearOfBirth(1995);
                user.setPassword(new BCryptPasswordEncoder().encode("password"));
                user.getRoles().add(Role.ROLE_USER);
                peopleRepository.save(user);
            }
        };
    }
}
