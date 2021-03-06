package org.nzvirtual.api.controllers;

import org.nzvirtual.api.ApiApplication;
import org.nzvirtual.api.dto.ServerInfoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/server")
public class ServerController {
    @Value("${app.version}")
    private String appversion;

    @GetMapping()
    public ResponseEntity<ServerInfoResponse> getServerInfo() {
        return new ResponseEntity<>(new ServerInfoResponse(String.format("%s-%s", appversion, ApiApplication.getVersion())), HttpStatus.OK);
    }

    @GetMapping("/ping")
    public String getPing() {
        return "PONG";
    }
}
