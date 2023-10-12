package com.danir.libraryAPI.repositories;

import com.danir.libraryAPI.models.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNullApi;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Integer> {
    @Query("from Book b order by b.year")
    Page<Book> findAll(Pageable pageable);
    @Query("from Book b where b.name like :title%")
    List<Book> findByTitleStartingWith(@Param("title") String title);
}
