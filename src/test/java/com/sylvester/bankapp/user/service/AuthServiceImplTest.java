package com.sylvester.bankapp.user.service;

import com.sylvester.bankapp.account.repository.AccountRepository;
import com.sylvester.bankapp.exception.AlreadyExistException;
import com.sylvester.bankapp.user.dto.CreateUserRequest;
import com.sylvester.bankapp.user.dto.LoginRequest;
import com.sylvester.bankapp.user.dto.RefreshTokenRequest;
import com.sylvester.bankapp.user.dto.RequestWithEmail;
import com.sylvester.bankapp.user.dto.ResetPassword;
import com.sylvester.bankapp.user.dto.TokenResponse;
import com.sylvester.bankapp.user.dto.UpdateAuthenticatedUserPassword;
import com.sylvester.bankapp.user.dto.UpdatePhoneNumberRequest;
import com.sylvester.bankapp.user.dto.UpdateUserInfo;
import com.sylvester.bankapp.user.dto.UserDto;
import com.sylvester.bankapp.user.dto.VerifyPhoneNumberRequest;
import com.sylvester.bankapp.user.dto.VerifyUpdatedEmailRequest;
import com.sylvester.bankapp.user.entity.User;
import com.sylvester.bankapp.user.entity.UserStatus;
import com.sylvester.bankapp.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ChangePasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmForgotPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ForgotPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserAttributeVerificationCodeRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ResendConfirmationCodeRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.RevokeTokenRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.VerifyUserAttributeRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String CLIENT_ID = "client-id";
    private static final String CLIENT_SECRET = "client-secret";
    private static final String USER_POOL_ID = "pool-id";

    @Mock
    private CognitoIdentityProviderClient cognito;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "clientId", CLIENT_ID);
        ReflectionTestUtils.setField(authService, "clientSecret", CLIENT_SECRET);
        ReflectionTestUtils.setField(authService, "userPoolId", USER_POOL_ID);
    }

    @Test
    void registerUserCreatesCognitoUserAndPersistsLocalUser() {
        CreateUserRequest request = createUserRequest();
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.existsByUsername(request.username())).thenReturn(false);
        when(cognito.listUsers(any(ListUsersRequest.class))).thenReturn(ListUsersResponse.builder().build());
        when(cognito.signUp(any(SignUpRequest.class))).thenReturn(SignUpResponse.builder().userSub("user-123").build());

        authService.registerUser(request);

        ArgumentCaptor<SignUpRequest> signUpCaptor = ArgumentCaptor.forClass(SignUpRequest.class);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<AdminAddUserToGroupRequest> groupCaptor = ArgumentCaptor.forClass(AdminAddUserToGroupRequest.class);
        verify(cognito).signUp(signUpCaptor.capture());
        verify(cognito).adminAddUserToGroup(groupCaptor.capture());
        verify(userRepository).save(userCaptor.capture());

        assertThat(signUpCaptor.getValue().clientId()).isEqualTo(CLIENT_ID);
        assertThat(signUpCaptor.getValue().username()).isEqualTo(request.email());
        assertThat(groupCaptor.getValue().groupName()).isEqualTo("USER");
        assertThat(userCaptor.getValue().getId()).isEqualTo("user-123");
        assertThat(userCaptor.getValue().getUsername()).isEqualTo(request.username());
    }

    @Test
    void registerUserRejectsDuplicateEmailAndInvalidPhone() {
        CreateUserRequest request = createUserRequest();
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.registerUser(request))
                .isInstanceOf(AlreadyExistException.class)
                .hasMessage("Email already taken. Try a different one");
        verifyNoInteractions(cognito);

        CreateUserRequest badPhone = new CreateUserRequest("Sylvester", "Onah", "middle", "new@example.com", "Password1!", "123 Main Street", "Zagreb", "Croatia", "385911234567");
        when(userRepository.existsByEmail(badPhone.email())).thenReturn(false);
        when(userRepository.existsByUsername(badPhone.username())).thenReturn(false);

        assertThatThrownBy(() -> authService.registerUser(badPhone))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Phone number must start with + and the country code");
        verify(cognito, never()).signUp(any(SignUpRequest.class));
    }

    @Test
    void resendCodeCallsCognito() {
        authService.resendCode(new RequestWithEmail("user@example.com"));

        ArgumentCaptor<ResendConfirmationCodeRequest> captor = ArgumentCaptor.forClass(ResendConfirmationCodeRequest.class);
        verify(cognito).resendConfirmationCode(captor.capture());
        assertThat(captor.getValue().username()).isEqualTo("user@example.com");
    }

    @Test
    void loginReturnsTokenResponseFromCognitoAuthenticationResult() {
        LoginRequest request = new LoginRequest("user@example.com", "Password1!");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user("user-123", "user@example.com", UserStatus.ACTIVE)));
        when(cognito.initiateAuth(any(InitiateAuthRequest.class)))
                .thenReturn(InitiateAuthResponse.builder()
                        .authenticationResult(AuthenticationResultType.builder()
                                .accessToken("access-token")
                                .refreshToken("refresh-token")
                                .idToken("id-token")
                                .expiresIn(3600)
                                .tokenType("Bearer")
                                .build())
                        .build());

        TokenResponse response = authService.login(request);

        ArgumentCaptor<InitiateAuthRequest> captor = ArgumentCaptor.forClass(InitiateAuthRequest.class);
        verify(cognito).initiateAuth(captor.capture());
        assertThat(captor.getValue().authFlow()).isEqualTo(AuthFlowType.USER_PASSWORD_AUTH);
        assertThat(captor.getValue().authParameters()).containsEntry("USERNAME", request.email());
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void forgotAndConfirmForgotPasswordCallCognito() {
        RequestWithEmail forgotRequest = new RequestWithEmail("user@example.com");
        ResetPassword resetPassword = new ResetPassword("user@example.com", "123456", "newPassword123");

        authService.forgotPassword(forgotRequest);
        authService.confirmForgotPassword(resetPassword);

        ArgumentCaptor<ForgotPasswordRequest> forgotCaptor = ArgumentCaptor.forClass(ForgotPasswordRequest.class);
        ArgumentCaptor<ConfirmForgotPasswordRequest> confirmCaptor = ArgumentCaptor.forClass(ConfirmForgotPasswordRequest.class);
        verify(cognito).forgotPassword(forgotCaptor.capture());
        verify(cognito).confirmForgotPassword(confirmCaptor.capture());
        assertThat(forgotCaptor.getValue().username()).isEqualTo("user@example.com");
        assertThat(confirmCaptor.getValue().confirmationCode()).isEqualTo("123456");
    }

    @Test
    void refreshTokenReturnsTokenResponseFromCognito() {
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");
        when(cognito.initiateAuth(any(InitiateAuthRequest.class)))
                .thenReturn(InitiateAuthResponse.builder()
                        .authenticationResult(AuthenticationResultType.builder()
                                .accessToken("access-token")
                                .refreshToken("new-refresh-token")
                                .idToken("id-token")
                                .expiresIn(1800)
                                .tokenType("Bearer")
                                .build())
                        .build());

        TokenResponse response = authService.refreshToken(request, "user-123");

        ArgumentCaptor<InitiateAuthRequest> captor = ArgumentCaptor.forClass(InitiateAuthRequest.class);
        verify(cognito).initiateAuth(captor.capture());
        assertThat(captor.getValue().authFlow()).isEqualTo(AuthFlowType.REFRESH_TOKEN_AUTH);
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    void updateUserOnlySendsChangedAttributesAndSavesLocalUser() {
        User user = user("user-123", "old@example.com", UserStatus.ACTIVE);
        UpdateUserInfo request = new UpdateUserInfo("New", null, null, "new@example.com", "New address", null, null, null);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));

        authService.updateUser("user-123", request);

        ArgumentCaptor<UpdateUserAttributesRequest> cognitoCaptor = ArgumentCaptor.forClass(UpdateUserAttributesRequest.class);
        verify(cognito).updateUserAttributes(cognitoCaptor.capture());
        verify(userRepository).save(user);
        assertThat(cognitoCaptor.getValue().userAttributes())
                .extracting(attribute -> attribute.name() + "=" + attribute.value())
                .containsExactlyInAnyOrder("custom:firstname=New", "email=new@example.com", "address=New address");
        assertThat(user.getFirstName()).isEqualTo("New");
        assertThat(user.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    void updateUserThrowsWhenNoFieldsProvided() {
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user("user-123", "old@example.com", UserStatus.ACTIVE)));

        assertThatThrownBy(() -> authService.updateUser("user-123", new UpdateUserInfo(null, null, null, null, null, null, null, null)))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);

        verify(cognito, never()).updateUserAttributes(any(UpdateUserAttributesRequest.class));
    }

    @Test
    void passwordEmailPhoneAndLogoutMethodsCallCognito() {
        when(cognito.listUsers(any(ListUsersRequest.class))).thenReturn(ListUsersResponse.builder().build());

        authService.changePassword(new UpdateAuthenticatedUserPassword("oldPassword", "newPassword"), "access-token");
        authService.verifyUpdatedEmail("access-token", new VerifyUpdatedEmailRequest("123456", "refresh-token"));
        authService.sendPhoneVerificationCode("access-token");
        authService.updatePhoneNumber("access-token", new UpdatePhoneNumberRequest("+385911234567"));
        authService.verifyPhoneNumber("access-token", new VerifyPhoneNumberRequest("123456"));
        authService.logout("refresh-token");

        verify(cognito).changePassword(any(ChangePasswordRequest.class));
        verify(cognito, times(2)).verifyUserAttribute(any(VerifyUserAttributeRequest.class));
        verify(cognito).getUserAttributeVerificationCode(any(GetUserAttributeVerificationCodeRequest.class));
        verify(cognito).updateUserAttributes(any(UpdateUserAttributesRequest.class));
        verify(cognito, times(2)).revokeToken(any(RevokeTokenRequest.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updatePhoneNumberRejectsMissingPlusPrefix() {
        assertThatThrownBy(() -> authService.updatePhoneNumber("access-token", new UpdatePhoneNumberRequest("385911234567")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Phone number must start with + and the country code");
    }

    @Test
    void getUserReturnsUserDto() {
        User user = user("user-123", "user@example.com", UserStatus.ACTIVE);
        user.setUsername("username");
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));

        UserDto dto = authService.getUser("user-123");

        assertThat(dto.username()).isEqualTo("username");
        assertThat(dto.email()).isEqualTo("user@example.com");
    }

    private User user(String id, String email, UserStatus status) {
        return User.builder()
                .id(id)
                .firstName("First")
                .lastName("Last")
                .username("username")
                .email(email)
                .status(status)
                .build();
    }

    private CreateUserRequest createUserRequest() {
        return new CreateUserRequest(
                "Sylvester",
                "Onah",
                "middle",
                "user@example.com",
                "Password1!",
                "123 Main Street",
                "Zagreb",
                "Croatia",
                "+385911234567"
        );
    }
}
