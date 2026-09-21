package com.pm.accountservice.dto;

import java.util.UUID;

public record CustomerResponseDTO(
        UUID id,
        String name,
        String email
) {}
