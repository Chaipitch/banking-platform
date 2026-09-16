package com.pm.accountservice.model;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class AccountTest {
    private Validator validator;

    @BeforeEach
    public void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void balanceWithTwoDecimalPlaces_isValid() {
        Account account = new Account();
        account.setBalance(new BigDecimal("100.00"));
        Set<ConstraintViolation<Account>> constraintViolations = validator.validate(account);
        assertThat(constraintViolations).isEmpty();
    }

    @Test
    void balanceWithThreeDecimalPlaces_isInvalid() {
        Account account = new Account();
        account.setBalance(new BigDecimal("100.123"));
        Set<ConstraintViolation<Account>> constraintViolations = validator.validate(account);
        assertThat(constraintViolations).hasSize(1);
        assertThat(constraintViolations.iterator().next().getPropertyPath().toString())
                .isEqualTo("balance");
    }
}
