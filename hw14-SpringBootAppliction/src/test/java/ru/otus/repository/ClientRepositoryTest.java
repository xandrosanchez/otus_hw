package ru.otus.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.otus.model.Client;
import ru.otus.testutils.DatabaseCleaner;

@SpringBootTest
@ActiveProfiles("test")
class ClientRepositoryTest {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
    }

    @Test
    void shouldSaveAndFindClient() {
        // given
        Client client = new Client("Тестовый клиент");

        // when
        Client saved = clientRepository.save(client);
        Optional<Client> found = clientRepository.findById(saved.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Тестовый клиент");
        assertThat(found.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    void shouldFindByNameContaining() {
        // given
        Client client1 = new Client("Иван Иванов");
        Client client2 = new Client("Петр Петров");
        clientRepository.save(client1);
        clientRepository.save(client2);

        // when
        var found = clientRepository.findByNameContainingIgnoreCase("Иван");

        // then
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getName()).isEqualTo("Иван Иванов");
    }
}
