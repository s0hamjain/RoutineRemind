package com.routineremind.api.web;

import com.routineremind.api.model.User;
import com.routineremind.api.security.AuthUser;
import com.routineremind.api.security.CurrentUser;
import com.routineremind.api.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Verifies the token (via the auth filter) and creates the profile on first login.
     */
    @PostMapping("/session")
    public User session(@CurrentUser AuthUser authUser) {
        return userService.upsertOnLogin(authUser);
    }
}
