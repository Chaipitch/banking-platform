package com.pm.accountservice;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests for the account-service REST API.
 * <p>
 * Runs against a real PostgreSQL started by Testcontainers (never H2), so Liquibase
 * migrations, column types and constraints are exercised exactly as in production.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AccountApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    /** Keeps emails/national IDs unique across tests - the container is shared by the whole class. */
    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    MockMvc mockMvc;

    @Test
    void createCustomer_returns201WithId() throws Exception {
        int n = SEQ.incrementAndGet();

        mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customerJson("Bob " + n, "bob" + n + "@mail.com", nationalId(n))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("bob" + n + "@mail.com"));
    }

    @Test
    void createCustomer_withDuplicateEmail_returns409() throws Exception {
        int n = SEQ.incrementAndGet();
        String email = "dup" + n + "@mail.com";

        mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customerJson("First", email, nationalId(n))))
                .andExpect(status().isCreated());

        // same email, different national ID -> the email check must reject it
        mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customerJson("Second", email, nationalId(SEQ.incrementAndGet()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate Resource"));
    }

    @Test
    void createCustomer_withBlankName_returns400() throws Exception {
        int n = SEQ.incrementAndGet();

        mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customerJson("  ", "blank" + n + "@mail.com", nationalId(n))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void openAccount_returns201WithGeneratedNumberAndScaledBalance() throws Exception {
        UUID customerId = createCustomer();

        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountJson(customerId, "100", "THB")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.accountNumber").value(org.hamcrest.Matchers.matchesPattern("\\d{10}")))
                .andExpect(jsonPath("$.balance").value(100.00))
                .andExpect(jsonPath("$.currency").value("THB"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void openAccount_withNegativeBalance_returns400() throws Exception {
        UUID customerId = createCustomer();

        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountJson(customerId, "-500", "THB")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.initialBalance").exists());
    }

    @Test
    void openAccount_withUnknownCurrency_returns400() throws Exception {
        UUID customerId = createCustomer();

        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountJson(customerId, "100", "ABC")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Currency"));
    }

    @Test
    void openAccount_withUnknownCustomer_returns404() throws Exception {
        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountJson(UUID.randomUUID(), "100", "THB")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    void getAccount_returns200() throws Exception {
        UUID accountId = createAccount(createCustomer(), "250.75", "THB");

        mockMvc.perform(get("/accounts/{id}", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accountId.toString()))
                .andExpect(jsonPath("$.balance").value(250.75));
    }

    @Test
    void getAccount_withUnknownId_returns404() throws Exception {
        mockMvc.perform(get("/accounts/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getBalance_returnsOnlyBalanceAndCurrency() throws Exception {
        UUID accountId = createAccount(createCustomer(), "42.50", "USD");

        mockMvc.perform(get("/accounts/{id}/balance", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.balance").value(42.50))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.accountNumber").doesNotExist());
    }

    @Test
    void getRelatedAccounts_returnsAllAccountsOfThatCustomer() throws Exception {
        UUID customerId = createCustomer();
        createAccount(customerId, "10.00", "THB");
        createAccount(customerId, "20.00", "USD");

        mockMvc.perform(get("/customers/{id}/accounts", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].customerId", org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.is(customerId.toString()))));
    }

    @Test
    void getRelatedAccounts_withUnknownCustomer_returns404() throws Exception {
        mockMvc.perform(get("/customers/{id}/accounts", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    // --- helpers -------------------------------------------------------------

    private UUID createCustomer() throws Exception {
        int n = SEQ.incrementAndGet();
        String body = mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customerJson("Customer " + n, "customer" + n + "@mail.com", nationalId(n))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return UUID.fromString(readId(body));
    }

    private UUID createAccount(UUID customerId, String balance, String currency) throws Exception {
        String body = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountJson(customerId, balance, currency)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return UUID.fromString(readId(body));
    }

    private static String readId(String json) {
        return JsonPath.read(json, "$.id");
    }

    private static String customerJson(String name, String email, String nationalId) {
        return """
                {"name": "%s", "email": "%s", "nationalId": "%s"}
                """.formatted(name, email, nationalId);
    }

    private static String accountJson(UUID customerId, String initialBalance, String currency) {
        return """
                {"customerId": "%s", "initialBalance": %s, "currency": "%s"}
                """.formatted(customerId, initialBalance, currency);
    }

    /** 13-digit national ID, unique per test. */
    private static String nationalId(int n) {
        return String.format("%013d", 1_000_000_000L + n);
    }
}
