package com.sylvester.bankapp.user.service;


import com.sylvester.bankapp.account.entity.Account;
import com.sylvester.bankapp.account.repository.AccountRepository;
import com.sylvester.bankapp.exception.AlreadyExistException;
import com.sylvester.bankapp.exception.NotFoundException;
import com.sylvester.bankapp.user.dto.*;
import com.sylvester.bankapp.user.entity.User;
import com.sylvester.bankapp.user.entity.UserStatus;
import com.sylvester.bankapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final CognitoIdentityProviderClient cognito;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    @Value("${aws.cognito.user-pool-id}")
    private String userPoolId;

    @Value("${aws.cognito.client-id}")
    private String clientId;

    @Value("${aws.cognito.client-secret}")
    private String clientSecret;


    @Transactional
    @Override
    public void registerUser(CreateUserRequest request){

        if (userRepository.existsByEmail(request.email())){
            throw new AlreadyExistException("Email already taken. Try a different one");
        }
        if (userRepository.existsByUsername(request.username())){
            throw new AlreadyExistException("Username already taken. Try a different one");
        }
        if (!request.phone().startsWith("+")){
            throw new RuntimeException("Phone number must start with + and the country code");
        }
        String fullname = request.firstname() + " " + request.lastname();

        if (phoneNumberExists(request.phone())){
            throw new AlreadyExistException("Phone number is already in use.");
        }
        SignUpRequest signUpRequest = SignUpRequest.builder()
                .clientId(clientId)
                .username(request.email())
                .password(request.password())
                .secretHash(calculateSecretHash(clientId,clientSecret, request.email()))
                .userAttributes(
                        AttributeType.builder()
                                .name("email")
                                .value(request.email())
                                .build(),
                        AttributeType.builder()
                                .name("custom:username")
                                .value(request.username())
                                .build(),

                        AttributeType.builder()
                                .name("custom:firstname")
                                .value(request.firstname())
                                .build(),
                        AttributeType.builder()
                                .name("custom:lastname")
                                .value(request.lastname())
                                .build(),
                        AttributeType.builder()
                                .name("address")
                                .value(request.address())
                                .build(),
                        AttributeType.builder()
                                .name("phone_number")
                                .value(request.phone())
                                .build(),
                        AttributeType.builder()
                                .name("custom:city")
                                .value(request.city())
                                .build(),
                        AttributeType.builder()
                                .name("custom:country")
                                .value(request.country())
                                .build()
                )
                .build();
        SignUpResponse response = cognito.signUp(signUpRequest);
        try {
            addUserRole(request.email());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        String id = response.userSub();
        User user = User.builder()
                .id(id)
                .email(request.email())
                .username(request.username())
                .firstName(request.firstname())
                .lastName(request.lastname())
                .country(request.country())
                .phone(request.phone())
                .status(UserStatus.ACTIVE)
                .city(request.city())
                .address(request.address())
                .build();
        userRepository.save(user);

    }


    @Override
    public void resendCode(RequestWithEmail request) {
        try {
            ResendConfirmationCodeRequest resend = ResendConfirmationCodeRequest.builder()
                    .clientId(clientId)
                    .username(request.email())
                    .secretHash(calculateSecretHash(clientId,clientSecret, request.email()))
                    .build();

            cognito.resendConfirmationCode(resend);
        } catch (RuntimeException e) {
            System.out.println("Error in resend code request: " + e.getMessage());
            throw new RuntimeException("Error in resend code request");
        }
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        try {
            var user = userRepository.findByEmail(request.email()).orElseThrow(
                    () -> new NotFoundException("User not found")
            );
            if (user.getStatus().equals(UserStatus.LOCKED) || user.getStatus().equals(UserStatus.DEACTIVATED)) {
                throw new RuntimeException("You are not allowed to login. Please try again or contact administrator");
            }
            InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                    .clientId(clientId)
                    .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                    .authParameters(Map.of("USERNAME", request.email(), "PASSWORD", request.password(),
                            "SECRET_HASH",
                            calculateSecretHash(clientId, clientSecret, request.email())))
                    .build();
            InitiateAuthResponse response = cognito.initiateAuth(authRequest);
            AuthenticationResultType result = response.authenticationResult();
            return new TokenResponse(
                    result.accessToken(),
                    result.refreshToken(),
                    result.idToken(),
                    result.expiresIn(),
                    result.tokenType()
            );
        } catch (NotAuthorizedException | UserNotFoundException e) {
            throw new BadCredentialsException("Invalid username or password");
        } catch (UserNotConfirmedException e){
            throw new RuntimeException("User is not confirmed. Please verify your email before logging in.");
        }
        catch (CognitoIdentityProviderException e) {
            System.out.println("Cognito identity provider exception: " + e.getMessage());
            throw new RuntimeException("Failed to login");
        }
    }

    @Override
    public void forgotPassword(RequestWithEmail request) {

        try {
            ForgotPasswordRequest forgotPassword =
                    ForgotPasswordRequest.builder()
                            .clientId(clientId)
                            .username(request.email())
                            .secretHash(calculateSecretHash(clientId, clientSecret, request.email()))
                            .build();

            cognito.forgotPassword(forgotPassword);
        } catch (RuntimeException e) {
            System.out.println("Forgot Password Error: " + e.getMessage());
            throw new RuntimeException("Something went wrong");
        }
    }

    @Override
    public void confirmForgotPassword(ResetPassword request) {

        try {
            ConfirmForgotPasswordRequest resetPassword =
                    ConfirmForgotPasswordRequest.builder()
                            .clientId(clientId)
                            .username(request.email())
                            .confirmationCode(request.code())
                            .secretHash(calculateSecretHash(clientId, clientSecret, request.email()))
                            .password(request.newPassword())
                            .build();

            cognito.confirmForgotPassword(resetPassword);
        } catch (RuntimeException e) {
            System.out.println("Unable to confirm reset password: " + e.getMessage());
            throw new RuntimeException("Unable to confirm reset password");
        }
    }

    @Override
    public TokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest, String userId) {

        try {
            Map<String, String> authParameters = Map.of(
                    "REFRESH_TOKEN", refreshTokenRequest.token(),
                    "SECRET_HASH", calculateSecretHash(clientId, clientSecret, userId)
            );

            InitiateAuthRequest request =
                    InitiateAuthRequest.builder()
                            .clientId(clientId)
                            .authFlow(AuthFlowType.REFRESH_TOKEN_AUTH)
                            .authParameters(authParameters)
                            .build();

            InitiateAuthResponse response = cognito.initiateAuth(request);

            AuthenticationResultType result = response.authenticationResult();

            return new TokenResponse(
                    result.accessToken(),
                    result.refreshToken(),
                    result.idToken(),
                    result.expiresIn(),
                    result.tokenType()
            );
        } catch (RuntimeException e) {
            System.out.println("Failed to refresh token: " + e.getMessage());
            throw new RuntimeException("Something went wrong");
        }

    }


    @Transactional
    @Override
    public void updateUser(String userId, UpdateUserInfo request) {

        try {
            User user = userRepository.findById(userId).orElseThrow(
                    () -> new NotFoundException("User with the Email not found")
            );

            if (userRepository.existsByEmail(request.email())) {
                throw new AlreadyExistException("Email already exists.Try a different one");
            }

            if (userRepository.existsByUsername(request.username())) {
                throw new AlreadyExistException("Username already exists.Try a different one");
            }

            List<AttributeType> attributes = new ArrayList<>();

            if (request.firstname() != null) {
                attributes.add(
                        AttributeType.builder()
                                .name("custom:firstname")
                                .value(request.firstname())
                                .build()
                );
                user.setFirstName(request.firstname());
            }

            if (request.lastname() != null) {
                attributes.add(
                        AttributeType.builder()
                                .name("custom:lastname")
                                .value(request.lastname())
                                .build()
                );
                user.setLastName(request.lastname());
            }

            if (request.username() != null) {
                attributes.add(
                        AttributeType.builder()
                                .name("custom:username")
                                .value(request.username())
                                .build()
                );
                user.setUsername(request.username());
            }

            if (request.email() != null) {
                attributes.add(
                        AttributeType.builder()
                                .name("email")
                                .value(request.email())
                                .build()
                );
                user.setEmail(request.email());
            }

            if (request.address() != null) {
                attributes.add(
                        AttributeType.builder()
                                .name("address")
                                .value(request.address())
                                .build()
                );
                user.setAddress(request.address());
            }

            if (request.city() != null) {
                attributes.add(
                        AttributeType.builder()
                                .name("custom:city")
                                .value(request.city())
                                .build()
                );
                user.setCity(request.city());
            }

            if (request.country() != null) {
                attributes.add(
                        AttributeType.builder()
                                .name("custom:country")
                                .value(request.country())
                                .build()
                );
                user.setCountry(request.country());
            }

            if (request.phone() != null) {
                attributes.add(
                        AttributeType.builder()
                                .name("phone_number")
                                .value(request.phone())
                                .build()
                );
                user.setPhone(request.phone());
            }

            if (attributes.isEmpty()) {
                throw new IllegalArgumentException("No fields provided for update");
            }

            UpdateUserAttributesRequest updateRequest =
                    UpdateUserAttributesRequest.builder()
                            .userAttributes(attributes)
                            .build();

            cognito.updateUserAttributes(updateRequest);
            user.setModifiedDate(Instant.now());
            userRepository.save(user);
        } catch (NotFoundException | IllegalArgumentException | AwsServiceException | SdkClientException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void changePassword(UpdateAuthenticatedUserPassword request, String token){
        try {
            ChangePasswordRequest changePassword =
                    ChangePasswordRequest.builder()
                            .accessToken(token)
                            .previousPassword(request.oldPassword())
                            .proposedPassword(request.newPassword())
                            .build();

            cognito.changePassword(changePassword);
        } catch (AwsServiceException e) {
            System.out.println("Change Password Failed: " + e.getMessage());
            throw new RuntimeException("Change Password Failed");
        }
    }

    @Override
    public void verifyUpdatedEmail(String accessToken, VerifyUpdatedEmailRequest verify) {

        try {
            VerifyUserAttributeRequest request = VerifyUserAttributeRequest.builder()
                    .accessToken(accessToken)
                    .attributeName("email")
                    .code(verify.code())
                    .build();

            cognito.verifyUserAttribute(request);

            RevokeTokenRequest tokenRequest = RevokeTokenRequest.builder()
                    .token(verify.refreshToken())
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .build();
            cognito.revokeToken(tokenRequest);
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException("Failed to verify updated email");
        }
    }

    private void addUserRole(String email)throws Exception{
        try {
            AdminAddUserToGroupRequest request = AdminAddUserToGroupRequest.builder()
                    .userPoolId(userPoolId)
                    .username(email)
                    .groupName("USER")
                    .build();
            cognito.adminAddUserToGroup(request);
        } catch (AwsServiceException e) {
            System.err.println("Failed to assign role: " + e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Failed to assign role");
        }
    }


    @Override
    public void sendPhoneVerificationCode(String accessToken) {

        try {
            GetUserAttributeVerificationCodeRequest request =
                    GetUserAttributeVerificationCodeRequest.builder()
                            .accessToken(accessToken)
                            .attributeName("phone_number")
                            .build();

            cognito.getUserAttributeVerificationCode(request);
        } catch ( RuntimeException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException("Failed to send phone verification code");
        }
    }


    @Transactional
    @Override
    public void updatePhoneNumber(String accessToken, UpdatePhoneNumberRequest update) {

        if (!update.phone_number().startsWith("+")){
            throw new RuntimeException("Phone number must start with + and the country code");
        }

        if (phoneNumberExists(update.phone_number())){
            throw new AlreadyExistException("Phone number already exists. Try a different phone number.");
        }

        try {
            UpdateUserAttributesRequest request = UpdateUserAttributesRequest.builder()
                    .accessToken(accessToken)
                    .userAttributes(
                            AttributeType.builder()
                                    .name("phone_number")
                                    .value(update.phone_number())
                                    .build()
                    )
                    .build();

            cognito.updateUserAttributes(request);
            User user = User.builder().phone(update.phone_number()).build();
            userRepository.save(user);
        } catch (AwsServiceException e) {
            System.out.println("Failed to update phone number: " + e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Failed to update phone number");
        }
    }

    @Override
    public void verifyPhoneNumber(String accessToken, VerifyPhoneNumberRequest verify) {

        try {
            VerifyUserAttributeRequest request =
                    VerifyUserAttributeRequest.builder()
                            .accessToken(accessToken)
                            .attributeName("phone_number")
                            .code(verify.code())
                            .build();

            cognito.verifyUserAttribute(request);
        } catch (AwsServiceException e) {
            System.out.println("Failed to verify phone number: " + e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Failed to verify phone number");
        }
    }



    @Override
    public void logout(String refreshToken){
        try {
            RevokeTokenRequest request = RevokeTokenRequest.builder()
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .token(refreshToken)
                    .build();
            cognito.revokeToken(request);
        } catch (CognitoIdentityProviderException e) {
            System.err.println("Failed to revoke token: " + e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Something went wrong while trying to logout");

        }
    }

    @Override
    public UserDto getUser(String userId){
        var user = userRepository.findById(userId).orElseThrow(
                () -> new NotFoundException("User not found")
        );
        return new UserDto(user.getUsername(), user.getEmail(), user.getFirstName(), user.getLastName(),
               user.getPhone(), user.getCity(), user.getCountry());
    }




    private String calculateSecretHash(String clientId, String clientSecret, String username) {

        try {
            String message = username + clientId;

            Mac mac = Mac.getInstance("HmacSHA256");

            SecretKeySpec keySpec = new SecretKeySpec(
                    clientSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );

            mac.init(keySpec);

            byte[] hmac = mac.doFinal(
                    message.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getEncoder().encodeToString(hmac);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean phoneNumberExists(String phoneNumber) {
        ListUsersRequest request = ListUsersRequest.builder()
                .userPoolId(userPoolId)
                .filter("phone_number = \"" + phoneNumber + "\"")
                .limit(1)
                .build();

        ListUsersResponse response = cognito.listUsers(request);

        return !response.users().isEmpty();
    }
}


