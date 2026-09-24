package com.cardfit.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecommendationControllerTests {

    private CreditCardRepository repository;
    private RecommendationController controller;

    @BeforeEach
    void setUp() {
        repository = mock(CreditCardRepository.class);
        controller = new RecommendationController(repository);

        // Deliberately unordered to test ranking.
        when(repository.findAll()).thenReturn(List.of(
            new CreditCard(
                "Quicksilver", "cashback",
                "0", "0.015", "0.015", "0.015"
            ),
            new CreditCard(
                "Venture", "travel",
                "95", "0.02", "0.02", "0.02"
            ),
            new CreditCard(
                "Savor", "cashback",
                "0", "0.03", "0.03", "0.01"
            ),
            new CreditCard(
                "VentureOne", "travel",
                "0", "0.0125", "0.0125", "0.0125"
            )
        ));
    }

    private RecommendationController.Spending sampleSpending() {
        return new RecommendationController.Spending(
            new BigDecimal("300"),
            new BigDecimal("500"),
            new BigDecimal("200"),
            new BigDecimal("100"),
            new BigDecimal("400")
        );
    }

    private RecommendationController.Results recommend(
        String preference,
        String feeLimit
    ) {
        return controller.recommend(
            new RecommendationController.QuizRequest(
                sampleSpending(),
                preference,
                new BigDecimal(feeLimit)
            )
        );
    }

    @Test
    void calculatesCashbackAndRanksHighestValueFirst() {
        var cards = recommend("cashback", "0").recommendations();

        assertEquals(2, cards.size());
        assertEquals("Savor", cards.get(0).name());
        assertEquals(
            new BigDecimal("372.00"),
            cards.get(0).netAnnualValue()
        );
        assertEquals("Quicksilver", cards.get(1).name());
        assertEquals(
            new BigDecimal("270.00"),
            cards.get(1).netAnnualValue()
        );
    }

    @Test
    void respectsFeeLimitAndIncludesExactBoundary() {
        var below = recommend("travel", "94").recommendations();

        assertEquals(1, below.size());
        assertEquals("VentureOne", below.get(0).name());

        var atBoundary = recommend("travel", "95").recommendations();

        assertEquals(2, atBoundary.size());
        assertTrue(
            atBoundary.stream().allMatch(
                card -> card.rewardType().equals("travel")
            )
        );

        var venture = atBoundary.stream()
            .filter(card -> card.name().equals("Venture"))
            .findFirst()
            .orElseThrow();

        assertEquals(
            new BigDecimal("360.00"),
            venture.annualRewards()
        );
        assertEquals(
            new BigDecimal("265.00"),
            venture.netAnnualValue()
        );
    }

    @Test
    void eitherPreferenceRanksAllCardsByNetValue() {
        var names = recommend("either", "400")
            .recommendations()
            .stream()
            .map(RecommendationController.Recommendation::name)
            .toList();

        assertEquals(
            List.of("Savor", "Quicksilver", "Venture", "VentureOne"),
            names
        );
    }

    @Test
    void zeroSpendingStillSubtractsAnnualFee() {
        var zero = BigDecimal.ZERO;
        var spending = new RecommendationController.Spending(
            zero, zero, zero, zero, zero
        );

        var results = controller.recommend(
            new RecommendationController.QuizRequest(
                spending, "travel", new BigDecimal("95")
            )
        );

        var cards = results.recommendations();

        assertEquals("VentureOne", cards.get(0).name());
        assertEquals(
            new BigDecimal("0.00"),
            cards.get(0).netAnnualValue()
        );
        assertEquals("Venture", cards.get(1).name());
        assertEquals(
            new BigDecimal("-95.00"),
            cards.get(1).netAnnualValue()
        );
    }

    @Test
    void emptyCatalogReturnsNoRecommendations() {
        when(repository.findAll()).thenReturn(List.of());

        assertTrue(
            recommend("either", "400").recommendations().isEmpty()
        );
    }
}