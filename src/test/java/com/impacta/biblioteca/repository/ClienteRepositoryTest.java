package com.impacta.biblioteca.repository;

import com.impacta.biblioteca.model.Cliente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("ClienteRepository — Testes de Integração JPA")
class ClienteRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ClienteRepository clienteRepository;

    private Cliente cliente;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setNome("João Silva");
        cliente.setEmail("joao@email.com");
        cliente.setCpf("12345678900");
        cliente.setTelefone("11999999999");
        cliente.setStatusBloqueio("ATIVO");
        entityManager.persistAndFlush(cliente);
    }

    @Test
    @DisplayName("existsByEmail — deve retornar true quando e-mail existir")
    void existsByEmail_DeveRetornarTrue() {
        boolean resultado = clienteRepository.existsByEmail("joao@email.com");
        assertThat(resultado).isTrue();
    }

    @Test
    @DisplayName("existsByEmail — deve retornar false quando e-mail não existir")
    void existsByEmail_DeveRetornarFalse() {
        boolean resultado = clienteRepository.existsByEmail("naoexiste@email.com");
        assertThat(resultado).isFalse();
    }

    @Test
    @DisplayName("existsByCpf — deve retornar true quando CPF existir")
    void existsByCpf_DeveRetornarTrue() {
        boolean resultado = clienteRepository.existsByCpf("12345678900");
        assertThat(resultado).isTrue();
    }

    @Test
    @DisplayName("existsByCpf — deve retornar false quando CPF não existir")
    void existsByCpf_DeveRetornarFalse() {
        boolean resultado = clienteRepository.existsByCpf("00000000000");
        assertThat(resultado).isFalse();
    }
}
