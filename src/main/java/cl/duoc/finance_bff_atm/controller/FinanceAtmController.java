package cl.duoc.finance_bff_atm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.finance_bff_atm.model.ResumenCajeroDTO;
import cl.duoc.finance_bff_atm.service.FinanceAtmService;

/**
 * Controlador REST principal del BFF ATM.
 *
 * Expone los endpoints protegidos bajo /bff/atm/v1/ que solo pueden ser
 * accedidos por operadores autenticados con rol CAJERO_AUT.
 * Todos los endpoints requieren un token JWT valido en el header Authorization.
 *
 * Este controlador delega la logica de negocio al servicio FinanceAtmService,
 * que se encarga de orquestar las llamadas al backend, enmascarar datos
 * sensibles y construir la respuesta para el cajero.
 *
 * @author Duoc UC - Backend 3
 */
@RestController
@RequestMapping("/bff/atm/v1")
public class FinanceAtmController {

    /** Servicio que orquesta las operaciones del cajero ATM */
    @Autowired
    private FinanceAtmService financeAtmService;

    /**
     * Consulta el saldo de una cuenta y retorna un resumen seguro para el cajero.
     *
     * El resumen incluye:
     * - Nombre del cliente enmascarado (ej: "J*** P***")
     * - Saldo actual de la cuenta
     * - Ultimos 3 movimientos (para impresion de voucher)
     * - Mensaje del sistema (exito o error)
     *
     * @param id identificador de la cuenta a consultar
     * @return ResumenCajeroDTO con la informacion formateada para el ATM
     */
    @GetMapping("/saldo/{id}")
    public ResponseEntity<ResumenCajeroDTO> consultarSaldo(@PathVariable Long id) {
        ResumenCajeroDTO respuesta = financeAtmService.consultarSaldoSeguro(id);
        return ResponseEntity.ok(respuesta);
    }
}
