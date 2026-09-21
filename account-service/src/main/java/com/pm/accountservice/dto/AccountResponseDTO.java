package com.pm.accountservice.dto;

import com.pm.accountservice.model.AccountStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponseDTO(
        UUID id,
        UUID customerId,
        String accountNumber,
        BigDecimal balance,
        String currency,
        AccountStatus status,
        Instant createdAt
) {}
