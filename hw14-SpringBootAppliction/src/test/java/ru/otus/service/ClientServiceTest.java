package ru.otus.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.otus.dto.ClientDto;
import ru.otus.testutils.DatabaseCleaner;

@SpringBootTest
@ActiveProfiles("test")
class ClientServiceTest {

    @Autowired
    private ClientService clientService;

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
    }

    @Test
    void shouldCreateClientWithAddressAndPhones() {
        // given
        ClientDto clientDto = new ClientDto();
        clientDto.setName("Анна Сидорова");
        clientDto.setAddress("ул. Ленина, 10");
        clientDto.addPhone("+7-999-111-11-11");
        clientDto.addPhone("+7-999-111-11-12");

        // when
        ClientDto saved = clientService.saveClient(clientDto);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Анна Сидорова");
        assertThat(saved.getAddress()).isEqualTo("ул. Ленина, 10");
        assertThat(saved.getPhones()).hasSize(2);
    }

    @Test
    void shouldFindClientById() {
        // given
        ClientDto clientDto = new ClientDto();
        clientDto.setName("Петр Иванов");
        ClientDto saved = clientService.saveClient(clientDto);

        // when
        var found = clientService.getClientById(saved.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Петр Иванов");
        assertThat(found.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    void shouldReturnEmptyWhenClientNotFound() {
        // when
        var found = clientService.getClientById(9999L);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    void shouldGetAllClients() {
        // given
        ClientDto client1 = new ClientDto();
        client1.setName("Клиент 1");
        clientService.saveClient(client1);

        ClientDto client2 = new ClientDto();
        client2.setName("Клиент 2");
        clientService.saveClient(client2);

        // when
        List<ClientDto> clients = clientService.getAllClients();

        // then
        assertThat(clients).hasSizeGreaterThanOrEqualTo(2);
        assertThat(clients).extracting(ClientDto::getName).contains("Клиент 1", "Клиент 2");
    }

    @Test
    void shouldUpdateClientRemoveAddressAndPhones() {
        // given - создаем клиента с адресом и телефонами
        ClientDto clientDto = new ClientDto();
        clientDto.setName("Клиент с данными");
        clientDto.setAddress("Старый адрес");
        clientDto.addPhone("+7-777-777-77-77");
        ClientDto saved = clientService.saveClient(clientDto);

        // when - обновляем без адреса и телефонов
        saved.setAddress(null);
        saved.setPhones(new ArrayList<>());
        ClientDto updated = clientService.saveClient(saved);

        // then
        assertThat(updated.getId()).isEqualTo(saved.getId());
        assertThat(updated.getName()).isEqualTo("Клиент с данными");
        assertThat(updated.getAddress()).isNull();
        assertThat(updated.getPhones()).isEmpty();
    }

    @Test
    void shouldDeleteClient() {
        // given
        ClientDto clientDto = new ClientDto();
        clientDto.setName("Клиент для удаления");
        ClientDto saved = clientService.saveClient(clientDto);

        // when
        clientService.deleteClient(saved.getId());

        // then
        var found = clientService.getClientById(saved.getId());
        assertThat(found).isEmpty();
    }

    @Test
    void shouldSearchClientsByName() {
        // given
        ClientDto client1 = new ClientDto();
        client1.setName("Иван Петров");
        clientService.saveClient(client1);

        ClientDto client2 = new ClientDto();
        client2.setName("Петр Иванов");
        clientService.saveClient(client2);

        // when
        List<ClientDto> found = clientService.searchClients("Иван");

        // then
        assertThat(found).hasSizeGreaterThanOrEqualTo(1);
        assertThat(found).extracting(ClientDto::getName).anyMatch(name -> name.contains("Иван"));
    }
}
