package ai.incorp.cmiap.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest {

    private final UserService userService = mock(UserService.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new UserController(userService, userMapper))
                .build();
    }

    @Test
    void registerListAndLookupEndpointsReturnExpectedResponses() throws Exception {
        UUID id = UUID.randomUUID();
        User user = createUser(id);
        UserResponseDto dto = new UserResponseDto(
                id, "Ana", "Silva", 52998224725L, "ana@example.com", UserRole.DEFAULT, UserDepartment.MARKETING);

        when(userService.registerUser(any())).thenReturn(user);
        when(userService.listUsers()).thenReturn(List.of(user));
        when(userService.findUserById(id)).thenReturn(user);
        when(userService.findUsersByName("Ana")).thenReturn(List.of(user));
        when(userService.findUserByCpf(52998224725L)).thenReturn(user);
        when(userService.findUserByEmail("ana@example.com")).thenReturn(user);
        when(userMapper.toResponseDto(user)).thenReturn(dto);

        var registration = new UserRegistrationDto("Ana", "Silva", 52998224725L, "ana@example.com", "secret123", UserDepartment.MARKETING);

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registration)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ana@example.com"));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()));

        mockMvc.perform(get("/users/by-id/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana"));

        mockMvc.perform(get("/users/by-name/{name}", "Ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Ana"));

        mockMvc.perform(get("/users/by-cpf/{cpf}", 52998224725L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value(52998224725L));

        mockMvc.perform(get("/users/by-email/{email}", "ana@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@example.com"));
    }

    @Test
    void updatePatchAndDeleteEndpointsWork() throws Exception {
        UUID id = UUID.randomUUID();
        User user = createUser(id);
        UserResponseDto dto = new UserResponseDto(
                id, "Bia", "Costa", 52998224725L, "bia@example.com", UserRole.DEFAULT, UserDepartment.MARKETING);

        when(userService.updateUser(eq(id), any())).thenReturn(user);
        when(userService.patchUser(eq(id), any())).thenReturn(user);
        when(userMapper.toResponseDto(user)).thenReturn(dto);

        mockMvc.perform(put("/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserUpdateDto("Bia", "Costa", 52998224725L, "bia@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Bia"));

        mockMvc.perform(patch("/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserPatchDto("Bia", null, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Bia"));

        mockMvc.perform(delete("/users/{id}", id))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(id);
    }

    private static User createUser(UUID id) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setName("Ana");
        user.setSurname("Silva");
        user.setCpf(52998224725L);
        user.setEmail("ana@example.com");
        user.setRole(UserRole.DEFAULT);
        user.setDepartment(UserDepartment.MARKETING);
        return user;
    }
}

