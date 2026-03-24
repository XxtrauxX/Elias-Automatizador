package com.elias.automatizador.controller;

import com.elias.automatizador.model.Contacto;
import com.elias.automatizador.repository.ContactoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/contactos")
@RequiredArgsConstructor
public class AdminApiController {

    private final ContactoRepository contactoRepository;

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarContacto(@PathVariable Long id, @RequestBody ContactoUpdateDTO request) {
        Optional<Contacto> contactoOpt = contactoRepository.findById(id);
        if (contactoOpt.isPresent()) {
            Contacto contacto = contactoOpt.get();
            contacto.setNombreEmpresa(request.getNombreEmpresa());
            contacto.setCorreo(request.getCorreo());
            contacto.setFechaActualizacion(LocalDateTime.now());
            contactoRepository.save(contacto);
            return ResponseEntity.ok().body("{\"message\": \"Contacto actualizado correctamente\"}");
        }
        return ResponseEntity.notFound().build();
    }

    public static class ContactoUpdateDTO {
        private String nombreEmpresa;
        private String correo;

        public String getNombreEmpresa() { return nombreEmpresa; }
        public void setNombreEmpresa(String nombreEmpresa) { this.nombreEmpresa = nombreEmpresa; }

        public String getCorreo() { return correo; }
        public void setCorreo(String correo) { this.correo = correo; }
    }
}
