package com.pm.accountservice.service;

import com.pm.accountservice.dto.AccountResponseDTO;
import com.pm.accountservice.dto.BalanceResponseDTO;
import com.pm.accountservice.dto.CreateAccountRequestDTO;
import com.pm.accountservice.exception.DuplicateResourceException;
import com.pm.accountservice.exception.InvalidCurrencyException;
import com.pm.accountservice.exception.ResourceNotFoundException;
import com.pm.accountservice.model.Account;
import com.pm.accountservice.model.AccountStatus;
import com.pm.accountservice.model.Customer;
import com.pm.accountservice.repository.AccountRepository;
import com.pm.accountservice.repository.CustomerRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final AccountNumberGenerator accountNumberGenerator;

    public AccountService(AccountRepository accountRepository, CustomerRepository customerRepository, AccountNumberGenerator accountNumberGenerator) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.accountNumberGenerator = accountNumberGenerator;

    }

    @Transactional
    public AccountResponseDTO openAccount(CreateAccountRequestDTO createAccountRequestDTO) {
        Customer customer = customerRepository.findById(createAccountRequestDTO.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + createAccountRequestDTO.customerId()));

        Currency currency;

        try {
            currency = Currency.getInstance(createAccountRequestDTO.currency());
        } catch (IllegalArgumentException e) {
            throw new InvalidCurrencyException("Unknown currency: " + createAccountRequestDTO.currency());
        }

        String accountNumber = accountNumberGenerator.generate();

        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setCustomer(customer);
        account.setBalance(createAccountRequestDTO.initialBalance().setScale(2));
        account.setCurrency(currency);
        account.setStatus(AccountStatus.ACTIVE);
        Account savedAcc = accountRepository.saveAndFlush(account);

        return toResponse(savedAcc);
    }

    @Transactional(readOnly = true)
    public AccountResponseDTO getAccount(UUID accountId) {
        Account existingAccount = accountRepository.findById(accountId).orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));
        return toResponse(existingAccount);
    }

    @Transactional(readOnly = true)
    public BalanceResponseDTO getBalance(UUID accountId) {
        Account existingAccount = accountRepository.findById(accountId).orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));

        return new BalanceResponseDTO(existingAccount.getId(), existingAccount.getBalance(), existingAccount.getCurrency().getCurrencyCode());
    }

    private AccountResponseDTO toResponse(Account account) {
        return new AccountResponseDTO(account.getId(), account.getCustomer().getId(), account.getAccountNumber(), account.getBalance(), account.getCurrency().getCurrencyCode(), account.getStatus(), account.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<AccountResponseDTO> getRelatedAccounts(UUID customerId) {
        if(!customerRepository.existsById(customerId)) throw new ResourceNotFoundException("Customer not found: " + customerId);

        return accountRepository.findByCustomerId(customerId).stream().map(this::toResponse).toList();
    }
}
