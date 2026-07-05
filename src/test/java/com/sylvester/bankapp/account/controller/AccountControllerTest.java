package com.sylvester.bankapp.account.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sylvester.bankapp.account.dto.AccountResponse;
import com.sylvester.bankapp.account.dto.CreateAccountRequest;
import com.sylvester.bankapp.account.entity.Account;
import com.sylvester.bankapp.account.service.AccountService;
import com.sylvester.bankapp.support.TestJwtArgumentResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    private AccountService accountService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("user-123").claim("sub", "user-123").build();
        mockMvc = MockMvcBuilders.standaloneSetup(new AccountController(accountService))
                .setCustomArgumentResolvers(new TestJwtArgumentResolver(jwt))
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void createAccountReturnsSuccessAndPassesAuthenticatedUserId() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest("SAVINGS");

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Account created successfully"));

        verify(accountService).createAccount(request, "user-123");
    }

    @Test
    void findAllAccountsReturnsAccounts() throws Exception {
        Account account = Account.builder().id(1L).accountNumber("HR123").build();
        when(accountService.getAllAccounts()).thenReturn(List.of(account));

        mockMvc.perform(get("/api/accounts/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].accountNumber").value("HR123"));

        verify(accountService).getAllAccounts();
    }

    @Test
    void findAllUsersAccountsReturnsAuthenticatedUsersAccounts() throws Exception {
        AccountResponse response = new AccountResponse(1L, "HR456", "SAVINGS", "Sylvester Onah", new BigDecimal("10.00"), "EURO");
        when(accountService.getAllUsersAccounts("user-123")).thenReturn(List.of(response));

        mockMvc.perform(get("/api/accounts/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountNumber").value("HR456"))
                .andExpect(jsonPath("$[0].accountType").value("SAVINGS"))
                .andExpect(jsonPath("$[0].accountHolderName").value("Sylvester Onah"));

        verify(accountService).getAllUsersAccounts("user-123");
    }
}
