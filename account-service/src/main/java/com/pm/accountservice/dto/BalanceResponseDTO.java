package com.pm.accountservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BalanceResponseDTO (
        UUID accountId,
        BigDecimal balance,
        String currency
) {}
