package com.routineremind.api.web;

import com.routineremind.api.config.AppProperties;
import com.routineremind.api.service.ReminderService;
import com.routineremind.api.service.ReminderService.ReminderRunResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/jobs/reminders")
public class ReminderJobController {

    private final AppProperties props;
    private final ReminderService reminderService;

    public ReminderJobController(AppProperties props, ReminderService reminderService) {
        this.props = props;
        this.reminderService = reminderService;
    }

    @PostMapping("/due")
    public ReminderRunResult sendDueReminders(HttpServletRequest request) {
        if (props.getReminders().hasSchedulerToken()) {
            String provided = request.getHeader("X-Scheduler-Token");
            if (!props.getReminders().getSchedulerToken().equals(provided)) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid scheduler token");
            }
        }
        return reminderService.sendTodayReminders();
    }
}
