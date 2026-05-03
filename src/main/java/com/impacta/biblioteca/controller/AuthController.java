package com.impacta.biblioteca.controller;

import com.impacta.biblioteca.model.Funcionario;
import com.impacta.biblioteca.model.enums.Perfil;
import com.impacta.biblioteca.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    // ─── POST /api/auth/login ───────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            String senha = body.get("senha");

            if (email == null || senha == null) {
                return ResponseEntity.badRequest().body(Map.of("erro", "E-mail e senha são obrigatórios."));
            }

            Map<String, Object> resposta = authService.login(email, senha);
            return ResponseEntity.ok(resposta);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("erro", e.getMessage()));
        }
    }

    // ─── POST /api/auth/solicitar-acesso ────────────────────────

    @PostMapping("/solicitar-acesso")
    public ResponseEntity<?> solicitarAcesso(@RequestBody Map<String, String> body) {
        try {
            String nome = body.get("nome");
            String email = body.get("email");
            String cpf = body.get("cpf");
            String username = body.get("username");
            String senha = body.get("senha");

            if (nome == null || email == null || cpf == null || username == null || senha == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("erro", "Todos os campos são obrigatórios: nome, email, cpf, username, senha."));
            }

            Funcionario funcionario = authService.solicitarAcesso(nome, email, cpf, username, senha);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "id", funcionario.getId(),
                    "nome", funcionario.getNome(),
                    "email", funcionario.getEmail(),
                    "mensagem", "Solicitação criada com sucesso. Aguarde a aprovação do suporte."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    // ─── PATCH /api/auth/aprovar/{id} ───────────────────────────

    @PatchMapping("/aprovar/{id}")
    public ResponseEntity<?> aprovar(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        try {
            Perfil perfil = null;
            if (body != null && body.get("perfil") != null) {
                try {
                    perfil = Perfil.valueOf(body.get("perfil").toUpperCase());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("erro", "Perfil inválido. Use: ADMIN, SUPORTE ou ATENDENTE."));
                }
            }

            Funcionario funcionario = authService.aprovar(id, perfil);
            return ResponseEntity.ok(funcionario);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    // ─── POST /api/auth/esqueci-senha ───────────────────────────

    @PostMapping("/esqueci-senha")
    public ResponseEntity<?> esqueciSenha(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            String cpf = body.get("cpf");

            if (email == null || cpf == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("erro", "E-mail e CPF são obrigatórios."));
            }

            String token = authService.esqueciSenha(email, cpf);
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "mensagem", "Token de redefinição gerado. Válido por 15 minutos."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    // ─── POST /api/auth/redefinir-senha ─────────────────────────

    @PostMapping("/redefinir-senha")
    public ResponseEntity<?> redefinirSenha(@RequestBody Map<String, String> body) {
        try {
            String token = body.get("token");
            String novaSenha = body.get("novaSenha");

            if (token == null || novaSenha == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("erro", "Token e nova senha são obrigatórios."));
            }

            authService.redefinirSenha(token, novaSenha);
            return ResponseEntity.ok(Map.of("mensagem", "Senha redefinida com sucesso."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    // ─── GET /api/auth/me ───────────────────────────────────────

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal Funcionario funcionario) {
        if (funcionario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("erro", "Não autenticado."));
        }

        return ResponseEntity.ok(Map.of(
                "id", funcionario.getId(),
                "nome", funcionario.getNome(),
                "email", funcionario.getEmail(),
                "username", funcionario.getUsername(),
                "perfil", funcionario.getPerfil() != null ? funcionario.getPerfil().name() : "PENDENTE",
                "ativo", funcionario.getAtivo()
        ));
    }
}
