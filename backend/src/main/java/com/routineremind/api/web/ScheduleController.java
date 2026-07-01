package com.routineremind.api.web;

import com.routineremind.api.model.Schedule;
import com.routineremind.api.model.ScheduleItem;
import com.routineremind.api.security.AuthUser;
import com.routineremind.api.security.CurrentUser;
import com.routineremind.api.service.ScheduleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping("/schedule/today")
    public Schedule today(@CurrentUser AuthUser authUser,
                          @RequestParam(required = false) String ownerUid) {
        return scheduleService.getToday(authUser.uid(), ownerUid);
    }

    @GetMapping("/schedules")
    public List<Schedule> list(@CurrentUser AuthUser authUser,
                               @RequestParam(required = false) String ownerUid,
                               @RequestParam(required = false) String before,
                               @RequestParam(required = false) Integer limit) {
        return scheduleService.listSchedules(authUser.uid(), ownerUid, before, limit);
    }

    @GetMapping("/schedules/{id}")
    public Schedule get(@CurrentUser AuthUser authUser, @PathVariable String id) {
        return scheduleService.getSchedule(authUser.uid(), id);
    }

    @PostMapping("/schedules")
    public Schedule create(@CurrentUser AuthUser authUser, @Valid @RequestBody ScheduleRequest request) {
        return scheduleService.createSchedule(
                authUser.uid(),
                request.ownerUid(),
                request.title(),
                request.date(),
                request.items()
        );
    }

    @PatchMapping("/schedules/{id}")
    public Schedule update(@CurrentUser AuthUser authUser,
                           @PathVariable String id,
                           @Valid @RequestBody ScheduleRequest request) {
        return scheduleService.updateSchedule(
                authUser.uid(),
                id,
                request.title(),
                request.date(),
                request.status(),
                request.items()
        );
    }

    @PostMapping("/schedules/{id}/items/{itemId}/complete")
    public Schedule completeItem(@CurrentUser AuthUser authUser,
                                 @PathVariable String id,
                                 @PathVariable String itemId) {
        return scheduleService.completeItem(authUser.uid(), id, itemId);
    }

    public record ScheduleRequest(
            String ownerUid,
            @NotBlank String title,
            @NotBlank String date,
            String status,
            List<ScheduleItem> items
    ) {
    }
}
