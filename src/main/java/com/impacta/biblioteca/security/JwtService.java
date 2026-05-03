package com.impacta.biblioteca.security;

import com.impacta.biblioteca.model.Funcionario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    // ─── Gerar token de login (24h) ─────────────────────────────

    public String gerarToken(UserDetails userDetails) {
        return gerarToken(Map.of(), userDetails, expiration);
    }

    // ─── Gerar token de redefinição de senha (15 min) ───────────

    public String gerarTokenRedefinicao(String email) {
        return Jwts.builder()
                .subject(email)
                .claim("tipo", "REDEFINICAO_SENHA")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 15 * 60 * 1000)) // 15 minutos
                .signWith(getChave())
                .compact();
    }

    // ─── Gerar token genérico ───────────────────────────────────
    // Usa o EMAIL como subject do JWT (não o username),
    // pois o filtro busca por email no banco de dados.

    public String gerarToken(Map<String, Object> claimsExtras, UserDetails userDetails, long tempoExpiracao) {
        // Extrai o email do Funcionario para usar como subject do token
        String subject = (userDetails instanceof Funcionario)
                ? ((Funcionario) userDetails).getEmail()
                : userDetails.getUsername();

        return Jwts.builder()
                .claims(claimsExtras)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + tempoExpiracao))
                .signWith(getChave())
                .compact();
    }

    // ─── Extrair e-mail (subject) do token ──────────────────────

    public String extrairEmail(String token) {
        return extrairClaim(token, Claims::getSubject);
    }

    // ─── Extrair claim "tipo" do token ──────────────────────────

    public String extrairTipo(String token) {
        return extrairClaim(token, claims -> claims.get("tipo", String.class));
    }

    // ─── Verificar se o token é válido ──────────────────────────

    public boolean tokenValido(String token, UserDetails userDetails) {
        final String emailDoToken = extrairEmail(token);
        // Compara com o email do Funcionario (não com getUsername() que retorna o campo 'username')
        String emailDoUsuario = (userDetails instanceof Funcionario)
                ? ((Funcionario) userDetails).getEmail()
                : userDetails.getUsername();
        return emailDoToken.equals(emailDoUsuario) && !tokenExpirado(token);
    }

    // ─── Verificar se o token de redefinição é válido ───────────

    public boolean tokenRedefinicaoValido(String token) {
        try {
            String tipo = extrairTipo(token);
            return "REDEFINICAO_SENHA".equals(tipo) && !tokenExpirado(token);
        } catch (Exception e) {
            return false;
        }
    }

    // ─── Helpers privados ───────────────────────────────────────

    private boolean tokenExpirado(String token) {
        return extrairExpiracao(token).before(new Date());
    }

    private Date extrairExpiracao(String token) {
        return extrairClaim(token, Claims::getExpiration);
    }

    private <T> T extrairClaim(String token, Function<Claims, T> resolver) {
        final Claims claims = extrairTodosClaims(token);
        return resolver.apply(claims);
    }

    private Claims extrairTodosClaims(String token) {
        return Jwts.parser()
                .verifyWith(getChave())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getChave() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
