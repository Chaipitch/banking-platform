package com.pm.accountservice.controller;

import com.pm.accountservice.dto.AccountResponseDTO;
import com.pm.accountservice.dto.BalanceResponseDTO;
import com.pm.accountservice.dto.CreateAccountRequestDTO;
import com.pm.accountservice.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/accounts")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponseDTO> getAccount(@PathVariable UUID id) {
        AccountResponseDTO account = accountService.getAccount(id);

        return ResponseEntity.ok().body(account);
    }

    @PostMapping
    public ResponseEntity<AccountResponseDTO> createAccount(@Valid @RequestBody CreateAccountRequestDTO req) {
        AccountResponseDTO account = accountService.openAccount(req);

        return ResponseEntity.status(HttpStatus.CREATED).body(account);
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<BalanceResponseDTO> getAccountBalance(@PathVariable UUID id) {
        BalanceResponseDTO balanceRes = accountService.getBalance(id);

        return ResponseEntity.ok().body(balanceRes);
    }
}
