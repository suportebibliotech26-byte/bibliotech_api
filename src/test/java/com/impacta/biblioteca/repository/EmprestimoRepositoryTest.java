package com.impacta.biblioteca.repository;

import com.impacta.biblioteca.model.Beneficiador;
import com.impacta.biblioteca.model.Cliente;
import com.impacta.biblioteca.model.Emprestimo;
import com.impacta.biblioteca.model.Livro;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("EmprestimoRepository — Testes de Integração JPA")
class EmprestimoRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EmprestimoRepository emprestimoRepository;

    private Cliente cliente;
    private Livro livro;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setNome("Maria Silva");
        cliente.setEmail("maria@email.com");
        cliente.setCpf("12345678900");
        cliente.setTelefone("11999999999");
        cliente.setStatusBloqueio("ATIVO");
        entityManager.persistAndFlush(cliente);

        livro = new Livro();
        livro.setTitulo("Clean Code");
        livro.setAutor("Robert Martin");
        livro.setAno(2008);
        livro.setDisponivel(false);
        entityManager.persistAndFlush(livro);
    }

    @Test
    @DisplayName("Deve encontrar empréstimos atrasados do cliente")
    void deveEncontrarEmprestimosAtrasados() {
        // Arrange — cria empréstimo com data prevista no passado (atrasado)
        Emprestimo atrasado = new Emprestimo();
        atrasado.setCliente(cliente);
        atrasado.setLivro(livro);
        atrasado.setDataEmprestimo(LocalDate.now().minusDays(14));
        atrasado.setDataPrevistaDevolucao(LocalDate.now().minusDays(7));
        atrasado.setDataDevolucao(null); // não devolvido
        atrasado.setPrazoDias(7);
        atrasado.setRenovacoesRealizadas(0);
        atrasado.setStatus("ATIVO");
        entityManager.persistAndFlush(atrasado);

        // Act
        List<Emprestimo> resultado = emprestimoRepository
                .findEmprestimosAtrasadosPorCliente(cliente, LocalDate.now());

        // Assert
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getCliente().getNome()).isEqualTo("Maria Silva");
    }

    @Test
    @DisplayName("Não deve retornar empréstimos já devolvidos como atrasados")
    void naoDeveRetornarEmprestimosDevolvidos() {
        // Arrange — empréstimo atrasado MAS já devolvido
        Emprestimo devolvido = new Emprestimo();
        devolvido.setCliente(cliente);
        devolvido.setLivro(livro);
        devolvido.setDataEmprestimo(LocalDate.now().minusDays(14));
        devolvido.setDataPrevistaDevolucao(LocalDate.now().minusDays(7));
        devolvido.setDataDevolucao(LocalDate.now().minusDays(1)); // já devolvido
        devolvido.setPrazoDias(7);
        devolvido.setRenovacoesRealizadas(0);
        devolvido.setStatus("DEVOLVIDO");
        entityManager.persistAndFlush(devolvido);

        // Act
        List<Emprestimo> resultado = emprestimoRepository
                .findEmprestimosAtrasadosPorCliente(cliente, LocalDate.now());

        // Assert
        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Não deve retornar empréstimos dentro do prazo como atrasados")
    void naoDeveRetornarEmprestimosDentroDoPrazo() {
        // Arrange — empréstimo com data prevista no futuro (dentro do prazo)
        Emprestimo noPrazo = new Emprestimo();
        noPrazo.setCliente(cliente);
        noPrazo.setLivro(livro);
        noPrazo.setDataEmprestimo(LocalDate.now());
        noPrazo.setDataPrevistaDevolucao(LocalDate.now().plusDays(7));
        noPrazo.setDataDevolucao(null);
        noPrazo.setPrazoDias(7);
        noPrazo.setRenovacoesRealizadas(0);
        noPrazo.setStatus("ATIVO");
        entityManager.persistAndFlush(noPrazo);

        // Act
        List<Emprestimo> resultado = emprestimoRepository
                .findEmprestimosAtrasadosPorCliente(cliente, LocalDate.now());

        // Assert
        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando cliente não possui empréstimos")
    void deveRetornarListaVaziaQuandoSemEmprestimos() {
        // Act
        List<Emprestimo> resultado = emprestimoRepository
                .findEmprestimosAtrasadosPorCliente(cliente, LocalDate.now());

        // Assert
        assertThat(resultado).isEmpty();
    }
}
