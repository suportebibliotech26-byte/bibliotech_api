package com.impacta.biblioteca.repository;

import com.impacta.biblioteca.model.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {
    boolean existsByEmail(String email);
    boolean existsByCpf(String cpf);
    boolean existsByUsername(String username);
    Optional<Funcionario> findByEmail(String email);
    Optional<Funcionario> findByUsername(String username);
}
