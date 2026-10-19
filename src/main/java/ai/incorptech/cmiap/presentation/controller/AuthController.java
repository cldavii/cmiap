package ai.incorptech.cmiap.presentation.controller;

import ai.incorptech.cmiap.application.useCase.AuthenticateUserService;
import ai.incorptech.cmiap.application.useCase.RegisterUserService;
import ai.incorptech.cmiap.presentation.controller.dto.auth.LoginRequestDTO;
import ai.incorptech.cmiap.presentation.controller.dto.auth.RegisterRequestDTO;
import ai.incorptech.cmiap.presentation.controller.dto.auth.TokenResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticateUserService authenticateUserService;
    private final RegisterUserService registerUserService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponseDTO> login(@RequestBody @Valid LoginRequestDTO request) {
        String token = authenticateUserService.execute(request.email(), request.password());
        TokenResponseDTO response = new TokenResponseDTO(token, "Bearer");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterRequestDTO request) {
        registerUserService.execute(request.name(), request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
