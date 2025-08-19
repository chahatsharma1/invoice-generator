package com.chahat.invoice_generator.request;

import lombok.Data;

@Data
public class InvoiceRequest {
    private Long dealerId;
    private Long vehicleId;
    private String customerName;
}