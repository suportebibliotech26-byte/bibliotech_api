package com.impacta.biblioteca.service;

import com.impacta.biblioteca.model.Beneficiador;
import com.impacta.biblioteca.model.Livro;
import com.impacta.biblioteca.repository.BeneficiadorRepository;
import com.impacta.biblioteca.repository.LivroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LivroService — Testes Unitários")
class LivroServiceTest {

    @Mock
    private LivroRepository livroRepository;

    @Mock
    private BeneficiadorRepository beneficiadorRepository;

    @InjectMocks
    private LivroService livroService;

    private Livro livro;
    private Beneficiador beneficiador;

    @BeforeEach
    void setUp() {
        beneficiador = new Beneficiador();
        beneficiador.setId(1L);
        beneficiador.setNome("Editora ABC");
        beneficiador.setCnpj("12345678000199");
        beneficiador.setTelefone("1133334444");

        livro = new Livro();
        livro.setId(1L);
        livro.setTitulo("Clean Code");
        livro.setAutor("Robert Martin");
        livro.setAno(2008);
        livro.setDisponivel(true);
        livro.setGenero("Tecnologia");
        livro.setIsbn("978-0132350884");
    }

    @Nested
    @DisplayName("listarTodos()")
    class ListarTodos {

        @Test
        @DisplayName("Deve retornar lista de todos os livros")
        void deveRetornarLista() {
            when(livroRepository.findAll()).thenReturn(List.of(livro));
            List<Livro> resultado = livroService.listarTodos();
            assertThat(resultado).hasSize(1);
        }
    }

    @Nested
    @DisplayName("buscarPorId()")
    class BuscarPorId {

        @Test
        @DisplayName("Deve retornar livro quando ID existir")
        void deveRetornarLivro() {
            when(livroRepository.findById(1L)).thenReturn(Optional.of(livro));
            Optional<Livro> resultado = livroService.buscarPorId(1L);
            assertThat(resultado).isPresent();
        }

        @Test
        @DisplayName("Deve retornar vazio quando ID não existir")
        void deveRetornarVazio() {
            when(livroRepository.findById(99L)).thenReturn(Optional.empty());
            Optional<Livro> resultado = livroService.buscarPorId(99L);
            assertThat(resultado).isEmpty();
        }
    }

    @Nested
    @DisplayName("criar()")
    class Criar {

        @Test
        @DisplayName("Deve criar livro com sucesso com campos obrigatórios")
        void deveCriarLivroComSucesso() {
            // Arrange
            Map<String, Object> body = new HashMap<>();
            body.put("titulo", "Novo Livro");
            body.put("autor", "Autor X");
            body.put("ano", 2024);
            when(livroRepository.save(any(Livro.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            Livro resultado = livroService.criar(body);

            // Assert
            assertThat(resultado.getTitulo()).isEqualTo("Novo Livro");
            assertThat(resultado.getDisponivel()).isTrue();
        }

        @Test
        @DisplayName("Deve criar livro com beneficiador quando informado")
        void deveCriarLivroComBeneficiador() {
            // Arrange
            Map<String, Object> body = new HashMap<>();
            body.put("titulo", "Livro Doado");
            body.put("autor", "Autor Y");
            body.put("ano", 2023);
            body.put("beneficiadorId", 1);

            when(beneficiadorRepository.findById(1L)).thenReturn(Optional.of(beneficiador));
            when(livroRepository.save(any(Livro.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            Livro resultado = livroService.criar(body);

            // Assert
            assertThat(resultado.getBeneficiador()).isNotNull();
            assertThat(resultado.getBeneficiador().getNome()).isEqualTo("Editora ABC");
        }

        @Test
        @DisplayName("Deve lançar exceção quando campos obrigatórios forem nulos")
        void deveLancarExcecaoQuandoCamposObrigatoriosNulos() {
            // Arrange
            Map<String, Object> body = new HashMap<>();
            body.put("titulo", null);
            body.put("autor", null);
            body.put("ano", null);

            // Act & Assert
            assertThatThrownBy(() -> livroService.criar(body))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("obrigatórios");
        }

        @Test
        @DisplayName("Deve lançar exceção quando beneficiador não for encontrado")
        void deveLancarExcecaoQuandoBeneficiadorNaoEncontrado() {
            // Arrange
            Map<String, Object> body = new HashMap<>();
            body.put("titulo", "Livro X");
            body.put("autor", "Autor X");
            body.put("ano", 2024);
            body.put("beneficiadorId", 99);

            when(beneficiadorRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> livroService.criar(body))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Beneficiador não encontrado.");
        }

        @Test
        @DisplayName("Deve definir gênero e ISBN quando informados")
        void deveDefinirGeneroEIsbn() {
            // Arrange
            Map<String, Object> body = new HashMap<>();
            body.put("titulo", "Livro Completo");
            body.put("autor", "Autor Z");
            body.put("ano", 2024);
            body.put("genero", "Ficção");
            body.put("isbn", "978-1234567890");
            when(livroRepository.save(any(Livro.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            Livro resultado = livroService.criar(body);

            // Assert
            assertThat(resultado.getGenero()).isEqualTo("Ficção");
            assertThat(resultado.getIsbn()).isEqualTo("978-1234567890");
        }
    }

    @Nested
    @DisplayName("editar()")
    class Editar {

        @Test
        @DisplayName("Deve editar livro com sucesso")
        void deveEditarComSucesso() {
            // Arrange
            Map<String, Object> body = new HashMap<>();
            body.put("titulo", "Título Atualizado");

            when(livroRepository.findById(1L)).thenReturn(Optional.of(livro));
            when(livroRepository.save(any(Livro.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            Optional<Livro> resultado = livroService.editar(1L, body);

            // Assert
            assertThat(resultado).isPresent();
            assertThat(resultado.get().getTitulo()).isEqualTo("Título Atualizado");
        }

        @Test
        @DisplayName("Deve remover beneficiador quando informado como null")
        void deveRemoverBeneficiador() {
            // Arrange
            livro.setBeneficiador(beneficiador);
            Map<String, Object> body = new HashMap<>();
            body.put("beneficiadorId", null);

            when(livroRepository.findById(1L)).thenReturn(Optional.of(livro));
            when(livroRepository.save(any(Livro.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            Optional<Livro> resultado = livroService.editar(1L, body);

            // Assert
            assertThat(resultado).isPresent();
            assertThat(resultado.get().getBeneficiador()).isNull();
        }

        @Test
        @DisplayName("Deve retornar vazio quando livro não existir")
        void deveRetornarVazioQuandoNaoExiste() {
            when(livroRepository.findById(99L)).thenReturn(Optional.empty());
            Optional<Livro> resultado = livroService.editar(99L, new HashMap<>());
            assertThat(resultado).isEmpty();
        }
    }

    @Nested
    @DisplayName("deletar()")
    class Deletar {

        @Test
        @DisplayName("Deve deletar livro com sucesso")
        void deveDeletarComSucesso() {
            when(livroRepository.findById(1L)).thenReturn(Optional.of(livro));
            boolean resultado = livroService.deletar(1L);
            assertThat(resultado).isTrue();
            verify(livroRepository).delete(livro);
        }

        @Test
        @DisplayName("Deve retornar false quando livro não for encontrado")
        void deveRetornarFalseQuandoNaoEncontrado() {
            when(livroRepository.findById(99L)).thenReturn(Optional.empty());
            boolean resultado = livroService.deletar(99L);
            assertThat(resultado).isFalse();
        }
    }
}
