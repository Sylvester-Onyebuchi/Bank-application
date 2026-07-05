package com.sylvester.bankapp.beneficiary.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sylvester.bankapp.beneficiary.dto.BeneficiaryRequest;
import com.sylvester.bankapp.beneficiary.service.BeneficiaryService;
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

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BeneficiaryControllerTest {

    @Mock
    private BeneficiaryService beneficiaryService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("user-123").build();
        mockMvc = MockMvcBuilders.standaloneSetup(new BeneficiaryController(beneficiaryService))
                .setCustomArgumentResolvers(new TestJwtArgumentResolver(jwt))
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void addAndDeleteBeneficiaryUseAuthenticatedUser() throws Exception {
        BeneficiaryRequest request = new BeneficiaryRequest("Rent", "HR123");

        mockMvc.perform(post("/api/beneficiaries/add-beneficiary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/beneficiaries/delete/5"))
                .andExpect(status().isNoContent());

        verify(beneficiaryService).addBeneficiary(request, "user-123");
        verify(beneficiaryService).removeFromBeneficiary(5L, "user-123");
    }
}
