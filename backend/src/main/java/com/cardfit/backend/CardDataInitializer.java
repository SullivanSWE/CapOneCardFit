package com.cardfit.backend;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class CardDataInitializer implements CommandLineRunner {

    private final CreditCardRepository repository;

    public CardDataInitializer(CreditCardRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        List<CreditCard> initialCards = List.of(
            new CreditCard(
                "Savor", "cashback",
                "0", "0.03", "0.03", "0.01"
            ),
            new CreditCard(
                "Quicksilver", "cashback",
                "0", "0.015", "0.015", "0.015"
            ),
            new CreditCard(
                "VentureOne", "travel",
                "0", "0.0125", "0.0125", "0.0125"
            ),
            new CreditCard(
                "Venture", "travel",
                "95", "0.02", "0.02", "0.02"
            )
        );

        for (CreditCard card : initialCards) {
            if (!repository.existsById(card.name())) {
                repository.save(card);
            }
        }
    }
}