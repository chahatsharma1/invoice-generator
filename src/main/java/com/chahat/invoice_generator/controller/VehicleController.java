package com.chahat.invoice_generator.controller;

import com.chahat.invoice_generator.record.Vehicle;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api")
public class VehicleController {

    private static final List<Vehicle> VEHICLES = List.of(
            new Vehicle(101L, "Maruti", "Swift Dzire", 750000.00, 1L),
            new Vehicle(102L, "Hyundai", "Creta", 1200000.00, 2L),
            new Vehicle(103L, "Tata", "Nexon EV", 1500000.00, 3L),
            new Vehicle(104L, "Maruti", "Baleno", 850000.00, 1L)
    );

    @GetMapping("/vehicles")
    public List<Vehicle> getAllVehicles() {
        return VEHICLES;
    }
}