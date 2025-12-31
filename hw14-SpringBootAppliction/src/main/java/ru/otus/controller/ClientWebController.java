package ru.otus.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.otus.dto.ClientDto;
import ru.otus.service.ClientService;

@Controller
@RequestMapping("/clients")
public class ClientWebController {

    private final ClientService clientService;

    public ClientWebController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    public String showClients(@RequestParam(required = false) String search, Model model) {

        if (search != null && !search.trim().isEmpty()) {
            model.addAttribute("clients", clientService.searchClients(search));
        } else {
            model.addAttribute("clients", clientService.getAllClients());
        }
        return "clients/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("client", new ClientDto());
        return "clients/form";
    }

    @PostMapping
    public String saveClient(@ModelAttribute ClientDto client) {
        clientService.saveClient(client);
        return "redirect:/clients";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        ClientDto client = clientService
                .getClientById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid client Id:" + id));
        model.addAttribute("client", client);
        return "clients/form";
    }

    @GetMapping("/delete/{id}")
    public String deleteClient(@PathVariable Long id) {
        clientService.deleteClient(id);
        return "redirect:/clients";
    }
}
