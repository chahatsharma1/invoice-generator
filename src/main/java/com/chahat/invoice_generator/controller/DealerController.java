package com.chahat.invoice_generator.controller;

import com.chahat.invoice_generator.record.Dealer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api")
public class DealerController {

    private static final List<Dealer> DEALERS = List.of(
            new Dealer(1L, "Maruti Suzuki Arena", "Connaught Place, New Delhi", "011-23456789"),
            new Dealer(2L, "Hyundai Motors India", "Anna Salai, Chennai", "044-22334455"),
            new Dealer(3L, "Tata Motors Showroom", "FC Road, Pune", "020-33445566")
    );

    @GetMapping("/dealers")
    public List<Dealer> getAllDealers() {
        return DEALERS;
    }
}