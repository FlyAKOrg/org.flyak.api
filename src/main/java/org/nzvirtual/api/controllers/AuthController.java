package org.nzvirtual.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.nzvirtual.api.data.entity.User;
import org.nzvirtual.api.data.repository.UserRepository;
import org.nzvirtual.api.dto.*;
import org.nzvirtual.api.exception.ErrorResponse;
import org.nzvirtual.api.exception.GeneralException;
import org.nzvirtual.api.exception.ValidationException;
import org.nzvirtual.api.security.JwtUtils;
import org.nzvirtual.api.security.UserDetailsImpl;
import org.nzvirtual.api.service.AuthService;
import org.nzvirtual.api.service.EmailService;
import org.nzvirtual.api.utils.TokenGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.security.SecureRandom;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    @Value("${app.registration.enabled}")
    private Boolean registrationEnabled;
    private Logger log = LoggerFactory.getLogger(AuthController.class);
    @Value("${app.ui.baseurl}")
    private String UIBaseURL;
    @Value("${app.ui.registration_verification}")
    private String UIRegVerificationURL;
    private TokenGenerator tokenGenerator;
    private EmailService emailService;
    private UserRepository userRepository;

    public AuthController(AuthService authService,
                          UserRepository userRepository,
                          AuthenticationManager authenticationManager,
                          JwtUtils jwtUtils,
                          EmailService emailService) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
        this.tokenGenerator = new TokenGenerator(12, new SecureRandom());
        this.emailService = emailService;
    }

    @Operation(description = "Delete all user's refresh token.", security = { @SecurityRequirement(name = "bearerAuth") }, responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content()),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content())
    })
    @DeleteMapping("/refresh")
    public ResponseEntity<GeneralStatusResponse> deleteAllRefresh(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User authUser = userDetails.getUser();

        authService.blacklistRefreshTokenForUser(authUser);

        return new ResponseEntity<GeneralStatusResponse>(new GeneralStatusResponse("OK"), HttpStatus.OK);
    }

    @Operation(description = "Delete a refresh token.", security = { @SecurityRequirement(name = "bearerAuth") }, responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content()),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content())
    })
    @DeleteMapping("/refresh/{refreshToken}")
    public ResponseEntity<GeneralStatusResponse> deleteRefresh(@PathVariable String refreshToken, Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User authUser = userDetails.getUser();

        User user;

        try {
            user = authService.lookupToken(refreshToken);
        } catch (Exception e) {
            throw new GeneralException("Not Found", HttpStatus.NOT_FOUND);
        }

        if (user.getId() != authUser.getId()) {
            authService.blacklistRefreshTokenForUser(authUser);
            authService.blacklistRefreshTokenForUser(user);
            throw new GeneralException("Forbidden", HttpStatus.FORBIDDEN);
        }

        authService.deleteToken(refreshToken);

        return new ResponseEntity<GeneralStatusResponse>(new GeneralStatusResponse("OK"), HttpStatus.OK);
    }

    @Operation(description = "Request a new token.", security = { @SecurityRequirement(name = "bearerAuth") }, responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content()),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content())
    })
    @GetMapping("/refresh/{refreshToken}")
    public ResponseEntity<TokenResponse> refresh(@PathVariable String refreshToken, Authentication authentication) {
        User authUser = null;

        if(authentication != null) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            authUser = userDetails.getUser();
        }

        User user;

        try {
            user = authService.lookupToken(refreshToken);
        } catch (Exception e) {
            // Token doesn't exist.
            throw new GeneralException("Forbidden", HttpStatus.FORBIDDEN);
        }

        if (authUser != null && user.getId() != authUser.getId()) {
            log.warn(String.format("Got refresh token, but authenicated user %s does not match associated user %s. Unwhitelisting user's refresh tokens.", user.getEmail(), authUser.getEmail()));
            authService.blacklistRefreshTokenForUser(user);
            throw new GeneralException("Forbidden", HttpStatus.FORBIDDEN);
        }

        String token = jwtUtils.generateJwtToken(user);

        return new ResponseEntity<>(new TokenResponse(token, "Bearer", null), HttpStatus.OK);
    }

    @Operation(description = "Login.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content())
    })
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest loginRequest, Errors errors) {
        if (errors.hasErrors()) {
            throw new ValidationException(errors);
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );
        } catch(AuthenticationException e) {
            log.warn("Authentication failed for: " + loginRequest.getEmail());
            throw new GeneralException("Authentication Failed", "", HttpStatus.FORBIDDEN);
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl)authentication.getPrincipal();
        log.debug(String.format("Login successful for %s", userDetails.getUsername()));
        return new ResponseEntity<>(new TokenResponse(jwt, "Bearer", authService.createRefreshToken(userDetails.getUser())), HttpStatus.OK);
    }

    @Operation(description = "Register new user.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "309", description = "Conflict, generally email is already registered.", content = @Content()),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest registerRequest, Errors errors) {
        if (!registrationEnabled) {
            log.warn(String.format("Registration attempted with email: %s, while registrations are disabled.", registerRequest.getEmail()));
            throw new GeneralException("Not found", "", HttpStatus.NOT_FOUND);
        }
        if (errors.hasErrors()) {
            throw new ValidationException(errors);
        }
        authService.register(registerRequest);
        log.info(String.format("User Registered (email='%s')", registerRequest.getEmail()));
        return new ResponseEntity<>("OK", HttpStatus.CREATED);
    }

    @PutMapping("/forgot")
    @Operation(description = "Request forgot password token.", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = GeneralStatusResponse.class)))
    })
    public ResponseEntity<GeneralStatusResponse> forgotPassword(@RequestBody ForgotPasswordRequest forgotPasswordRequest) {
        Optional<User> optionalUser = userRepository.findByEmail(forgotPasswordRequest.getEmail());
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            this.authService.forgotPassword(user);
        }

        return new ResponseEntity<>(new GeneralStatusResponse("OK"), HttpStatus.OK);
    }

    @PutMapping("/forgot/{token}")
    @Operation(description = "Utilize forgot password token.", responses = {
            @ApiResponse(responseCode = "202", description = "Accepted", content = @Content(schema = @Schema(implementation = GeneralStatusResponse.class))),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content())
    })
    public ResponseEntity<GeneralStatusResponse> verifyForgotPassword(@PathVariable String token) {
        Boolean result = authService.checkAndReset(token);

        if (result) return new ResponseEntity<>(new GeneralStatusResponse("OK"), HttpStatus.ACCEPTED);

        return new ResponseEntity<>(new GeneralStatusResponse("Not Found"), HttpStatus.NOT_FOUND);
    }

    @PutMapping("/verify/{token}")
    @Operation(description = "Verify registration token.", responses = {
            @ApiResponse(responseCode = "202", description = "Accepted", content = @Content(schema = @Schema(implementation = GeneralStatusResponse.class))),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content())
    })
    public ResponseEntity<GeneralStatusResponse> verifyRegistrationToken(@PathVariable String token) {
        Boolean result = authService.verifyAccount(token);

        if (result) return new ResponseEntity<>(new GeneralStatusResponse("OK"), HttpStatus.ACCEPTED);

        return new ResponseEntity<>(new GeneralStatusResponse("Not Found"), HttpStatus.NOT_FOUND);
    }
}
