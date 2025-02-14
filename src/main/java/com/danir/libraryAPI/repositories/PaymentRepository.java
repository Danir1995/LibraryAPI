package com.danir.libraryAPI.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.danir.libraryAPI.models.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
}

