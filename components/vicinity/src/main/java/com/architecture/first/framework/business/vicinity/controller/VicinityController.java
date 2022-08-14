package com.architecture.first.framework.business.vicinity.controller;

import com.architecture.first.framework.business.vicinity.Vicinity;
import com.architecture.first.framework.business.vicinity.VicinityServer;
import com.architecture.first.framework.business.vicinity.info.VicinityInfo;
import com.architecture.first.framework.business.vicinity.messages.VicinityMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("api/vicinity")
public class VicinityController {

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private Vicinity vicinity;

    @Autowired
    private VicinityServer vicinityServer;

    @Autowired
    private VicinityInfo vicinityInfo;

    @PostMapping("message/send")
    public Integer send(@RequestBody VicinityMessage message) {
        try {
            vicinityServer.publishVicinityMessage(message);
        }
        catch (Exception e) {
            log.error("Vicinity Server Error ", e);
            return 505;
        }

        return 200;
    }

    @GetMapping("info")
    public VicinityInfo getInfo() {
        return vicinityInfo;
    }

}
