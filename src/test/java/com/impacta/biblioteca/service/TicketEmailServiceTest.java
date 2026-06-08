package com.impacta.biblioteca.service;

import com.impacta.biblioteca.model.Ticket;
import com.impacta.biblioteca.model.enums.Categoria;
import com.impacta.biblioteca.model.enums.Prioridade;
import com.impacta.biblioteca.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketEmailService — Testes Unitários")
class TicketEmailServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private RestTemplate restTemplate;

    private TicketEmailService ticketEmailService;

    @BeforeEach
    void setUp() {
        ticketEmailService = new TicketEmailService(ticketRepository);
        // Injeta o RestTemplate mockado via reflection (campo privado)
        ReflectionTestUtils.setField(ticketEmailService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(ticketEmailService, "resendApiKey", "test-api-key");
        ReflectionTestUtils.setField(ticketEmailService, "fromEmail", "BiblioTech <onboarding@resend.dev>");
    }

    @Nested
    @DisplayName("enviarRelatorioTicketsAbertos()")
    class EnviarRelatorio {

        @Test
        @DisplayName("Deve enviar relatório com sucesso quando existem tickets abertos")
        void deveEnviarRelatorioComTicketsAbertos() {
            // Arrange
            Ticket ticket = new Ticket();
            ticket.setId(1L);
            ticket.setTitulo("Bug no Login");
            ticket.setDescricao("Erro ao logar");
            ticket.setCategoria(Categoria.BUG);
            ticket.setPrioridade(Prioridade.ALTA);
            ticket.setStatus("ABERTO");
            ticket.setDataCriacao(LocalDateTime.now());

            when(ticketRepository.findByStatus("ABERTO")).thenReturn(List.of(ticket));
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(new ResponseEntity<>("OK", HttpStatus.OK));

            // Act
            ticketEmailService.enviarRelatorioTicketsAbertos("destino@email.com");

            // Assert
            verify(restTemplate).postForEntity(eq("https://api.resend.com/emails"), any(HttpEntity.class), eq(String.class));
        }

        @Test
        @DisplayName("Deve enviar relatório mesmo sem tickets abertos")
        void deveEnviarRelatorioSemTickets() {
            // Arrange
            when(ticketRepository.findByStatus("ABERTO")).thenReturn(List.of());
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(new ResponseEntity<>("OK", HttpStatus.OK));

            // Act
            ticketEmailService.enviarRelatorioTicketsAbertos("destino@email.com");

            // Assert
            verify(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
        }

        @Test
        @DisplayName("Deve lançar exceção quando API Resend retornar erro")
        void deveLancarExcecaoQuandoApiRetornaErro() {
            // Arrange
            when(ticketRepository.findByStatus("ABERTO")).thenReturn(List.of());
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(new ResponseEntity<>("Erro", HttpStatus.INTERNAL_SERVER_ERROR));

            // Act & Assert
            assertThatThrownBy(() -> ticketEmailService.enviarRelatorioTicketsAbertos("destino@email.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Erro ao enviar email via Resend");
        }
    }
}
