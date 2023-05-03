package com.backend.api.ussdservice.ussd_reflection.menuHandlers;

import com.backend.api.ussdservice.ussd_reflection.web.pojo.response.*;
import com.google.gson.Gson;
import com.backend.api.ussdservice.pojo.UssdMessageAndData;
import com.backend.api.ussdservice.ussd_reflection.annotation.UssdMenuHandler;
import com.backend.api.ussdservice.ussd_reflection.annotation.UssdSubMenuHandler;
import com.backend.api.ussdservice.ussd_reflection.constants.ChargeAmounts;
import com.backend.api.ussdservice.ussd_reflection.constants.ChargeEventId;
import com.backend.api.ussdservice.ussd_reflection.constants.CustomerTypes;
import com.backend.api.ussdservice.ussd_reflection.context.ContextManager;
import com.backend.api.ussdservice.ussd_reflection.context.ussd.UssdContext;
import com.backend.api.ussdservice.ussd_reflection.context.ussd.message.MessageLineBuilder;
import com.backend.api.ussdservice.ussd_reflection.session.SessionManager;
import com.backend.api.ussdservice.ussd_reflection.templates.FundsTransferMenuTemplate;
import com.backend.api.ussdservice.ussd_reflection.templates.HomeMenuTemplate;
import com.backend.api.ussdservice.ussd_reflection.utils.Utils;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.dto.FundsTransferDTO;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.request.ChargeCustomerRequestPayload;
import com.backend.api.ussdservice.ussd_reflection.web.services.AccountService;
import com.backend.api.ussdservice.ussd_reflection.web.services.FundsTransferService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.backend.api.ussdservice.ussd_reflection.session.SessionManager.continueSessionMessage;
import static com.backend.api.ussdservice.ussd_reflection.session.SessionManager.endSessionMessage;


@Slf4j
@UssdMenuHandler
public class InternalFundsTransferMenuHandler
{

    @Autowired
    private FundsTransferService fundsTransferService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private ChargeEventId chargeEventId;


    private static final Gson gson = new Gson();
    private final FundsTransferMenuTemplate template = new FundsTransferMenuTemplate();
    private final HomeMenuTemplate homeMenuTemplate = new HomeMenuTemplate();

    @UssdSubMenuHandler("*1*2#")
    public String enterAmountForInternalFundScreen(UssdContext context){
        return continueSessionMessage(template.getEnterAmountScreen());
    }

    // User enter amount
    @UssdSubMenuHandler("*1*2*{amount}#")
    public String selectAccountForInternalFundsTransferFrom(UssdContext context, String amount){
        List<String> accountNumbers = new ArrayList<>();
        try{
            accountNumbers = accountService.getAccountNumbersForCustomer(context.getMobileNumber())
                    .getData().stream()
                    .map(AccountNumbersResponse::getAccountNo)
                    .collect(Collectors.toList());
        }catch (Exception e){
            SessionManager.clearSession(context.getSessionId());
            return endSessionMessage(new MessageLineBuilder()
                    .addLine("Sorry you don't have any account associated with your profile though you are a registered customer.")
                    .addLine("Please contact administrator or support.")
                    .addLine("Thank you.")
                    .toString());
        }

        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);;
        customerData.put("customerType", CustomerTypes.EXISTING.name());

        UssdMessageAndData messageAndData = template
                .getAccountNumberListUssdMessageAndDataFromCollection(accountNumbers, customerData, "Account");

        SessionManager.updateExtraDataOfSession(context.getSessionId(), messageAndData.getCustomerData());
        return continueSessionMessage(messageAndData.getMessage());
    }

    // User decides to go back to the main menu
    @UssdSubMenuHandler("*1*2*0#")
    public String goBackToMainMenuForInternalFunds(UssdContext context){
        SessionManager.clearSession(context.getSessionId());
        return continueSessionMessage(homeMenuTemplate.getExistingCustomerHomeMenuListScreen());
    }


    // User decides to enter beneficiary number or go back. The input here represents the number option
    // of the corresponding account.
    @UssdSubMenuHandler("*1*2*{amount}*{input}#")
    public String showEnterBeneficiaryAccountMsgForInternalFunds(UssdContext context, String amount, String input){
        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);

        // Check if the user enters 0. Then go back to the previous menu.
        if(ContextManager.isGoBackOption(input)){
            return continueSessionMessage(enterAmountForInternalFundScreen(context));
        }

        // Wrong input
        Object selectedAccount = customerData.get("Account".concat(input));
        if(selectedAccount == null){
            return endSessionMessage(template.getErrorMessage(context));
        }

        String selectedCustomerAccountNumber = String.valueOf(selectedAccount);
        customerData.put("selectedCustomerAccount", selectedCustomerAccountNumber);
        SessionManager.updateExtraDataOfSession(context.getSessionId(), customerData);

        return continueSessionMessage(template.getEnterBeneficiaryAccountNumber());
    }


    // Method for user enters beneficiary account number or wants to go back to the account numbers list.
    @UssdSubMenuHandler("*1*2*{amount}*{input}*{beneficiaryAccount}#")
    public String enterBeneficiaryAccountOrGoBackToAccountListForInternalFunds(UssdContext context, String amount, String input, String beneficiaryAccount){
// Get the customer details
        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);

        if(ContextManager.isGoBackOption(beneficiaryAccount)){
            List<String> accountNumbers = (List<String>) customerData.get("accountNumbers");
            UssdMessageAndData messageAndData = template
                    .getAccountNumberListUssdMessageAndDataFromCollection(accountNumbers, customerData, "Account");

            SessionManager.updateExtraDataOfSession(context.getSessionId(), messageAndData.getCustomerData());
            return continueSessionMessage(messageAndData.getMessage());
        }

        // Do internal name enquiry
        String beneficiaryBank = "WAYA MICROFINANCE BANK";
        NameEnquiryResponse nameEnquiryResponse = fundsTransferService
                .getBeneficiaryNameEnquiryInternal(beneficiaryAccount);

        InternalNameEnquiryResponseDTO internalNameEnquiryResponseDTO = fundsTransferService
                .getBeneficiaryNameEnquiryInternal2(beneficiaryAccount);

        // First check from their system
        if(internalNameEnquiryResponseDTO.isStatus()){
            String beneficiaryName = internalNameEnquiryResponseDTO.getData().getAccountName();
            customerData.put("beneficiaryName", beneficiaryName);
            customerData.put("beneficiaryBank", beneficiaryBank);
            customerData.put("beneficiaryAccount", beneficiaryAccount);
            SessionManager.updateExtraDataOfSession(context.getSessionId(), customerData);
            return template.getProceedToTransferScreen(beneficiaryBank, beneficiaryName, beneficiaryAccount);
        }

        // Check from the first normal NIP name enquiry.
        else if(nameEnquiryResponse != null &&
                nameEnquiryResponse.isStatus() &&
                !nameEnquiryResponse.getData().isEmpty() &&
                !nameEnquiryResponse.getData().isBlank())
        {
            String beneficiaryName = nameEnquiryResponse.getData();
            customerData.put("beneficiaryName", beneficiaryName);
            customerData.put("beneficiaryBank", beneficiaryBank);
            customerData.put("beneficiaryAccount", beneficiaryAccount);
            SessionManager.updateExtraDataOfSession(context.getSessionId(), customerData);
            return template.getProceedToTransferScreen(beneficiaryBank, beneficiaryName, beneficiaryAccount);
        }

        // Here name enquiry fails. Display error message and cancel session
        SessionManager.clearSession(context.getSessionId());
        return endSessionMessage(template.getNameInquiryFailureMessage(context));
    }


    // Here handle the proceed option of the user
    @UssdSubMenuHandler("*1*2*{amount}*{input}*{beneficiaryAccount}*{proceedOption}#")
    public String proceedToPaymentOrCancelForInternal(UssdContext context, String amount, String input, String benefitsAccount, String proceedOption){
        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);

        // Check if the user decides to go back.
        if(ContextManager.isGoBackOption(proceedOption)){
            return continueSessionMessage(template.getEnterBeneficiaryAccountNumber());
        }

        return continueSessionMessage(template.getEnterPinToProceedScreen());
    }

    // Here the user have inserted 4 digit pin. Do funds transfer
    @UssdSubMenuHandler("*1*2*{amount}*{input}*{beneficiaryAccount}*{proceedOption}*{pin}#")
    public String executeFundsTransferForInternal(UssdContext context, String amount, String input, String benefitsAccount, String proceedOption, String pin){
        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);
        String message;

        // Charge the customer for the USSD service
        String selectedAccount = String.valueOf(customerData.get("selectedCustomerAccount"));
        Object isCustomerCharged = customerData.get("isCustomerCharged");
        if(isCustomerCharged == null || !((Boolean)isCustomerCharged)) {
            UserProfileDetailsData data = (UserProfileDetailsData) customerData.get("customerDetails");
            ChargeCustomerRequestPayload requestPayload = new ChargeCustomerRequestPayload();
            requestPayload.setAmount(ChargeAmounts.USSD_SERVICE_CHARGE);
            requestPayload.setEventId(chargeEventId.getTRANSFER_AND_TRANSACTION_FEE_EVENT_ID());
            requestPayload.setSenderName(String.join(" ", data.getFirstName(), data.getMiddleName(), data.getLastName()));
            requestPayload.setPaymentReference(Utils.generatePaymentReference());
            requestPayload.setCustomerAccountNumber(String.valueOf(selectedAccount));
            requestPayload.setTranNarration("USSD Service Charge");
            log.info("Body to charge customer for Ussd service in Internal Funds transfer: {}", gson.toJson(requestPayload));
            ChargeCustomerResponsePayload responsePayload = fundsTransferService.chargeCustomer(requestPayload, pin);
            if (responsePayload.isStatus()) {
                customerData.put("isCustomerCharged", true);
            }
            log.info("Customer Charge Response InFT: {}", gson.toJson(responsePayload));
        }

        // Prepare the payload to the Funds Transfer Service.
        FundsTransferDTO requestDTO = FundsTransferDTO.builder()
                .sourceAccount(String.valueOf(customerData.get("selectedCustomerAccount")))
                .sourcePhoneNumber(context.getMobileNumber())
                .beneficiaryAccount(String.valueOf(customerData.get("beneficiaryAccount")))
                .beneficiaryName(String.valueOf(customerData.get("beneficiaryName")))
                .amount(amount)
                .bankName(String.valueOf(customerData.get("beneficiaryBank")))
                .pin(pin)
                .build();

        log.info("Internal Funds Transfer DTO to service: " + gson.toJson(requestDTO));

        FundsTransferResponse response = fundsTransferService.processIntraBankFundsTransfer(requestDTO);
        if(response != null){
            if(response.isDone()) {
                message = new MessageLineBuilder()
                        .addLine(String.format("Successful funds transfer to account %s", benefitsAccount))
                        .toString();

                // Charge the customer for a successful transfer.
                String fee = fundsTransferService.getUserTransactionFee(String.valueOf(customerData.get("selectedCustomerAccount")), "10", chargeEventId.getTRANSFER_AND_TRANSACTION_FEE_EVENT_ID());
                UserProfileDetailsData data = (UserProfileDetailsData) customerData.get("customerDetails");
                ChargeCustomerRequestPayload requestPayload = new ChargeCustomerRequestPayload();
                requestPayload.setAmount(fee);
                requestPayload.setEventId(chargeEventId.getTRANSFER_AND_TRANSACTION_FEE_EVENT_ID());
                requestPayload.setSenderName(String.join(" ", data.getFirstName(), data.getMiddleName(), data.getLastName()));
                requestPayload.setPaymentReference(Utils.generatePaymentReference());
                requestPayload.setCustomerAccountNumber(String.valueOf(selectedAccount));
                requestPayload.setTranNarration("Local Funds transfer Service Charge");
                log.info("Body to charge customer for Internal Funds transfer service: {}", gson.toJson(requestPayload));
                ChargeCustomerResponsePayload responsePayload = fundsTransferService.chargeCustomer(requestPayload, pin);

                log.info("Customer Charge Response For Internal funds transfer: {}", gson.toJson(responsePayload));
            }
            else if (response.isCancelled()){
                message = new MessageLineBuilder()
                        .addLine(String.format("Funds transfer to %s was cancelled by system. Please try again.", benefitsAccount))
                        .toString();
            }
            else if(response.isCompletedExceptionally()){
                message = new MessageLineBuilder()
                        .addLine(String.format("System could not process transfer to %s. An exception occurred internally.", benefitsAccount))
                        .toString();
            }
            else{
                String reason = response.getMessage() == null ? "System failed to respond" : response.getMessage();
                message = new MessageLineBuilder()
                        .addLine("System failed to process transaction.")
                        .addLine("Reason: " + reason)
                        .toString();
            }
        }else{
            message = new MessageLineBuilder()
                    .addLine("Oops! ")
                    .addLine("Transaction could not be completed. Please try again, thank you.")
                    .toString();
        }

        SessionManager.clearSession(context.getSessionId());
        return endSessionMessage(message);
    }
}
