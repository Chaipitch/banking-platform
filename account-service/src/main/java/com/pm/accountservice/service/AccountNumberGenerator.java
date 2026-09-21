package com.pm.accountservice.service;

import com.pm.accountservice.repository.AccountRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class AccountNumberGenerator {
    private final AccountRepository accountRepository;
    private final SecureRandom random = new SecureRandom();
    public AccountNumberGenerator(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public String generate(){
        String accountNumber;

        do {
            StringBuilder sb = new StringBuilder(10);
            for (int i = 0; i < 10; i++) {
                sb.append(random.nextInt(10));
            }
            accountNumber = sb.toString();

        } while (accountRepository.existsByAccountNumber(accountNumber));

        return accountNumber;
    }
}
