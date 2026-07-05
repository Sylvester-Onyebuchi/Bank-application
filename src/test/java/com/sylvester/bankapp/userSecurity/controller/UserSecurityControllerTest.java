package com.sylvester.bankapp.userSecurity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sylvester.bankapp.support.TestJwtArgumentResolver;
import com.sylvester.bankapp.userSecurity.dto.CreatePinRequest;
import com.sylvester.bankapp.userSecurity.service.SecurityService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserSecurityControllerTest {

    @Mock
    private SecurityService securityService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("user-123").build();
        mockMvc = MockMvcBuilders.standaloneSetup(new UserSecurityController(securityService))
                .setCustomArgumentResolvers(new TestJwtArgumentResolver(jwt))
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void createAndUpdatePinUseAuthenticatedUser() throws Exception {
        CreatePinRequest request = new CreatePinRequest("1234");

        mockMvc.perform(post("/api/pin/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/pin/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(securityService).createPin("1234", "user-123");
        verify(securityService).changePin("1234", "user-123");
    }
}
