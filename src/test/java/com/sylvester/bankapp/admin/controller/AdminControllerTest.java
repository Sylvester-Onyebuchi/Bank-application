package com.sylvester.bankapp.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sylvester.bankapp.admin.dto.ExtendRoleRequest;
import com.sylvester.bankapp.admin.dto.LockAndUnlockAccount;
import com.sylvester.bankapp.admin.dto.RevertTransaction;
import com.sylvester.bankapp.admin.service.AdminService;
import com.sylvester.bankapp.audit.entity.AuditLog;
import com.sylvester.bankapp.support.TestJwtArgumentResolver;
import com.sylvester.bankapp.transaction.entity.Transaction;
import com.sylvester.bankapp.user.dto.RequestWithEmail;
import com.sylvester.bankapp.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminService adminService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("admin-id")
                .claim("cognito:groups", List.of("ADMIN"))
                .build();
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminController(adminService))
                .setCustomArgumentResolvers(new TestJwtArgumentResolver(jwt))
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void lockAndUnlockUserEndpointsCallService() throws Exception {
        RequestWithEmail request = new RequestWithEmail("user@example.com");

        mockMvc.perform(post("/api/admins/lock-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("User locked successfully"));

        mockMvc.perform(post("/api/admins/unlock-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("User unlocked successfully"));

        verify(adminService).lockUser("user@example.com", "admin-id");
        verify(adminService).unLockUser("user@example.com", "admin-id");
    }

    @Test
    void accountMutationEndpointsCallService() throws Exception {
        LockAndUnlockAccount request = new LockAndUnlockAccount("HR123");

        mockMvc.perform(post("/api/admins/lock-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("User account locked successfully"));

        mockMvc.perform(post("/api/admins/unlock-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("User account unlocked successfully"));

        mockMvc.perform(delete("/api/admins/delete-account").param("accountNumber", "HR123"))
                .andExpect(status().isOk())
                .andExpect(content().string("Account deleted successfully"));

        verify(adminService).lockUserAccount("HR123", "admin-id");
        verify(adminService).unLockUserAccount("HR123", "admin-id");
        verify(adminService).disableAccount("HR123");
    }

    @Test
    void transactionAndRoleMutationEndpointsCallService() throws Exception {
        RevertTransaction revertRequest = new RevertTransaction("tx-123");
        ExtendRoleRequest roleRequest = new ExtendRoleRequest("user@example.com", "ADMIN");

        mockMvc.perform(post("/api/admins/reverse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revertRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("Transaction revert successfully"));

        mockMvc.perform(post("/api/admins/update-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("Role updated successfully"));

        mockMvc.perform(delete("/api/admins/remove-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("Role removed successfully"));

        verify(adminService).reverseTransaction("tx-123", "admin-id");
        verify(adminService).extendUserRole(roleRequest, "admin-id");
        verify(adminService).removeRole(roleRequest, "admin-id");
    }

    @Test
    void disableUserEndpointCallsService() throws Exception {
        mockMvc.perform(delete("/api/admins/delete-user/user@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("Account deleted successfully"));

        verify(adminService).disableUser("user@example.com", "admin-id");
    }

    @Test
    void queryEndpointsReturnServiceResults() throws Exception {
        when(adminService.findTransactions()).thenReturn(new PageImpl<>(List.of(Transaction.builder().transactionId("tx-123").build()), PageRequest.of(0, 20), 1));
        when(adminService.findUsers()).thenReturn(new PageImpl<>(List.of(User.builder().id("user-123").email("user@example.com").build()), PageRequest.of(0, 20), 1));
        when(adminService.findAuditLogs()).thenReturn(new PageImpl<>(List.of(AuditLog.builder().id(1L).action("Action").build()), PageRequest.of(0, 10), 1));
        when(adminService.getAdmins()).thenReturn(List.of());

        mockMvc.perform(get("/api/admins/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].transactionId").value("tx-123"));
        mockMvc.perform(get("/api/admins/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("user-123"));
        mockMvc.perform(get("/api/admins/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("Action"));
        mockMvc.perform(get("/api/admins/all"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void meReturnsCognitoGroupsFromJwt() throws Exception {
        mockMvc.perform(get("/api/admins/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("ADMIN"));
    }
}
