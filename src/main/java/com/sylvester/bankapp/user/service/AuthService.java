package com.sylvester.bankapp.user.service;

import com.sylvester.bankapp.user.dto.*;
import org.springframework.stereotype.Service;


@Service
public interface AuthService {

    void registerUser(CreateUserRequest request);

    void resendCode(RequestWithEmail request);

    TokenResponse login(LoginRequest request);

    void forgotPassword(RequestWithEmail request);

    void confirmForgotPassword(ResetPassword request);

    TokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest, String userId);

    void updateUser(String userId, UpdateUserInfo request);

    void changePassword(UpdateAuthenticatedUserPassword request, String token);


    void verifyUpdatedEmail(String accessToken,VerifyUpdatedEmailRequest request);

    void sendPhoneVerificationCode(String accessToken);

    void updatePhoneNumber(String accessToken, UpdatePhoneNumberRequest update);

    void verifyPhoneNumber(String accessToken, VerifyPhoneNumberRequest verify);

    void logout(String token);

    UserDto getUser(String userId);

}
