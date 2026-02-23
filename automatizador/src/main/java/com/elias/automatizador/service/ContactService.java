package com.elias.automatizador.service;

import com.elias.automatizador.model.Contacto;
import com.elias.automatizador.model.dto.SiigoCustomerDTO;
import com.elias.automatizador.repository.ContactoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactoRepository contactoRepository;
    private final SiigoClient siigoClient;

    public Contacto obtenerContactoHibrido(String nit) {
        // 1. Buscar primero en nuestra base de datos local (MySQL)
        return contactoRepository.findByNit(nit)
            .orElseGet(() -> {
                System.out.println("NIT " + nit + " no está en DB. Consultando a Siigo...");
                
                // 2. Si no existe localmente, pedirlo a Siigo
                SiigoCustomerDTO siigoData = siigoClient.buscarClientePorNit(nit);
                
                if (siigoData != null) {
                    // 3. Guardar en MySQL para la próxima vez
                    return guardarNuevoContacto(siigoData);
                }
                return null;
            });
    }

    private Contacto guardarNuevoContacto(SiigoCustomerDTO dto) {
        Contacto nuevo = new Contacto();
        nuevo.setNit(dto.getIdentification());
        
        // Extraemos el nombre de la lista de Siigo
        String nombre = (dto.getName() != null && !dto.getName().isEmpty()) 
                        ? dto.getName().get(0) : "Empresa sin nombre";
        nuevo.setNombreEmpresa(nombre);

        // Extraemos Celular y Correo del primer contacto de la lista
        if (dto.getContacts() != null && !dto.getContacts().isEmpty()) {
            var principal = dto.getContacts().get(0);
            nuevo.setCorreo(principal.getEmail());
            if (principal.getPhone() != null) {
                nuevo.setCelular(principal.getPhone().getNumber());
            }
        }

        nuevo.setFechaActualizacion(LocalDateTime.now());
        System.out.println("💾 Guardando en MySQL: " + nuevo.getNombreEmpresa());
        return contactoRepository.save(nuevo);
    }
}