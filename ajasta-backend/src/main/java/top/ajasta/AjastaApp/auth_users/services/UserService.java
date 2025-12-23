package top.ajasta.AjastaApp.auth_users.services;

import top.ajasta.AjastaApp.auth_users.dtos.UserDTO;
import top.ajasta.AjastaApp.auth_users.entity.User;
import top.ajasta.AjastaApp.response.Response;

import java.util.List;

public interface UserService {


    User getCurrentLoggedInUser();

    Response<List<UserDTO>> getAllUsers();

    Response<UserDTO> getOwnAccountDetails();

    Response<?> updateOwnAccount(UserDTO userDTO);

    Response<?> deactivateOwnAccount();

    Response<List<String>> getSavedEmails();

    Response<?> addSavedEmail(String email);

    Response<UserDTO> updateUserRoles(Long userId, List<String> roleNames);
}
