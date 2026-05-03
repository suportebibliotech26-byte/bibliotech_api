package com.impacta.biblioteca.service;

import com.impacta.biblioteca.model.Funcionario;
import com.impacta.biblioteca.model.enums.Perfil;
import com.impacta.biblioteca.repository.FuncionarioRepository;
import com.impacta.biblioteca.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final FuncionarioRepository funcionarioRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    // ─── Login ──────────────────────────────────────────────────

    public Map<String, Object> login(String email, String senha) {
        Funcionario funcionario = funcionarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("E-mail ou senha inválidos."));

        if (!funcionario.getAtivo()) {
            throw new IllegalArgumentException("Conta ainda não aprovada. Aguarde a ativação pelo suporte.");
        }

        if (funcionario.getPerfil() == null) {
            throw new IllegalArgumentException("Conta pendente de aprovação. Aguarde a definição do perfil.");
        }

        if (!passwordEncoder.matches(senha, funcionario.getSenha())) {
            throw new IllegalArgumentException("E-mail ou senha inválidos.");
        }

        String token = jwtService.gerarToken(funcionario);

        return Map.of(
                "token", token,
                "id", funcionario.getId(),
                "nome", funcionario.getNome(),
                "email", funcionario.getEmail(),
                "perfil", funcionario.getPerfil().name()
        );
    }

    // ─── Solicitar Acesso (cadastro pendente) ───────────────────

    public Funcionario solicitarAcesso(String nome, String email, String cpf, String username, String senha) {
        if (funcionarioRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Já existe um cadastro com este e-mail.");
        }
        if (funcionarioRepository.existsByCpf(cpf)) {
            throw new IllegalArgumentException("Já existe um cadastro com este CPF.");
        }
        if (funcionarioRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Já existe um cadastro com este username.");
        }

        Funcionario funcionario = new Funcionario();
        funcionario.setNome(nome);
        funcionario.setEmail(email);
        funcionario.setCpf(cpf);
        funcionario.setUsername(username);
        funcionario.setSenha(passwordEncoder.encode(senha));
        funcionario.setAtivo(false);
        funcionario.setPerfil(null); // Pendente até aprovação

        return funcionarioRepository.save(funcionario);
    }

    // ─── Aprovar cadastro (Admin/Suporte) ───────────────────────

    public Funcionario aprovar(Long id, Perfil perfil) {
        Funcionario funcionario = funcionarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado."));

        if (funcionario.getAtivo()) {
            throw new IllegalArgumentException("Este funcionário já está ativo.");
        }

        funcionario.setAtivo(true);
        funcionario.setPerfil(perfil != null ? perfil : Perfil.ATENDENTE);

        return funcionarioRepository.save(funcionario);
    }

    // ─── Esqueci minha senha (gera token de redefinição) ────────

    public String esqueciSenha(String email, String cpf) {
        Funcionario funcionario = funcionarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Dados não encontrados no sistema."));

        if (!funcionario.getCpf().equals(cpf)) {
            throw new IllegalArgumentException("Dados não encontrados no sistema.");
        }

        if (!funcionario.getAtivo()) {
            throw new IllegalArgumentException("Conta inativa. Entre em contato com o suporte.");
        }

        // Gera token JWT curto (15 minutos) para redefinição de senha
        return jwtService.gerarTokenRedefinicao(email);
    }

    // ─── Redefinir senha (com token) ────────────────────────────

    public void redefinirSenha(String token, String novaSenha) {
        if (!jwtService.tokenRedefinicaoValido(token)) {
            throw new IllegalArgumentException("Token inválido ou expirado. Solicite uma nova redefinição.");
        }

        String email = jwtService.extrairEmail(token);

        Funcionario funcionario = funcionarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado."));

        funcionario.setSenha(passwordEncoder.encode(novaSenha));
        funcionarioRepository.save(funcionario);
    }
}
