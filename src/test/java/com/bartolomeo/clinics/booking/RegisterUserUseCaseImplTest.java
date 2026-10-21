package com.bartolomeo.clinics.booking;

import com.bartolomeo.clinics.booking.infrastructure.web.dto.LoginRequest;
import com.bartolomeo.clinics.booking.infrastructure.web.dto.RegisterRequest;
import com.bartolomeo.clinics.booking.infrastructure.web.mapper.AuthDtoMapper;
import com.bartolomeo.clinics.booking.user.application.exception.EmailAlreadyTakenException;
import com.bartolomeo.clinics.booking.user.application.exception.LoginAlreadyTakenException;
import com.bartolomeo.clinics.booking.user.domain.model.RegistrationData;
import com.bartolomeo.clinics.booking.user.domain.model.Role;
import com.bartolomeo.clinics.booking.user.domain.model.User;
import com.bartolomeo.clinics.booking.user.domain.ports.in.RegisterUserUseCase;
import com.bartolomeo.clinics.booking.user.domain.ports.out.PasswordEncoderPort;
import com.bartolomeo.clinics.booking.user.domain.ports.out.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Reglas de negocio del registro.
 *
 * <p>Tests unitarios puros: no levantan Spring ni base de datos. El caso de uso se prueba con
 * los puertos simulados, que es justamente de lo que sirve un port: el dominio se puede
 * probar sin la tecnologia.
 */
class RegisterUserUseCaseImplTest {

    private final UserRepository repository = mock(UserRepository.class);
    private final PasswordEncoderPort passwordEncoder = mock(PasswordEncoderPort.class);
    private final RegisterUserUseCase useCase =
            new com.bartolomeo.clinics.booking.user.application.usecases.RegisterUserUseCaseImpl(
                    repository, passwordEncoder);

    private RegistrationData data() {
        return RegistrationData.builder()
                .login("nicobartok0")
                .password("clave-de-prueba-123")
                .firstName("Nicolás")
                .lastName("Bartolomeo")
                .email("nico@ejemplo.edu")
                .langKey("es")
                .build();
    }

    @Test
    @DisplayName("registra el usuario, lo activa y le asigna ROLE_USER")
    void registra() {
        when(repository.existsByLogin(anyString())).thenReturn(false);
        when(repository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(repository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        User user = useCase.register(data());

        assertThat(user.getLogin()).isEqualTo("nicobartok0");
        assertThat(user.isActivated()).isTrue();
        assertThat(user.getRoles()).containsExactly(Role.ROLE_USER);
        assertThat(user.getPasswordHash()).isEqualTo("hash");
    }

    @Test
    @DisplayName("la contraseña se hashea y nunca se guarda en claro")
    void hasheaLaContrasena() {
        when(repository.existsByLogin(anyString())).thenReturn(false);
        when(repository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash-bcrypt");
        when(repository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        User user = useCase.register(data());

        assertThat(user.getPasswordHash()).isEqualTo("hash-bcrypt");
        verify(passwordEncoder).encode("clave-de-prueba-123");
    }

    @Test
    @DisplayName("rechaza un login ya registrado")
    void rechazaLoginDuplicado() {
        when(repository.existsByLogin("nicobartok0")).thenReturn(true);

        LoginAlreadyTakenException thrown = catchThrowableOfType(
                () -> useCase.register(data()), LoginAlreadyTakenException.class);

        assertThat(thrown.getErrorCode().getCode()).isEqualTo("login-already-taken");
        assertThat(thrown.getMessage()).contains("nicobartok0");

        verify(repository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("rechaza un email ya registrado")
    void rechazaEmailDuplicado() {
        when(repository.existsByLogin(anyString())).thenReturn(false);
        when(repository.existsByEmail("nico@ejemplo.edu")).thenReturn(true);

        EmailAlreadyTakenException thrown = catchThrowableOfType(
                () -> useCase.register(data()), EmailAlreadyTakenException.class);

        assertThat(thrown.getErrorCode().getCode()).isEqualTo("email-already-taken");

        verify(repository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("el idioma por defecto es es")
    void idiomaPorDefecto() {
        RegistrationData sinIdioma = data();
        sinIdioma.setLangKey(null);

        when(repository.existsByLogin(anyString())).thenReturn(false);
        when(repository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(repository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        assertThat(useCase.register(sinIdioma).getLangKey()).isEqualTo("es");
    }
}
