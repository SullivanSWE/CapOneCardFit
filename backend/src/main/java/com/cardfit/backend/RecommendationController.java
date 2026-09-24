package com.cardfit.backend;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api")
public class RecommendationController {

    private final CreditCardRepository repository;

    public RecommendationController(CreditCardRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/recommendations")
    public Results recommend(@Valid @RequestBody QuizRequest request) {

        List<Recommendation> matches = repository.findAll().stream()
            .filter(card ->
                card.annualFee().compareTo(request.maxAnnualFee()) <= 0
            )
            .filter(card ->
                request.rewardPreference().equals("either")
                    || card.rewardType().equals(request.rewardPreference())
            )
            .map(card -> calculate(card, request.spending()))
            .sorted(
                Comparator.comparing(Recommendation::netAnnualValue)
                    .reversed()
                    .thenComparing(Recommendation::name)
            )
            .toList();

        return new Results(
            matches,
            List.of(
                "Estimates assume the same spending each month and "
                    + "balances paid in full; interest is not included.",
                "Travel miles are valued at an assumed 1 cent each. "
                    + "Actual redemption values vary.",
                "Welcome bonuses, credits, perks and special booking "
                    + "rates are excluded.",
                "Grocery spending means eligible grocery stores, "
                    + "excluding superstores such as Walmart and Target.",
                "Other purchases use the base rate, including any "
                    + "entertainment or streaming entered there.",
                "Results compare the cards in our catalog and do not "
                    + "predict approval."
            )
        );
    }

    private Recommendation calculate(CreditCard card, Spending spending) {

        BigDecimal baseSpending = spending.gas()
            .add(spending.travel())
            .add(spending.other());

        BigDecimal annualRewards = spending.dining()
            .multiply(card.diningRate())
            .add(spending.groceries().multiply(card.groceryRate()))
            .add(baseSpending.multiply(card.baseRate()))
            .multiply(new BigDecimal("12"))
            .setScale(2, RoundingMode.HALF_UP);

        BigDecimal netValue = annualRewards
            .subtract(card.annualFee())
            .setScale(2, RoundingMode.HALF_UP);

        String explanation = String.format(
            "Your spending produces an estimated $%s in annual reward "
                + "value, minus a $%s annual fee.",
            annualRewards.toPlainString(),
            card.annualFee().toPlainString()
        );

        return new Recommendation(
            card.name(),
            card.rewardType(),
            card.annualFee(),
            annualRewards,
            netValue,
            explanation
        );
    }

    public record Spending(
        @NotNull @DecimalMin("0") @DecimalMax("100000")
        BigDecimal dining,

        @NotNull @DecimalMin("0") @DecimalMax("100000")
        BigDecimal groceries,

        @NotNull @DecimalMin("0") @DecimalMax("100000")
        BigDecimal gas,

        @NotNull @DecimalMin("0") @DecimalMax("100000")
        BigDecimal travel,

        @NotNull @DecimalMin("0") @DecimalMax("100000")
        BigDecimal other
    ) {}

    public record QuizRequest(
        @NotNull @Valid Spending spending,

        @NotNull @Pattern(regexp = "cashback|travel|either")
        String rewardPreference,

        @NotNull @DecimalMin("0") @DecimalMax("400")
        BigDecimal maxAnnualFee
    ) {}

    public record Recommendation(
        String name,
        String rewardType,
        BigDecimal annualFee,
        BigDecimal annualRewards,
        BigDecimal netAnnualValue,
        String explanation
    ) {}

    public record Results(
        List<Recommendation> recommendations,
        List<String> assumptions
    ) {}
}