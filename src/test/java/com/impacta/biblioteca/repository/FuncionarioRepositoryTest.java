package com.impacta.biblioteca.repository;

import com.impacta.biblioteca.model.Funcionario;
import com.impacta.biblioteca.model.enums.Perfil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("FuncionarioRepository — Testes de Integração JPA")
class FuncionarioRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    private Funcionario funcionario;

    @BeforeEach
    void setUp() {
        funcionario = new Funcionario();
        funcionario.setNome("Admin");
        funcionario.setEmail("admin@biblioteca.com");
        funcionario.setCpf("11111111111");
        funcionario.setUsername("admin");
        funcionario.setSenha("$2a$10$hashedPassword");
        funcionario.setAtivo(true);
        funcionario.setPerfil(Perfil.ADMIN);
        entityManager.persistAndFlush(funcionario);
    }

    @Test
    @DisplayName("existsByEmail — deve retornar true quando e-mail existir")
    void existsByEmail_DeveRetornarTrue() {
        assertThat(funcionarioRepository.existsByEmail("admin@biblioteca.com")).isTrue();
    }

    @Test
    @DisplayName("existsByEmail — deve retornar false quando e-mail não existir")
    void existsByEmail_DeveRetornarFalse() {
        assertThat(funcionarioRepository.existsByEmail("naoexiste@email.com")).isFalse();
    }

    @Test
    @DisplayName("existsByCpf — deve retornar true quando CPF existir")
    void existsByCpf_DeveRetornarTrue() {
        assertThat(funcionarioRepository.existsByCpf("11111111111")).isTrue();
    }

    @Test
    @DisplayName("existsByCpf — deve retornar false quando CPF não existir")
    void existsByCpf_DeveRetornarFalse() {
        assertThat(funcionarioRepository.existsByCpf("00000000000")).isFalse();
    }

    @Test
    @DisplayName("existsByUsername — deve retornar true quando username existir")
    void existsByUsername_DeveRetornarTrue() {
        assertThat(funcionarioRepository.existsByUsername("admin")).isTrue();
    }

    @Test
    @DisplayName("existsByUsername — deve retornar false quando username não existir")
    void existsByUsername_DeveRetornarFalse() {
        assertThat(funcionarioRepository.existsByUsername("naoexiste")).isFalse();
    }

    @Test
    @DisplayName("findByEmail — deve retornar funcionário quando e-mail existir")
    void findByEmail_DeveRetornarFuncionario() {
        Optional<Funcionario> resultado = funcionarioRepository.findByEmail("admin@biblioteca.com");
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNome()).isEqualTo("Admin");
    }

    @Test
    @DisplayName("findByEmail — deve retornar vazio quando e-mail não existir")
    void findByEmail_DeveRetornarVazio() {
        Optional<Funcionario> resultado = funcionarioRepository.findByEmail("naoexiste@email.com");
        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("findByUsername — deve retornar funcionário quando username existir")
    void findByUsername_DeveRetornarFuncionario() {
        Optional<Funcionario> resultado = funcionarioRepository.findByUsername("admin");
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getEmail()).isEqualTo("admin@biblioteca.com");
    }

    @Test
    @DisplayName("findByUsername — deve retornar vazio quando username não existir")
    void findByUsername_DeveRetornarVazio() {
        Optional<Funcionario> resultado = funcionarioRepository.findByUsername("naoexiste");
        assertThat(resultado).isEmpty();
    }
}
