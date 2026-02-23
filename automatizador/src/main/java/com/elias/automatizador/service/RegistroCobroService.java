package com.elias.automatizador.service;

import com.elias.automatizador.model.RegistroCobro;
import com.elias.automatizador.repository.RegistroCobroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class RegistroCobroService {

    private final RegistroCobroRepository repository;

    @Transactional
    public void registrarCobro(String nit, String nombre, BigDecimal monto, String canal, String estado) {
        RegistroCobro registro = new RegistroCobro();
        registro.setNit(nit);
        registro.setNombreCliente(nombre);
        registro.setMontoDeuda(monto);
        registro.setCanal(canal);
        registro.setEstado(estado);
        repository.save(registro);
        System.out.println("💾 Log guardado en MySQL para NIT: " + nit);
    }
}
