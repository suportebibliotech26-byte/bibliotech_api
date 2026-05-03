package com.impacta.biblioteca.config;

import com.impacta.biblioteca.model.Funcionario;
import com.impacta.biblioteca.model.enums.Perfil;
import com.impacta.biblioteca.repository.FuncionarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final FuncionarioRepository funcionarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Se não há nenhum funcionário no banco, cria o admin padrão
        if (funcionarioRepository.count() == 0) {
            Funcionario admin = new Funcionario();
            admin.setNome("Administrador");
            admin.setEmail("admin@bibliotech.com");
            admin.setCpf("000.000.000-00");
            admin.setUsername("admin");
            admin.setSenha(passwordEncoder.encode("Admin123!"));
            admin.setAtivo(true);
            admin.setPerfil(Perfil.ADMIN);

            funcionarioRepository.save(admin);

            System.out.println("══════════════════════════════════════════════════");
            System.out.println("   Admin padrão criado com sucesso!");
            System.out.println("   E-mail:  admin@bibliotech.com");
            System.out.println("   Senha:   Admin123!");
            System.out.println("   Perfil:  ADMIN");
            System.out.println("══════════════════════════════════════════════════");
        }
    }
}
