package com.routineremind.api.web;

import com.routineremind.api.model.DeviceRegistration;
import com.routineremind.api.security.AuthUser;
import com.routineremind.api.security.CurrentUser;
import com.routineremind.api.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping
    public Map<String, String> register(@CurrentUser AuthUser authUser,
                                        @Valid @RequestBody DeviceRegistration request) {
        return deviceService.register(authUser.uid(), request);
    }
}
