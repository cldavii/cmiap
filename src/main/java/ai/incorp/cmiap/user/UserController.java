package ai.incorp.cmiap.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDto register(@RequestBody UserRegistrationDto registrationDto) {
        return userMapper.toResponseDto(userService.registerUser(registrationDto));
    }

    @GetMapping
    public List<UserResponseDto> listUsers() {
        return userService.listUsers().stream()
                .map(userMapper::toResponseDto)
                .toList();
    }

    @GetMapping("/by-id/{id}")
    public UserResponseDto findUserById(@PathVariable UUID id) {
        return userMapper.toResponseDto(userService.findUserById(id));
    }

    @GetMapping("/by-name/{name}")
    public List<UserResponseDto> findUsersByName(@PathVariable String name) {
        return userService.findUsersByName(name).stream()
                .map(userMapper::toResponseDto)
                .toList();
    }

    @GetMapping("/by-cpf/{cpf}")
    public UserResponseDto findUserByCpf(@PathVariable String cpf) {
        return userMapper.toResponseDto(userService.findUserByCpf(cpf));
    }

    @GetMapping("/by-email/{email}")
    public UserResponseDto findUserByEmail(@PathVariable String email) {
        return userMapper.toResponseDto(userService.findUserByEmail(email));
    }

    @PutMapping("/{id}")
    public UserResponseDto updateUser(
            @PathVariable UUID id,
            @RequestBody UserUpdateDto updateDto
    ) {
        return userMapper.toResponseDto(userService.updateUser(id, updateDto));
    }

    @PatchMapping("/{id}")
    public UserResponseDto patchUser(
            @PathVariable UUID id,
            @RequestBody UserPatchDto patchDto
    ) {
        return userMapper.toResponseDto(userService.patchUser(id, patchDto));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
    }
}
