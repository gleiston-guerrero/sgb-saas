package com.uteq.backend.p5spike;

import com.uteq.backend.entity.Book;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Repositorio solo-spike (test): prueba `function('similarity',...)` en JPQL. */
public interface SpikeBookRepo extends Repository<Book, Long> {
    @Query("SELECT b FROM Book b WHERE b.status.name = :status "
            + "AND function('similarity', b.title, :t) > 0.1 "
            + "ORDER BY function('similarity', b.title, :t) DESC")
    List<Book> suggest(@Param("t") String text, @Param("status") String status, Pageable pageable);
}
