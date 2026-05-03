package com.impacta.biblioteca.service;

import com.impacta.biblioteca.model.Funcionario;
import com.impacta.biblioteca.repository.FuncionarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FuncionarioService {

    private final FuncionarioRepository funcionarioRepository;
    private final PasswordEncoder passwordEncoder;

    // ─── Listar todos os funcionários ────────────────────────────

    public List<Funcionario> listarTodos() {
        return funcionarioRepository.findAll();
    }

    // ─── Buscar funcionário por ID ───────────────────────────────

    public Optional<Funcionario> buscarPorId(Long id) {
        return funcionarioRepository.findById(id);
    }

    // ─── Criar novo funcionário ──────────────────────────────────

    public Funcionario criar(Funcionario funcionario) {
        if (funcionarioRepository.existsByEmail(funcionario.getEmail())) {
            throw new IllegalArgumentException("Já existe um funcionário com este e-mail.");
        }
        if (funcionarioRepository.existsByCpf(funcionario.getCpf())) {
            throw new IllegalArgumentException("Já existe um funcionário com este CPF.");
        }
        if (funcionarioRepository.existsByUsername(funcionario.getUsername())) {
            throw new IllegalArgumentException("Já existe um funcionário com este nome de usuário (username).");
        }

        funcionario.setId(null); // Garante que será um novo registro
        // Criptografa a senha se fornecida em texto plano
        if (funcionario.getSenha() != null && !funcionario.getSenha().startsWith("$2a$")) {
            funcionario.setSenha(passwordEncoder.encode(funcionario.getSenha()));
        }
        return funcionarioRepository.save(funcionario);
    }

    // ─── Editar funcionário por ID ───────────────────────────────

    public Optional<Funcionario> editar(Long id, Funcionario funcionarioAtualizado) {
        return funcionarioRepository.findById(id)
                .map(funcionarioExistente -> {
                    if (!funcionarioExistente.getEmail().equals(funcionarioAtualizado.getEmail()) &&
                            funcionarioRepository.existsByEmail(funcionarioAtualizado.getEmail())) {
                        throw new IllegalArgumentException("Já existe um funcionário com este e-mail.");
                    }
                    if (!funcionarioExistente.getCpf().equals(funcionarioAtualizado.getCpf()) &&
                            funcionarioRepository.existsByCpf(funcionarioAtualizado.getCpf())) {
                        throw new IllegalArgumentException("Já existe um funcionário com este CPF.");
                    }
                    if (!funcionarioExistente.getUsername().equals(funcionarioAtualizado.getUsername()) &&
                            funcionarioRepository.existsByUsername(funcionarioAtualizado.getUsername())) {
                        throw new IllegalArgumentException("Já existe um funcionário com este nome de usuário (username).");
                    }

                    funcionarioExistente.setNome(funcionarioAtualizado.getNome());
                    funcionarioExistente.setEmail(funcionarioAtualizado.getEmail());
                    funcionarioExistente.setCpf(funcionarioAtualizado.getCpf());
                    funcionarioExistente.setUsername(funcionarioAtualizado.getUsername());
                    funcionarioExistente.setAtivo(funcionarioAtualizado.getAtivo());
                    funcionarioExistente.setPerfil(funcionarioAtualizado.getPerfil());

                    // Atualiza senha apenas se fornecida
                    if (funcionarioAtualizado.getSenha() != null && !funcionarioAtualizado.getSenha().isBlank()) {
                        if (!funcionarioAtualizado.getSenha().startsWith("$2a$")) {
                            funcionarioExistente.setSenha(passwordEncoder.encode(funcionarioAtualizado.getSenha()));
                        } else {
                            funcionarioExistente.setSenha(funcionarioAtualizado.getSenha());
                        }
                    }

                    return funcionarioRepository.save(funcionarioExistente);
                });
    }

    // ─── Deletar funcionário por ID ──────────────────────────────

    public boolean deletar(Long id) {
        return funcionarioRepository.findById(id)
                .map(funcionario -> {
                    funcionarioRepository.delete(funcionario);
                    return true;
                })
                .orElse(false);
    }
}
