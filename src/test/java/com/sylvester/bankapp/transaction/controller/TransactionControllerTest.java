package com.sylvester.bankapp.transaction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sylvester.bankapp.support.TestJwtArgumentResolver;
import com.sylvester.bankapp.transaction.dto.BankStatementRequest;
import com.sylvester.bankapp.transaction.dto.DepositAndWithdrawRequest;
import com.sylvester.bankapp.transaction.dto.TransactionDto;
import com.sylvester.bankapp.transaction.dto.TransferRequest;
import com.sylvester.bankapp.transaction.entity.TransactionStatus;
import com.sylvester.bankapp.transaction.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("user-123").claim("sub", "user-123").build();
        mockMvc = MockMvcBuilders.standaloneSetup(new TransactionController(transactionService))
                .setCustomArgumentResolvers(new TestJwtArgumentResolver(jwt))
                .build();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    void transferCallsServiceWithIdempotencyKeyAndAuthenticatedUserId() throws Exception {
        TransferRequest request = new TransferRequest("HR1", "HR2", "Recipient", BigDecimal.TEN, "Test", "1234");

        mockMvc.perform(post("/api/transactions/transfer")
                        .header("idempotency-key", "idem-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Transfer successful"));

        verify(transactionService).transfer(request, "idem-123", "user-123");
    }

    @Test
    void withdrawAndDepositCallServiceWithAuthenticatedUserId() throws Exception {
        DepositAndWithdrawRequest request = new DepositAndWithdrawRequest("HR1", new BigDecimal("25.00"));

        mockMvc.perform(post("/api/transactions/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Withdraw successful"));

        mockMvc.perform(post("/api/transactions/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Deposit successful"));

        verify(transactionService).withDrawMoney(request, "user-123");
        verify(transactionService).depositMoney(request, "user-123");
    }

    @Test
    void downloadReceiptReturnsPdfWithAttachmentHeader() throws Exception {
        byte[] pdf = "pdf-bytes".getBytes();
        when(transactionService.getReceipt("tx-123")).thenReturn(pdf);

        mockMvc.perform(get("/api/transactions/receipt/tx-123"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tx-123.pdf"))
                .andExpect(content().bytes(pdf));

        verify(transactionService).getReceipt("tx-123");
    }

    @Test
    void statementCallsServiceWithRequestDatesAndAuthenticatedUserId() throws Exception {
        BankStatementRequest request = new BankStatementRequest("HR1", "2026-01-01", "2026-01-31");

        mockMvc.perform(post("/api/transactions/statement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Statement successful sent"));

        verify(transactionService).statementOfAccount("HR1", "2026-01-01", "2026-01-31", "user-123");
    }

    @Test
    void findAllByAccountIdReturnsLastTenTransactions() throws Exception {
        TransactionDto dto = new TransactionDto("tx-123", BigDecimal.TEN, "TRANSFER", "DEBIT", "Transfer to Recipient", LocalDate.of(2026, 1, 1), TransactionStatus.SUCCESS);
        when(transactionService.getLastTenTransactions("user-123", 1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/transactions/me/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].transactionType").value("TRANSFER"))
                .andExpect(jsonPath("$[0].direction").value("DEBIT"));

        verify(transactionService).getLastTenTransactions("user-123", 1L);
    }
}
