package ru.otus.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.otus.dto.ClientDto;
import ru.otus.service.ClientService;
import ru.otus.testutils.DatabaseCleaner;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClientApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClientService clientService;

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
    }

    @Test
    void shouldGetClientById() throws Exception {
        // given
        ClientDto clientDto = new ClientDto();
        clientDto.setName("Мария Кузнецова");
        ClientDto savedClient = clientService.saveClient(clientDto);

        // when & then
        mockMvc.perform(get("/api/client/{id}", savedClient.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Мария Кузнецова"))
                .andExpect(jsonPath("$.id").value(savedClient.getId()));
    }

    @Test
    void shouldReturnNotFoundForNonExistentClient() throws Exception {
        mockMvc.perform(get("/api/client/9999")).andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateClient() throws Exception {
        String clientJson =
                """
            {
                "name": "Новый клиент",
                "address": "ул. Тестовая, 123",
                "phones": ["+7-999-888-77-66", "+7-999-888-77-67"]
            }
            """;

        mockMvc.perform(post("/api/client")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clientJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Новый клиент"))
                .andExpect(jsonPath("$.address").value("ул. Тестовая, 123"))
                .andExpect(jsonPath("$.phones").isArray())
                .andExpect(jsonPath("$.phones.length()").value(2));
    }

    @Test
    void shouldUpdateClient() throws Exception {
        // given
        ClientDto clientDto = new ClientDto();
        clientDto.setName("Старое имя");
        ClientDto savedClient = clientService.saveClient(clientDto);

        String updateJson =
                """
            {
                "name": "Обновленное имя",
                "address": "Новый адрес"
            }
            """;

        // when & then
        mockMvc.perform(put("/api/client/{id}", savedClient.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Обновленное имя"))
                .andExpect(jsonPath("$.address").value("Новый адрес"));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingNonExistentClient() throws Exception {
        String updateJson = """
        {
            "name": "Обновленное имя"
        }
        """;

        mockMvc.perform(put("/api/client/9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteClient() throws Exception {
        // given
        ClientDto clientDto = new ClientDto();
        clientDto.setName("Клиент для удаления");
        ClientDto savedClient = clientService.saveClient(clientDto);

        // when & then
        mockMvc.perform(delete("/api/client/{id}", savedClient.getId())).andExpect(status().isNoContent());

        // Проверяем, что клиент действительно удален
        mockMvc.perform(get("/api/client/{id}", savedClient.getId())).andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonExistentClient() throws Exception {
        mockMvc.perform(delete("/api/client/9999")).andExpect(status().isNotFound());
    }

    @Test
    void shouldGetAllClients() throws Exception {
        // given
        ClientDto client1 = new ClientDto();
        client1.setName("Клиент 1");
        clientService.saveClient(client1);

        ClientDto client2 = new ClientDto();
        client2.setName("Клиент 2");
        clientService.saveClient(client2);

        // when & then
        mockMvc.perform(get("/api/client"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }
}
