package com.danir.libraryAPI.util;

import com.danir.libraryAPI.dto.BookDTO;
import com.danir.libraryAPI.models.Book;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        modelMapper.createTypeMap(Book.class, BookDTO.class)
                .addMappings(mapper -> mapper.map(src -> src.getPerson().getFullName(), BookDTO::setPerson_name))
                .addMappings(mapper -> mapper.map(src -> src.getReservedBy().getFullName(), BookDTO::setReserved_by_name));

        modelMapper.createTypeMap(BookDTO.class, Book.class)
                .addMappings(mapper -> {
                    mapper.skip(Book::setPerson);
                    mapper.skip(Book::setReservedBy);
                });

        return modelMapper;
    }
}