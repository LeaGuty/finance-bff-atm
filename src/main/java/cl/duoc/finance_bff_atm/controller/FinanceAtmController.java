package cl.duoc.finance_bff_atm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.finance_bff_atm.model.ResumenCajeroDTO;
import cl.duoc.finance_bff_atm.service.FinanceAtmService;

@RestController
@RequestMapping("/bff/atm/v1")
public class FinanceAtmController {

    @Autowired
    private FinanceAtmService financeAtmService;

    @GetMapping("/saldo/{id}")
    public ResponseEntity<ResumenCajeroDTO> consultarSaldo(@PathVariable Long id) {
        ResumenCajeroDTO respuesta = financeAtmService.consultarSaldoSeguro(id);
        return ResponseEntity.ok(respuesta);
    }
}