package com.danir.libraryAPI.repositories;

import com.danir.libraryAPI.models.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Integer> {
    @Query("from Book b order by b.year")
    Page<Book> findAll(Pageable pageable);
    @Query("from Book b where b.name like :title%")
    List<Book> findByTitleStartingWith(@Param("title") String title);
    Page<Book> findByPersonIsNullAndReservedByIsNull(Pageable pageable);
    @Query("select b from Book b where b.borrowedDate is not null and b.borrowedDate <= :tenDaysAgo")
    List<Book> findOverdueBooks(OffsetDateTime tenDaysAgo);
}
