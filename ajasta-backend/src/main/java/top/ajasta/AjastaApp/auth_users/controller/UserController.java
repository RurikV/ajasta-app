package top.ajasta.AjastaApp.auth_users.controller;


import top.ajasta.AjastaApp.auth_users.dtos.UserDTO;
import top.ajasta.AjastaApp.auth_users.services.UserService;
import top.ajasta.AjastaApp.response.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;


    @GetMapping("/all")
    @PreAuthorize("hasAuthority('ADMIN')") // ADMIN ALONE HAVE ACCESS TO THIS endpoint
    public ResponseEntity<Response<List<UserDTO>>> getAllUsers(){
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Response<?>> updateOwnAccount(
            @ModelAttribute UserDTO userDTO,
            @RequestPart(value = "imageFile", required = false)MultipartFile imageFile
            ){
        userDTO.setImageFile(imageFile);
        return ResponseEntity.ok(userService.updateOwnAccount(userDTO));
    }


    @DeleteMapping("/deactivate")
    public ResponseEntity<Response<?>> deactivateOwnAccount() {
        return ResponseEntity.ok(userService.deactivateOwnAccount());
    }

    @GetMapping("/account")
    public ResponseEntity<Response<UserDTO>> getOwnAccountDetails() {
        return ResponseEntity.ok(userService.getOwnAccountDetails());
    }

    @GetMapping("/saved-emails")
    public ResponseEntity<Response<List<String>>> getSavedEmails() {
        return ResponseEntity.ok(userService.getSavedEmails());
    }

    @PostMapping("/saved-emails")
    public ResponseEntity<Response<?>> addSavedEmail(@RequestParam("email") String email) {
        return ResponseEntity.ok(userService.addSavedEmail(email));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response<UserDTO>> updateUserRoles(@PathVariable("id") Long userId,
                                                             @RequestBody List<String> roles) {
        return ResponseEntity.ok(userService.updateUserRoles(userId, roles));
    }

}
