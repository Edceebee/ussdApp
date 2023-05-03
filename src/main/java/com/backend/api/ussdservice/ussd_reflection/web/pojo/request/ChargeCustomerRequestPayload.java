package com.backend.api.ussdservice.ussd_reflection.web.pojo.request;
import lombok.Data;

@Data
public class ChargeCustomerRequestPayload
{
    private String amount;
    private String customerAccountNumber;
    private String eventId;
    private String paymentReference;
    private String receiverName = "Waya Bank";
    private String senderName;
    private String tranCrncy = "NGN";
    private String tranNarration;
    private String transactionCategory = "FUNDING";
}
