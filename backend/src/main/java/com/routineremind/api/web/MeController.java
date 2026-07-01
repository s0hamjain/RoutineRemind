package com.routineremind.api.web;

import com.routineremind.api.model.LinkedStudent;
import com.routineremind.api.model.User;
import com.routineremind.api.security.AuthUser;
import com.routineremind.api.security.CurrentUser;
import com.routineremind.api.service.UserService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final UserService userService;

    public MeController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public User me(@CurrentUser AuthUser authUser) {
        return userService.require(authUser.uid());
    }

    @PostMapping("/role")
    public User setRole(@CurrentUser AuthUser authUser, @Valid @RequestBody RoleRequest request) {
        return userService.setRole(authUser.uid(), request.role());
    }

    @PostMapping("/link")
    public User link(@CurrentUser AuthUser authUser, @Valid @RequestBody LinkRequest request) {
        return userService.linkStudent(authUser.uid(), request.shareCode());
    }

    @GetMapping("/linked-students")
    public List<LinkedStudent> linkedStudents(@CurrentUser AuthUser authUser) {
        return userService.linkedStudents(authUser.uid());
    }

    @DeleteMapping("/linked-students/{studentUid}")
    public User unlinkStudent(@CurrentUser AuthUser authUser, @PathVariable String studentUid) {
        return userService.unlinkStudent(authUser.uid(), studentUid);
    }

    public record RoleRequest(@NotBlank String role) {
    }

    public record LinkRequest(@NotBlank String shareCode) {
    }
}
