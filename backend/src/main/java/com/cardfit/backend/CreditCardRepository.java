package com.cardfit.backend;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditCardRepository
        extends JpaRepository<CreditCard, String> {
}