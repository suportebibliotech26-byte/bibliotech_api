package com.impacta.biblioteca.service;

import com.impacta.biblioteca.model.Funcionario;
import com.impacta.biblioteca.model.enums.Perfil;
import com.impacta.biblioteca.repository.FuncionarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FuncionarioService — Testes Unitários")
class FuncionarioServiceTest {

    @Mock
    private FuncionarioRepository funcionarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private FuncionarioService funcionarioService;

    private Funcionario funcionario;

    @BeforeEach
    void setUp() {
        funcionario = new Funcionario();
        funcionario.setId(1L);
        funcionario.setNome("Ana Costa");
        funcionario.setEmail("ana@biblioteca.com");
        funcionario.setCpf("22222222222");
        funcionario.setUsername("anacosta");
        funcionario.setSenha("$2a$10$hashedPassword");
        funcionario.setAtivo(true);
        funcionario.setPerfil(Perfil.ATENDENTE);
    }

    @Nested
    @DisplayName("listarTodos()")
    class ListarTodos {

        @Test
        @DisplayName("Deve retornar lista de todos os funcionários")
        void deveRetornarLista() {
            // Arrange
            when(funcionarioRepository.findAll()).thenReturn(Arrays.asList(funcionario));

            // Act
            List<Funcionario> resultado = funcionarioService.listarTodos();

            // Assert
            assertThat(resultado).hasSize(1);
        }
    }

    @Nested
    @DisplayName("criar()")
    class Criar {

        @Test
        @DisplayName("Deve criar funcionário com sucesso e criptografar senha")
        void deveCriarFuncionarioComSucesso() {
            // Arrange
            Funcionario novo = new Funcionario();
            novo.setNome("Novo");
            novo.setEmail("novo@email.com");
            novo.setCpf("33333333333");
            novo.setUsername("novofunc");
            novo.setSenha("senhaPlana");

            when(funcionarioRepository.existsByEmail("novo@email.com")).thenReturn(false);
            when(funcionarioRepository.existsByCpf("33333333333")).thenReturn(false);
            when(funcionarioRepository.existsByUsername("novofunc")).thenReturn(false);
            when(passwordEncoder.encode("senhaPlana")).thenReturn("$2a$10$encoded");
            when(funcionarioRepository.save(any(Funcionario.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            Funcionario resultado = funcionarioService.criar(novo);

            // Assert
            assertThat(resultado.getSenha()).isEqualTo("$2a$10$encoded");
            assertThat(resultado.getId()).isNull();
        }

        @Test
        @DisplayName("Não deve criptografar senha já hashada (começando com $2a$)")
        void naoDeveCriptografarSenhaJaHashada() {
            // Arrange
            Funcionario novo = new Funcionario();
            novo.setNome("Novo");
            novo.setEmail("novo@email.com");
            novo.setCpf("33333333333");
            novo.setUsername("novofunc");
            novo.setSenha("$2a$10$jaHashado");

            when(funcionarioRepository.existsByEmail(anyString())).thenReturn(false);
            when(funcionarioRepository.existsByCpf(anyString())).thenReturn(false);
            when(funcionarioRepository.existsByUsername(anyString())).thenReturn(false);
            when(funcionarioRepository.save(any(Funcionario.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            Funcionario resultado = funcionarioService.criar(novo);

            // Assert
            assertThat(resultado.getSenha()).isEqualTo("$2a$10$jaHashado");
            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("Deve lançar exceção quando e-mail já existir")
        void deveLancarExcecaoQuandoEmailDuplicado() {
            // Arrange
            when(funcionarioRepository.existsByEmail("ana@biblioteca.com")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> funcionarioService.criar(funcionario))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("e-mail");
        }

        @Test
        @DisplayName("Deve lançar exceção quando CPF já existir")
        void deveLancarExcecaoQuandoCpfDuplicado() {
            // Arrange
            when(funcionarioRepository.existsByEmail(anyString())).thenReturn(false);
            when(funcionarioRepository.existsByCpf("22222222222")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> funcionarioService.criar(funcionario))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("CPF");
        }

        @Test
        @DisplayName("Deve lançar exceção quando username já existir")
        void deveLancarExcecaoQuandoUsernameDuplicado() {
            // Arrange
            when(funcionarioRepository.existsByEmail(anyString())).thenReturn(false);
            when(funcionarioRepository.existsByCpf(anyString())).thenReturn(false);
            when(funcionarioRepository.existsByUsername("anacosta")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> funcionarioService.criar(funcionario))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("username");
        }
    }

    @Nested
    @DisplayName("editar()")
    class Editar {

        @Test
        @DisplayName("Deve editar funcionário e criptografar nova senha")
        void deveEditarECriptografarNovaSenha() {
            // Arrange
            Funcionario atualizado = new Funcionario();
            atualizado.setNome("Ana Atualizada");
            atualizado.setEmail("ana@biblioteca.com");
            atualizado.setCpf("22222222222");
            atualizado.setUsername("anacosta");
            atualizado.setSenha("novaSenhaPlana");
            atualizado.setAtivo(true);
            atualizado.setPerfil(Perfil.SUPORTE);

            when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionario));
            when(passwordEncoder.encode("novaSenhaPlana")).thenReturn("$2a$10$newHash");
            when(funcionarioRepository.save(any(Funcionario.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            Optional<Funcionario> resultado = funcionarioService.editar(1L, atualizado);

            // Assert
            assertThat(resultado).isPresent();
            assertThat(resultado.get().getNome()).isEqualTo("Ana Atualizada");
            assertThat(resultado.get().getSenha()).isEqualTo("$2a$10$newHash");
        }

        @Test
        @DisplayName("Não deve alterar senha quando não fornecida na edição")
        void naoDeveAlterarSenhaQuandoNaoFornecida() {
            // Arrange
            Funcionario atualizado = new Funcionario();
            atualizado.setNome("Ana Atualizada");
            atualizado.setEmail("ana@biblioteca.com");
            atualizado.setCpf("22222222222");
            atualizado.setUsername("anacosta");
            atualizado.setSenha(null);
            atualizado.setAtivo(true);
            atualizado.setPerfil(Perfil.ATENDENTE);

            when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionario));
            when(funcionarioRepository.save(any(Funcionario.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            Optional<Funcionario> resultado = funcionarioService.editar(1L, atualizado);

            // Assert
            assertThat(resultado).isPresent();
            assertThat(resultado.get().getSenha()).isEqualTo("$2a$10$hashedPassword");
            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("Deve retornar Optional vazio quando funcionário não existir")
        void deveRetornarVazioQuandoNaoExiste() {
            // Arrange
            when(funcionarioRepository.findById(99L)).thenReturn(Optional.empty());

            // Act
            Optional<Funcionario> resultado = funcionarioService.editar(99L, new Funcionario());

            // Assert
            assertThat(resultado).isEmpty();
        }
    }

    @Nested
    @DisplayName("deletar()")
    class Deletar {

        @Test
        @DisplayName("Deve deletar funcionário com sucesso")
        void deveDeletarComSucesso() {
            // Arrange
            when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionario));

            // Act
            boolean resultado = funcionarioService.deletar(1L);

            // Assert
            assertThat(resultado).isTrue();
            verify(funcionarioRepository).delete(funcionario);
        }

        @Test
        @DisplayName("Deve retornar false quando funcionário não for encontrado")
        void deveRetornarFalseQuandoNaoEncontrado() {
            // Arrange
            when(funcionarioRepository.findById(99L)).thenReturn(Optional.empty());

            // Act
            boolean resultado = funcionarioService.deletar(99L);

            // Assert
            assertThat(resultado).isFalse();
        }
    }
}
