package com.hr.controller;

import com.hr.service.CronService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/cron")
@RequiredArgsConstructor
public class CronController {

    private final CronService cronService;

    @PostMapping("/daily")
    public Map<String, Object> runDaily(@RequestHeader(value = "X-Cron-Secret", required = false) String cronSecret) {
        return cronService.runDailyJobs(cronSecret);
    }
}
