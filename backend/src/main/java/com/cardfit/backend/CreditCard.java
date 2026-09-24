package com.cardfit.backend;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "credit_cards")
public class CreditCard {

    @Id
    private String name;

    @Column(nullable = false)
    private String rewardType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal annualFee;

    @Column(nullable = false, precision = 8, scale = 5)
    private BigDecimal diningRate;

    @Column(nullable = false, precision = 8, scale = 5)
    private BigDecimal groceryRate;

    @Column(nullable = false, precision = 8, scale = 5)
    private BigDecimal baseRate;

    protected CreditCard() {
        // Required by JPA.
    }

    public CreditCard(
        String name,
        String rewardType,
        String annualFee,
        String diningRate,
        String groceryRate,
        String baseRate
    ) {
        this.name = name;
        this.rewardType = rewardType;
        this.annualFee = new BigDecimal(annualFee);
        this.diningRate = new BigDecimal(diningRate);
        this.groceryRate = new BigDecimal(groceryRate);
        this.baseRate = new BigDecimal(baseRate);
    }

    public String name() {
        return name;
    }

    public String rewardType() {
        return rewardType;
    }

    public BigDecimal annualFee() {
        return annualFee;
    }

    public BigDecimal diningRate() {
        return diningRate;
    }

    public BigDecimal groceryRate() {
        return groceryRate;
    }

    public BigDecimal baseRate() {
        return baseRate;
    }
}