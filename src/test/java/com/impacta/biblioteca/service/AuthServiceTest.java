package com.impacta.biblioteca.service;

import com.impacta.biblioteca.model.Funcionario;
import com.impacta.biblioteca.model.enums.Perfil;
import com.impacta.biblioteca.repository.FuncionarioRepository;
import com.impacta.biblioteca.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — Testes Unitários")
class AuthServiceTest {

    @Mock
    private FuncionarioRepository funcionarioRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private Funcionario funcionario;

    @BeforeEach
    void setUp() {
        funcionario = new Funcionario();
        funcionario.setId(1L);
        funcionario.setNome("Admin");
        funcionario.setEmail("admin@biblioteca.com");
        funcionario.setCpf("11111111111");
        funcionario.setUsername("admin");
        funcionario.setSenha("$2a$10$hashedPassword");
        funcionario.setAtivo(true);
        funcionario.setPerfil(Perfil.ADMIN);
    }

    // ═══════════════════════════════════════════════════════════════
    // login
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("Deve realizar login com sucesso e retornar token JWT")
        void deveRealizarLoginComSucesso() {
            // Arrange
            when(funcionarioRepository.findByEmail("admin@biblioteca.com")).thenReturn(Optional.of(funcionario));
            when(passwordEncoder.matches("senha123", funcionario.getSenha())).thenReturn(true);
            when(jwtService.gerarToken(funcionario)).thenReturn("jwt-token-123");

            // Act
            Map<String, Object> resultado = authService.login("admin@biblioteca.com", "senha123");

            // Assert
            assertThat(resultado).containsEntry("token", "jwt-token-123");
            assertThat(resultado).containsEntry("nome", "Admin");
            assertThat(resultado).containsEntry("email", "admin@biblioteca.com");
            assertThat(resultado).containsEntry("perfil", "ADMIN");
        }

        @Test
        @DisplayName("Deve lançar exceção quando e-mail não for encontrado")
        void deveLancarExcecaoQuandoEmailNaoEncontrado() {
            // Arrange
            when(funcionarioRepository.findByEmail("inexistente@email.com")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authService.login("inexistente@email.com", "senha"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("E-mail ou senha inválidos.");
        }

        @Test
        @DisplayName("Deve lançar exceção quando conta não estiver ativa")
        void deveLancarExcecaoQuandoContaInativa() {
            // Arrange
            funcionario.setAtivo(false);
            when(funcionarioRepository.findByEmail("admin@biblioteca.com")).thenReturn(Optional.of(funcionario));

            // Act & Assert
            assertThatThrownBy(() -> authService.login("admin@biblioteca.com", "senha"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Conta ainda não aprovada");
        }

        @Test
        @DisplayName("Deve lançar exceção quando perfil for nulo (pendente)")
        void deveLancarExcecaoQuandoPerfilNulo() {
            // Arrange
            funcionario.setPerfil(null);
            when(funcionarioRepository.findByEmail("admin@biblioteca.com")).thenReturn(Optional.of(funcionario));

            // Act & Assert
            assertThatThrownBy(() -> authService.login("admin@biblioteca.com", "senha"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Conta pendente de aprovação");
        }

        @Test
        @DisplayName("Deve lançar exceção quando senha estiver incorreta")
        void deveLancarExcecaoQuandoSenhaIncorreta() {
            // Arrange
            when(funcionarioRepository.findByEmail("admin@biblioteca.com")).thenReturn(Optional.of(funcionario));
            when(passwordEncoder.matches("senhaErrada", funcionario.getSenha())).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> authService.login("admin@biblioteca.com", "senhaErrada"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("E-mail ou senha inválidos.");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // solicitarAcesso
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("solicitarAcesso()")
    class SolicitarAcesso {

        @Test
        @DisplayName("Deve criar solicitação de acesso com sucesso")
        void deveCriarSolicitacaoComSucesso() {
            // Arrange
            when(funcionarioRepository.existsByEmail("novo@email.com")).thenReturn(false);
            when(funcionarioRepository.existsByCpf("99999999999")).thenReturn(false);
            when(funcionarioRepository.existsByUsername("novouser")).thenReturn(false);
            when(passwordEncoder.encode("senha123")).thenReturn("$2a$10$encoded");
            when(funcionarioRepository.save(any(Funcionario.class))).thenAnswer(invocation -> {
                Funcionario f = invocation.getArgument(0);
                f.setId(2L);
                return f;
            });

            // Act
            Funcionario resultado = authService.solicitarAcesso(
                    "Novo Funcionário", "novo@email.com", "99999999999", "novouser", "senha123");

            // Assert
            assertThat(resultado.getAtivo()).isFalse();
            assertThat(resultado.getPerfil()).isNull();
            assertThat(resultado.getSenha()).isEqualTo("$2a$10$encoded");
        }

        @Test
        @DisplayName("Deve lançar exceção quando e-mail já existir")
        void deveLancarExcecaoQuandoEmailJaExiste() {
            // Arrange
            when(funcionarioRepository.existsByEmail("existente@email.com")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> authService.solicitarAcesso(
                    "Nome", "existente@email.com", "99999999999", "user", "senha"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("e-mail");
        }

        @Test
        @DisplayName("Deve lançar exceção quando CPF já existir")
        void deveLancarExcecaoQuandoCpfJaExiste() {
            // Arrange
            when(funcionarioRepository.existsByEmail(anyString())).thenReturn(false);
            when(funcionarioRepository.existsByCpf("11111111111")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> authService.solicitarAcesso(
                    "Nome", "novo@email.com", "11111111111", "user", "senha"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("CPF");
        }

        @Test
        @DisplayName("Deve lançar exceção quando username já existir")
        void deveLancarExcecaoQuandoUsernameJaExiste() {
            // Arrange
            when(funcionarioRepository.existsByEmail(anyString())).thenReturn(false);
            when(funcionarioRepository.existsByCpf(anyString())).thenReturn(false);
            when(funcionarioRepository.existsByUsername("admin")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> authService.solicitarAcesso(
                    "Nome", "novo@email.com", "99999999999", "admin", "senha"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("username");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // aprovar
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("aprovar()")
    class Aprovar {

        @Test
        @DisplayName("Deve aprovar funcionário com perfil especificado")
        void deveAprovarComPerfilEspecificado() {
            // Arrange
            funcionario.setAtivo(false);
            funcionario.setPerfil(null);
            when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionario));
            when(funcionarioRepository.save(any(Funcionario.class))).thenReturn(funcionario);

            // Act
            Funcionario resultado = authService.aprovar(1L, Perfil.SUPORTE);

            // Assert
            assertThat(resultado.getAtivo()).isTrue();
            assertThat(resultado.getPerfil()).isEqualTo(Perfil.SUPORTE);
        }

        @Test
        @DisplayName("Deve aprovar funcionário com perfil ATENDENTE quando perfil não for informado")
        void deveAprovarComPerfilPadrao() {
            // Arrange
            funcionario.setAtivo(false);
            funcionario.setPerfil(null);
            when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionario));
            when(funcionarioRepository.save(any(Funcionario.class))).thenReturn(funcionario);

            // Act
            Funcionario resultado = authService.aprovar(1L, null);

            // Assert
            assertThat(resultado.getAtivo()).isTrue();
            assertThat(resultado.getPerfil()).isEqualTo(Perfil.ATENDENTE);
        }

        @Test
        @DisplayName("Deve lançar exceção quando funcionário não for encontrado")
        void deveLancarExcecaoQuandoFuncionarioNaoEncontrado() {
            // Arrange
            when(funcionarioRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authService.aprovar(99L, Perfil.ADMIN))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Funcionário não encontrado.");
        }

        @Test
        @DisplayName("Deve lançar exceção quando funcionário já estiver ativo")
        void deveLancarExcecaoQuandoJaAtivo() {
            // Arrange
            funcionario.setAtivo(true);
            when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionario));

            // Act & Assert
            assertThatThrownBy(() -> authService.aprovar(1L, Perfil.ADMIN))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Este funcionário já está ativo.");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // esqueciSenha
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("esqueciSenha()")
    class EsqueciSenha {

        @Test
        @DisplayName("Deve gerar token de redefinição com sucesso")
        void deveGerarTokenComSucesso() {
            // Arrange
            when(funcionarioRepository.findByEmail("admin@biblioteca.com")).thenReturn(Optional.of(funcionario));
            when(jwtService.gerarTokenRedefinicao("admin@biblioteca.com")).thenReturn("reset-token-456");

            // Act
            String token = authService.esqueciSenha("admin@biblioteca.com", "11111111111");

            // Assert
            assertThat(token).isEqualTo("reset-token-456");
        }

        @Test
        @DisplayName("Deve lançar exceção quando e-mail não for encontrado")
        void deveLancarExcecaoQuandoEmailNaoEncontrado() {
            // Arrange
            when(funcionarioRepository.findByEmail("x@x.com")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authService.esqueciSenha("x@x.com", "000"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Dados não encontrados no sistema.");
        }

        @Test
        @DisplayName("Deve lançar exceção quando CPF não conferir")
        void deveLancarExcecaoQuandoCpfNaoConfere() {
            // Arrange
            when(funcionarioRepository.findByEmail("admin@biblioteca.com")).thenReturn(Optional.of(funcionario));

            // Act & Assert
            assertThatThrownBy(() -> authService.esqueciSenha("admin@biblioteca.com", "00000000000"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Dados não encontrados no sistema.");
        }

        @Test
        @DisplayName("Deve lançar exceção quando conta estiver inativa")
        void deveLancarExcecaoQuandoContaInativa() {
            // Arrange
            funcionario.setAtivo(false);
            when(funcionarioRepository.findByEmail("admin@biblioteca.com")).thenReturn(Optional.of(funcionario));

            // Act & Assert
            assertThatThrownBy(() -> authService.esqueciSenha("admin@biblioteca.com", "11111111111"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Conta inativa");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // redefinirSenha
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("redefinirSenha()")
    class RedefinirSenha {

        @Test
        @DisplayName("Deve redefinir senha com sucesso quando token é válido")
        void deveRedefinirSenhaComSucesso() {
            // Arrange
            when(jwtService.tokenRedefinicaoValido("valid-token")).thenReturn(true);
            when(jwtService.extrairEmail("valid-token")).thenReturn("admin@biblioteca.com");
            when(funcionarioRepository.findByEmail("admin@biblioteca.com")).thenReturn(Optional.of(funcionario));
            when(passwordEncoder.encode("novaSenha123")).thenReturn("$2a$10$newHash");

            // Act
            authService.redefinirSenha("valid-token", "novaSenha123");

            // Assert
            assertThat(funcionario.getSenha()).isEqualTo("$2a$10$newHash");
            verify(funcionarioRepository).save(funcionario);
        }

        @Test
        @DisplayName("Deve lançar exceção quando token for inválido ou expirado")
        void deveLancarExcecaoQuandoTokenInvalido() {
            // Arrange
            when(jwtService.tokenRedefinicaoValido("bad-token")).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> authService.redefinirSenha("bad-token", "novaSenha"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Token inválido ou expirado");
        }

        @Test
        @DisplayName("Deve lançar exceção quando funcionário do token não for encontrado")
        void deveLancarExcecaoQuandoFuncionarioDoTokenNaoEncontrado() {
            // Arrange
            when(jwtService.tokenRedefinicaoValido("valid-token")).thenReturn(true);
            when(jwtService.extrairEmail("valid-token")).thenReturn("ghost@email.com");
            when(funcionarioRepository.findByEmail("ghost@email.com")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authService.redefinirSenha("valid-token", "novaSenha"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Funcionário não encontrado.");
        }
    }
}
