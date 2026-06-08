package com.impacta.biblioteca.repository;

import com.impacta.biblioteca.model.Ticket;
import com.impacta.biblioteca.model.enums.Categoria;
import com.impacta.biblioteca.model.enums.Prioridade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("TicketRepository — Testes de Integração JPA")
class TicketRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TicketRepository ticketRepository;

    @BeforeEach
    void setUp() {
        Ticket aberto1 = new Ticket();
        aberto1.setTitulo("Bug no Login");
        aberto1.setDescricao("Não consigo logar");
        aberto1.setCategoria(Categoria.BUG);
        aberto1.setPrioridade(Prioridade.ALTA);
        aberto1.setStatus("ABERTO");
        entityManager.persistAndFlush(aberto1);

        Ticket aberto2 = new Ticket();
        aberto2.setTitulo("Sugestão de melhoria");
        aberto2.setDescricao("Adicionar filtro por autor");
        aberto2.setCategoria(Categoria.SUGESTAO);
        aberto2.setPrioridade(Prioridade.BAIXA);
        aberto2.setStatus("ABERTO");
        entityManager.persistAndFlush(aberto2);

        Ticket concluido = new Ticket();
        concluido.setTitulo("Dúvida sobre empréstimo");
        concluido.setDescricao("Como renovar?");
        concluido.setCategoria(Categoria.DUVIDA);
        concluido.setPrioridade(Prioridade.MEDIA);
        concluido.setStatus("CONCLUIDO");
        concluido.setResposta("Clique em renovar.");
        entityManager.persistAndFlush(concluido);
    }

    @Test
    @DisplayName("findByStatus — deve retornar todos os tickets ABERTOS")
    void findByStatus_DeveRetornarTicketsAbertos() {
        List<Ticket> resultado = ticketRepository.findByStatus("ABERTO");
        assertThat(resultado).hasSize(2);
        assertThat(resultado).allSatisfy(t -> assertThat(t.getStatus()).isEqualTo("ABERTO"));
    }

    @Test
    @DisplayName("findByStatus — deve retornar apenas tickets CONCLUIDOS")
    void findByStatus_DeveRetornarTicketsConcluidos() {
        List<Ticket> resultado = ticketRepository.findByStatus("CONCLUIDO");
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getTitulo()).isEqualTo("Dúvida sobre empréstimo");
    }

    @Test
    @DisplayName("findByStatus — deve retornar lista vazia para status inexistente")
    void findByStatus_DeveRetornarListaVazia() {
        List<Ticket> resultado = ticketRepository.findByStatus("CANCELADO");
        assertThat(resultado).isEmpty();
    }
}
