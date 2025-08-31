package top.ajasta.AjastaApp.auth_users.services;

import top.ajasta.AjastaApp.auth_users.dtos.LoginRequest;
import top.ajasta.AjastaApp.auth_users.dtos.LoginResponse;
import top.ajasta.AjastaApp.auth_users.dtos.RegistrationRequest;
import top.ajasta.AjastaApp.response.Response;

public interface AuthService {
    Response<?> register(RegistrationRequest registrationRequest);
    Response<LoginResponse> login(LoginRequest loginRequest);
}
