package com.pm.accountservice.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateAccountRequestDTO(
        @NotNull UUID customerId,
        @NotNull @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal initialBalance,
        @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "must be a 3-letter currency code, e.g. THB") String currency
) {}
