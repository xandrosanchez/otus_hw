package ru.otus.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.dto.ClientDto;
import ru.otus.model.Address;
import ru.otus.model.Client;
import ru.otus.model.Phone;
import ru.otus.repository.ClientRepository;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public List<ClientDto> getAllClients() {
        List<Client> clients = (List<Client>) clientRepository.findAll();
        return clients.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<ClientDto> getClientById(Long id) {
        return clientRepository.findById(id).map(this::toDto);
    }

    @Transactional
    public ClientDto saveClient(ClientDto clientDto) {
        Client client;

        if (clientDto.getId() != null) {
            // Обновление существующего клиента
            Client existingClient = clientRepository
                    .findById(clientDto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Client not found with id: " + clientDto.getId()));
            client = updateEntity(existingClient, clientDto);
        } else {
            // Создание нового клиента
            client = toEntity(clientDto);
        }

        Client savedClient = clientRepository.save(client);
        return toDto(savedClient);
    }

    @Transactional
    public void deleteClient(Long id) {
        clientRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<ClientDto> searchClients(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllClients();
        }

        List<Client> clients = clientRepository.findByNameContainingIgnoreCase(searchTerm);
        return clients.stream().map(this::toDto).collect(Collectors.toList());
    }

    private ClientDto toDto(Client client) {
        ClientDto dto = new ClientDto(client.getId(), client.getName());

        if (client.getAddress() != null) {
            dto.setAddress(client.getAddress().street());
        }

        List<String> phoneNumbers =
                client.getPhones().stream().map(Phone::number).collect(Collectors.toList());
        dto.setPhones(phoneNumbers);

        return dto;
    }

    private Client toEntity(ClientDto clientDto) {
        Client client = new Client(clientDto.getName());

        // Адрес (только для нового клиента)
        if (clientDto.getAddress() != null && !clientDto.getAddress().trim().isEmpty()) {
            // Для нового клиента ID будет null
            Address address = new Address(clientDto.getAddress().trim(), null);
            client.setAddress(address);
        }

        // Телефоны (только для нового клиента)
        if (clientDto.getPhones() != null && !clientDto.getPhones().isEmpty()) {
            var phones = clientDto.getPhones().stream()
                    .filter(phone -> phone != null && !phone.trim().isEmpty())
                    .map(phone -> new Phone(phone.trim(), null)) // clientId будет установлен автоматически
                    .collect(Collectors.toSet());
            client.setPhones(phones);
        }

        return client;
    }

    private Client updateEntity(Client existingClient, ClientDto clientDto) {
        // Обновляем основные поля
        existingClient.setName(clientDto.getName());

        // Обновляем адрес
        if (clientDto.getAddress() != null && !clientDto.getAddress().trim().isEmpty()) {
            Address newAddress = new Address(clientDto.getAddress().trim(), existingClient.getId());
            existingClient.setAddress(newAddress);
        } else {
            existingClient.setAddress(null);
        }

        // Обновляем телефоны (полная замена)
        if (clientDto.getPhones() != null && !clientDto.getPhones().isEmpty()) {
            var newPhones = clientDto.getPhones().stream()
                    .filter(phone -> phone != null && !phone.trim().isEmpty())
                    .map(phone -> new Phone(phone.trim(), existingClient.getId()))
                    .collect(Collectors.toSet());
            existingClient.setPhones(newPhones);
        } else {
            existingClient.setPhones(new HashSet<>());
        }

        return existingClient;
    }
}
