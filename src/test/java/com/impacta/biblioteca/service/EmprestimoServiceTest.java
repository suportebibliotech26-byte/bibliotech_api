package com.impacta.biblioteca.service;

import com.impacta.biblioteca.model.Cliente;
import com.impacta.biblioteca.model.Emprestimo;
import com.impacta.biblioteca.model.Livro;
import com.impacta.biblioteca.repository.ClienteRepository;
import com.impacta.biblioteca.repository.EmprestimoRepository;
import com.impacta.biblioteca.repository.LivroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmprestimoService — Testes Unitários")
class EmprestimoServiceTest {

    @Mock
    private EmprestimoRepository emprestimoRepository;

    @Mock
    private LivroRepository livroRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private EmprestimoService emprestimoService;

    private Livro livro;
    private Cliente cliente;
    private Emprestimo emprestimo;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("João Silva");
        cliente.setEmail("joao@email.com");
        cliente.setCpf("12345678900");
        cliente.setTelefone("11999999999");
        cliente.setStatusBloqueio("ATIVO");

        livro = new Livro();
        livro.setId(1L);
        livro.setTitulo("Clean Code");
        livro.setAutor("Robert C. Martin");
        livro.setAno(2008);
        livro.setDisponivel(true);

        emprestimo = new Emprestimo();
        emprestimo.setId(1L);
        emprestimo.setCliente(cliente);
        emprestimo.setLivro(livro);
        emprestimo.setDataEmprestimo(LocalDate.now());
        emprestimo.setDataPrevistaDevolucao(LocalDate.now().plusDays(7));
        emprestimo.setPrazoDias(7);
        emprestimo.setRenovacoesRealizadas(0);
        emprestimo.setStatus("ATIVO");
    }

    // ═══════════════════════════════════════════════════════════════
    // criarEmprestimo
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("criarEmprestimo()")
    class CriarEmprestimo {

        @Test
        @DisplayName("Deve criar empréstimo com sucesso quando livro está disponível")
        void deveCriarEmprestimoComSucesso() {
            // Arrange
            when(livroRepository.findById(1L)).thenReturn(Optional.of(livro));
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
            when(livroRepository.save(any(Livro.class))).thenReturn(livro);
            when(emprestimoRepository.save(any(Emprestimo.class))).thenReturn(emprestimo);

            // Act
            Emprestimo resultado = emprestimoService.criarEmprestimo(1L, 1L);

            // Assert
            assertThat(resultado).isNotNull();
            assertThat(resultado.getStatus()).isEqualTo("ATIVO");
            verify(livroRepository).save(argThat(l -> !l.getDisponivel()));
            verify(emprestimoRepository).save(any(Emprestimo.class));
        }

        @Test
        @DisplayName("Deve lançar exceção quando livro não for encontrado")
        void deveLancarExcecaoQuandoLivroNaoEncontrado() {
            // Arrange
            when(livroRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> emprestimoService.criarEmprestimo(99L, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Livro não encontrado.");
        }

        @Test
        @DisplayName("Deve lançar exceção quando livro já está emprestado (indisponível)")
        void deveLancarExcecaoQuandoLivroIndisponivel() {
            // Arrange
            livro.setDisponivel(false);
            when(livroRepository.findById(1L)).thenReturn(Optional.of(livro));

            // Act & Assert
            assertThatThrownBy(() -> emprestimoService.criarEmprestimo(1L, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Livro já está emprestado.");
        }

        @Test
        @DisplayName("Deve lançar exceção quando cliente não for encontrado")
        void deveLancarExcecaoQuandoClienteNaoEncontrado() {
            // Arrange
            when(livroRepository.findById(1L)).thenReturn(Optional.of(livro));
            when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> emprestimoService.criarEmprestimo(1L, 99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Cliente não encontrado.");
        }

        @Test
        @DisplayName("Deve marcar livro como indisponível ao criar empréstimo")
        void deveMarcarLivroComoIndisponivel() {
            // Arrange
            when(livroRepository.findById(1L)).thenReturn(Optional.of(livro));
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
            when(livroRepository.save(any(Livro.class))).thenReturn(livro);
            when(emprestimoRepository.save(any(Emprestimo.class))).thenReturn(emprestimo);

            // Act
            emprestimoService.criarEmprestimo(1L, 1L);

            // Assert
            assertThat(livro.getDisponivel()).isFalse();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // renovarEmprestimo
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("renovarEmprestimo()")
    class RenovarEmprestimo {

        @Test
        @DisplayName("Deve renovar empréstimo com sucesso na primeira renovação")
        void deveRenovarComSucessoPrimeiraVez() {
            // Arrange
            emprestimo.setRenovacoesRealizadas(0);
            when(emprestimoRepository.findById(1L)).thenReturn(Optional.of(emprestimo));
            when(emprestimoRepository.findEmprestimosAtrasadosPorCliente(eq(cliente), any(LocalDate.class)))
                    .thenReturn(Collections.emptyList());
            when(emprestimoRepository.save(any(Emprestimo.class))).thenReturn(emprestimo);

            // Act
            Emprestimo resultado = emprestimoService.renovarEmprestimo(1L);

            // Assert
            assertThat(resultado.getRenovacoesRealizadas()).isEqualTo(1);
            assertThat(resultado.getDataPrevistaDevolucao()).isEqualTo(LocalDate.now().plusDays(7));
        }

        @Test
        @DisplayName("Deve renovar empréstimo com sucesso na segunda renovação")
        void deveRenovarComSucessoSegundaVez() {
            // Arrange
            emprestimo.setRenovacoesRealizadas(1);
            when(emprestimoRepository.findById(1L)).thenReturn(Optional.of(emprestimo));
            when(emprestimoRepository.findEmprestimosAtrasadosPorCliente(eq(cliente), any(LocalDate.class)))
                    .thenReturn(Collections.emptyList());
            when(emprestimoRepository.save(any(Emprestimo.class))).thenReturn(emprestimo);

            // Act
            Emprestimo resultado = emprestimoService.renovarEmprestimo(1L);

            // Assert
            assertThat(resultado.getRenovacoesRealizadas()).isEqualTo(2);
        }

        @Test
        @DisplayName("Deve lançar exceção quando limite de 2 renovações for atingido")
        void deveLancarExcecaoQuandoLimiteDeRenovacoesAtingido() {
            // Arrange
            emprestimo.setRenovacoesRealizadas(2);
            when(emprestimoRepository.findById(1L)).thenReturn(Optional.of(emprestimo));

            // Act & Assert
            assertThatThrownBy(() -> emprestimoService.renovarEmprestimo(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Limite de renovações atingido");
        }

        @Test
        @DisplayName("Deve lançar exceção quando cliente possui empréstimos em atraso")
        void deveLancarExcecaoQuandoClienteComAtrasos() {
            // Arrange
            emprestimo.setRenovacoesRealizadas(0);
            Emprestimo emprestimoAtrasado = new Emprestimo();
            emprestimoAtrasado.setId(2L);

            when(emprestimoRepository.findById(1L)).thenReturn(Optional.of(emprestimo));
            when(emprestimoRepository.findEmprestimosAtrasadosPorCliente(eq(cliente), any(LocalDate.class)))
                    .thenReturn(List.of(emprestimoAtrasado));

            // Act & Assert
            assertThatThrownBy(() -> emprestimoService.renovarEmprestimo(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Renovação não permitida");
        }

        @Test
        @DisplayName("Deve lançar exceção quando empréstimo não for encontrado")
        void deveLancarExcecaoQuandoEmprestimoNaoEncontrado() {
            // Arrange
            when(emprestimoRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> emprestimoService.renovarEmprestimo(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Empréstimo não encontrado");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // registrarDevolucao
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("registrarDevolucao()")
    class RegistrarDevolucao {

        @Test
        @DisplayName("Deve registrar devolução com sucesso dentro do prazo")
        void deveRegistrarDevolucaoDentroDoPrazo() {
            // Arrange
            emprestimo.setDataPrevistaDevolucao(LocalDate.now().plusDays(3));
            when(emprestimoRepository.findById(1L)).thenReturn(Optional.of(emprestimo));
            when(livroRepository.save(any(Livro.class))).thenReturn(livro);
            when(emprestimoRepository.save(any(Emprestimo.class))).thenReturn(emprestimo);

            // Act
            Emprestimo resultado = emprestimoService.registrarDevolucao(1L);

            // Assert
            assertThat(resultado.getStatus()).isEqualTo("DEVOLVIDO");
            assertThat(resultado.getDataDevolucao()).isEqualTo(LocalDate.now());
            assertThat(livro.getDisponivel()).isTrue();
            verify(clienteRepository, never()).save(any(Cliente.class));
        }

        @Test
        @DisplayName("Deve BLOQUEAR cliente quando devolução for em atraso")
        void deveBloquearClienteQuandoDevolucaoEmAtraso() {
            // Arrange
            emprestimo.setDataPrevistaDevolucao(LocalDate.now().minusDays(1));
            when(emprestimoRepository.findById(1L)).thenReturn(Optional.of(emprestimo));
            when(livroRepository.save(any(Livro.class))).thenReturn(livro);
            when(clienteRepository.save(any(Cliente.class))).thenReturn(cliente);
            when(emprestimoRepository.save(any(Emprestimo.class))).thenReturn(emprestimo);

            // Act
            emprestimoService.registrarDevolucao(1L);

            // Assert
            assertThat(cliente.getStatusBloqueio()).isEqualTo("BLOQUEADO");
            verify(clienteRepository).save(cliente);
        }

        @Test
        @DisplayName("Deve marcar livro como disponível após devolução")
        void deveMarcarLivroComoDisponivelAposDevolucao() {
            // Arrange
            livro.setDisponivel(false);
            emprestimo.setDataPrevistaDevolucao(LocalDate.now().plusDays(3));
            when(emprestimoRepository.findById(1L)).thenReturn(Optional.of(emprestimo));
            when(livroRepository.save(any(Livro.class))).thenReturn(livro);
            when(emprestimoRepository.save(any(Emprestimo.class))).thenReturn(emprestimo);

            // Act
            emprestimoService.registrarDevolucao(1L);

            // Assert
            assertThat(livro.getDisponivel()).isTrue();
        }

        @Test
        @DisplayName("Deve lançar exceção quando empréstimo não for encontrado")
        void deveLancarExcecaoQuandoEmprestimoNaoEncontrado() {
            // Arrange
            when(emprestimoRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> emprestimoService.registrarDevolucao(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Empréstimo não encontrado");
        }
    }
}
