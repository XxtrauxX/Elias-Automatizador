package com.elias.automatizador.controller;

import com.elias.automatizador.model.Contacto;
import com.elias.automatizador.repository.ContactoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class AdminDashboardController {

    private final ContactoRepository contactoRepository;

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        List<Contacto> contactos = contactoRepository.findAll();
        long totalClientes = contactoRepository.count();

        model.addAttribute("contactos", contactos);
        model.addAttribute("totalClientes", totalClientes);
        // "Mora" count logic could be sophisticated, but we'll use a placeholder or derived info
        model.addAttribute("totalMora", contactos.stream().filter(c -> c.getUltimaGestion() != null).count());

        return "admin-dashboard";
    }
}
