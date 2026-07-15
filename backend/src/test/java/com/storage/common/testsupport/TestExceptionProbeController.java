package com.storage.common.testsupport;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("test")
@RequestMapping("/api/test")
public class TestExceptionProbeController {

    @GetMapping("/boom")
    public void boom() {
        throw new IllegalStateException("probe unexpected failure");
    }
}
