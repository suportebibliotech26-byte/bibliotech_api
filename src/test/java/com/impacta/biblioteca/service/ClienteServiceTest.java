package com.impacta.biblioteca.service;

import com.impacta.biblioteca.model.Cliente;
import com.impacta.biblioteca.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteService — Testes Unitários")
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClienteService clienteService;

    private Cliente cliente;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Maria Silva");
        cliente.setEmail("maria@email.com");
        cliente.setCpf("12345678900");
        cliente.setTelefone("11988887777");
        cliente.setStatusBloqueio("ATIVO");
        cliente.setSexo("F");
        cliente.setDataNascimento(LocalDate.of(1990, 5, 15));
    }

    @Nested
    @DisplayName("listarTodos()")
    class ListarTodos {

        @Test
        @DisplayName("Deve retornar lista de todos os clientes")
        void deveRetornarListaDeClientes() {
            // Arrange
            Cliente cliente2 = new Cliente();
            cliente2.setId(2L);
            cliente2.setNome("Pedro Santos");
            when(clienteRepository.findAll()).thenReturn(Arrays.asList(cliente, cliente2));

            // Act
            List<Cliente> resultado = clienteService.listarTodos();

            // Assert
            assertThat(resultado).hasSize(2);
            verify(clienteRepository).findAll();
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não houver clientes")
        void deveRetornarListaVazia() {
            // Arrange
            when(clienteRepository.findAll()).thenReturn(List.of());

            // Act
            List<Cliente> resultado = clienteService.listarTodos();

            // Assert
            assertThat(resultado).isEmpty();
        }
    }

    @Nested
    @DisplayName("buscarPorId()")
    class BuscarPorId {

        @Test
        @DisplayName("Deve retornar cliente quando ID existir")
        void deveRetornarClienteQuandoIdExiste() {
            // Arrange
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

            // Act
            Optional<Cliente> resultado = clienteService.buscarPorId(1L);

            // Assert
            assertThat(resultado).isPresent();
            assertThat(resultado.get().getNome()).isEqualTo("Maria Silva");
        }

        @Test
        @DisplayName("Deve retornar Optional vazio quando ID não existir")
        void deveRetornarVazioQuandoIdNaoExiste() {
            // Arrange
            when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

            // Act
            Optional<Cliente> resultado = clienteService.buscarPorId(99L);

            // Assert
            assertThat(resultado).isEmpty();
        }
    }

    @Nested
    @DisplayName("criar()")
    class Criar {

        @Test
        @DisplayName("Deve criar cliente com sucesso")
        void deveCriarClienteComSucesso() {
            // Arrange
            when(clienteRepository.existsByEmail("maria@email.com")).thenReturn(false);
            when(clienteRepository.existsByCpf("12345678900")).thenReturn(false);
            when(clienteRepository.save(any(Cliente.class))).thenReturn(cliente);

            // Act
            Cliente resultado = clienteService.criar(cliente);

            // Assert
            assertThat(resultado).isNotNull();
            assertThat(resultado.getNome()).isEqualTo("Maria Silva");
        }

        @Test
        @DisplayName("Deve lançar exceção quando e-mail já estiver cadastrado")
        void deveLancarExcecaoQuandoEmailDuplicado() {
            // Arrange
            when(clienteRepository.existsByEmail("maria@email.com")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> clienteService.criar(cliente))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("e-mail");
        }

        @Test
        @DisplayName("Deve lançar exceção quando CPF já estiver cadastrado")
        void deveLancarExcecaoQuandoCpfDuplicado() {
            // Arrange
            when(clienteRepository.existsByEmail("maria@email.com")).thenReturn(false);
            when(clienteRepository.existsByCpf("12345678900")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> clienteService.criar(cliente))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("CPF");
        }

        @Test
        @DisplayName("Deve setar ID como null para garantir novo registro")
        void deveSetarIdComoNull() {
            // Arrange
            cliente.setId(999L);
            when(clienteRepository.existsByEmail(anyString())).thenReturn(false);
            when(clienteRepository.existsByCpf(anyString())).thenReturn(false);
            when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            Cliente resultado = clienteService.criar(cliente);

            // Assert
            assertThat(resultado.getId()).isNull();
        }
    }

    @Nested
    @DisplayName("editar()")
    class Editar {

        @Test
        @DisplayName("Deve editar cliente com sucesso")
        void deveEditarClienteComSucesso() {
            // Arrange
            Cliente atualizado = new Cliente();
            atualizado.setNome("Maria Atualizada");
            atualizado.setEmail("maria@email.com");
            atualizado.setCpf("12345678900");
            atualizado.setTelefone("11999998888");
            atualizado.setSexo("F");
            atualizado.setDataNascimento(LocalDate.of(1990, 5, 15));

            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
            when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            Optional<Cliente> resultado = clienteService.editar(1L, atualizado);

            // Assert
            assertThat(resultado).isPresent();
            assertThat(resultado.get().getNome()).isEqualTo("Maria Atualizada");
        }

        @Test
        @DisplayName("Deve lançar exceção ao editar com e-mail já existente de outro cliente")
        void deveLancarExcecaoQuandoEmailJaExisteEmOutro() {
            // Arrange
            Cliente atualizado = new Cliente();
            atualizado.setEmail("outro@email.com");
            atualizado.setCpf("12345678900");

            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
            when(clienteRepository.existsByEmail("outro@email.com")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> clienteService.editar(1L, atualizado))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("e-mail");
        }

        @Test
        @DisplayName("Deve retornar Optional vazio quando cliente não for encontrado")
        void deveRetornarVazioQuandoClienteNaoExiste() {
            // Arrange
            when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

            // Act
            Optional<Cliente> resultado = clienteService.editar(99L, new Cliente());

            // Assert
            assertThat(resultado).isEmpty();
        }
    }

    @Nested
    @DisplayName("deletar()")
    class Deletar {

        @Test
        @DisplayName("Deve deletar cliente com sucesso")
        void deveDeletarClienteComSucesso() {
            // Arrange
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

            // Act
            boolean resultado = clienteService.deletar(1L);

            // Assert
            assertThat(resultado).isTrue();
            verify(clienteRepository).delete(cliente);
        }

        @Test
        @DisplayName("Deve retornar false quando cliente não for encontrado")
        void deveRetornarFalseQuandoNaoEncontrado() {
            // Arrange
            when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

            // Act
            boolean resultado = clienteService.deletar(99L);

            // Assert
            assertThat(resultado).isFalse();
            verify(clienteRepository, never()).delete(any());
        }
    }
}
