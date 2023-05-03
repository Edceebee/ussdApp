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
import com.backend.api.ussdservice.ussd_reflection.context.model.UssdSession;
import com.backend.api.ussdservice.ussd_reflection.context.ussd.UssdContext;
import com.backend.api.ussdservice.ussd_reflection.context.ussd.message.MessageLineBuilder;
import com.backend.api.ussdservice.ussd_reflection.session.SessionManager;
import com.backend.api.ussdservice.ussd_reflection.templates.FundsTransferMenuTemplate;
import com.backend.api.ussdservice.ussd_reflection.templates.HomeMenuTemplate;
import com.backend.api.ussdservice.ussd_reflection.utils.Utils;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.dto.BankAndIndex;
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


@UssdMenuHandler
@Slf4j
public class FundsTransferMenuHandler
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

    @UssdSubMenuHandler("*1*3#")
    public String enterAmountToFundScreen(UssdContext context){
        return continueSessionMessage(template.getEnterAmountScreen());
    }

    // User enter amount
    @UssdSubMenuHandler("*1*3*{amount}#")
    public String selectAccountToTransferFrom(UssdContext context, String amount){
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

        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);
        customerData.put("customerType", CustomerTypes.EXISTING.name());

        UssdMessageAndData messageAndData = template
                .getAccountNumberListUssdMessageAndDataFromCollection(accountNumbers, customerData, "Account");

        SessionManager.updateExtraDataOfSession(context.getSessionId(), messageAndData.getCustomerData());
        return continueSessionMessage(messageAndData.getMessage());
    }

    // User decides to go back to the main menu
    @UssdSubMenuHandler("*1*3*0#")
    public String goBackToMainMenu(UssdContext context){
        return continueSessionMessage(homeMenuTemplate.getExistingCustomerHomeMenuListScreen());
    }


    // User decides to enter beneficiary number or go back. The input here represents the number option
    // of the corresponding account.
    @UssdSubMenuHandler("*1*3*{amount}*{input}#")
    public String showEnterBeneficiaryAccountMsg(UssdContext context, String amount, String input){

        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);

        // Check if the user enters 0. Then go back to the previous menu.
        if(ContextManager.isGoBackOption(input)){
           return continueSessionMessage(enterAmountToFundScreen(context));
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
    @UssdSubMenuHandler("*1*3*{amount}*{input}*{beneficiaryAccount}#")
    public String enterBeneficiaryAccountOrGoBackToAccountList(UssdContext context, String amount, String input, String beneficiaryAccount){
        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);
        List<String> accountNumbers = (List<String>) customerData.get("accountNumbers");

        // Check if the user wants to go back.
        if(ContextManager.isGoBackOption(beneficiaryAccount)){
            UssdMessageAndData messageAndData = template
                    .getAccountNumberListUssdMessageAndDataFromCollection(accountNumbers, customerData, "Account");

            SessionManager.updateExtraDataOfSession(context.getSessionId(), messageAndData.getCustomerData());
            return continueSessionMessage(messageAndData.getMessage());
        }

        // Here the beneficiary account is entered. Save it and navigate to the beneficiary details screen.
        customerData.put("beneficiaryAccount", beneficiaryAccount);

        // Also get the array of bank names associated with the beneficiary account number.
        BankAndIndex bankAndIndex = this.fundsTransferService.getNextBanksForSession(context.getSessionId());
        UssdMessageAndData messageAndData = template
                .getBankListUssdMessageAndDataFromBankAndIndex(bankAndIndex, customerData, "bank");

        // Update the session data
        SessionManager.updateExtraDataOfSession(context.getSessionId(), messageAndData.getCustomerData());
        return continueSessionMessage(messageAndData.getMessage());
    }


    // Here user selects a bankName or decides to go back to the
    @UssdSubMenuHandler("*1*3*{amount}*{input}*{beneficiaryAccount}*{bankNumber}#")
    public String showBeneficiaryDetailsScreenOrGoBackToBankList(UssdContext context, String amount, String input, String beneficiaryAccount, String bankNumber){
        // Get the customer details
        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);
        customerData.put("bankListContextData", context.getOriginatingContextData());
        SessionManager.updateExtraDataOfSession(context.getSessionId(), customerData);

        // Check if the customer wants to go back
        String isBankListJustStarted = String.valueOf(customerData.get("isBankListJustStarted"));
        if(ContextManager.isGoBackOption(bankNumber)){
            // Check if the bankList is just starting
            if(isBankListJustStarted.equalsIgnoreCase("true")) {
                final String bankIndexArrayKey = "BANK_INDEX_ARRAY";
                customerData.remove(bankIndexArrayKey);
                return continueSessionMessage(template.getEnterBeneficiaryAccountNumber());
            }else {
                final String bankIndexArrayKey = "BANK_INDEX_ARRAY";
                List<Integer> bankIndexArray = (List<Integer>) customerData.get(bankIndexArrayKey);
                if(bankIndexArray.get(0) == 16 && bankIndexArray.get(1) == 20){
                    customerData.remove(bankIndexArrayKey);
                }
                if(bankIndexArray.get(0) == 0 && bankIndexArray.get(1) == 7){
                    customerData.put(bankIndexArrayKey, List.of(8, 15));
                }
                BankAndIndex bankAndIndex = this.fundsTransferService.getNextBanksForSession(context.getSessionId());
                UssdMessageAndData messageAndData = template
                        .getBankListUssdMessageAndDataFromBankAndIndex(bankAndIndex, customerData, "bank");

                // Update the session and context data
                SessionManager.updateExtraDataOfSession(context.getSessionId(), messageAndData.getCustomerData());

                SessionManager sessionManager = new SessionManager();
                String originatingContextData = String.valueOf(customerData.get("bankListContextData"));
                UssdSession session = sessionManager.findBySessionId(context.getSessionId());
                session.setContextData(originatingContextData);
                sessionManager.save(session);
                return continueSessionMessage(messageAndData.getMessage());
            }

        }

        // Check if the customer presses next option of "00"
        if(bankNumber.equalsIgnoreCase("00")){
            BankAndIndex bankAndIndex = this.fundsTransferService.getNextBanksForSession(context.getSessionId());
            UssdMessageAndData messageAndData = template
                    .getBankListUssdMessageAndDataFromBankAndIndex(bankAndIndex, customerData, "bank");

            // Update the session data
            SessionManager.updateExtraDataOfSession(context.getSessionId(), messageAndData.getCustomerData());

            SessionManager sessionManager = new SessionManager();
            String originatingContextData = String.valueOf(customerData.get("bankListContextData"));
            UssdSession session = sessionManager.findBySessionId(context.getSessionId());
            session.setContextData(originatingContextData);
            sessionManager.save(session);
            return continueSessionMessage(messageAndData.getMessage());
        }

        // Wrong input
        Object selectedBank = customerData.get("bank".concat(bankNumber));
        if(selectedBank == null){
            return endSessionMessage(template.getErrorMessage(context));
        }

        // Do name enquiry
        String beneficiaryBank = String.valueOf(selectedBank);
        NameEnquiryResponse nameEnquiryResponse = fundsTransferService
                .getBeneficiaryNameEnquiry(beneficiaryBank, beneficiaryAccount);

        if(nameEnquiryResponse != null &&
                nameEnquiryResponse.isStatus() &&
                !nameEnquiryResponse.getData().isEmpty() &&
                !nameEnquiryResponse.getData().isBlank())
        {
            String beneficiaryName = nameEnquiryResponse.getData();
            customerData.put("beneficiaryName", beneficiaryName);
            customerData.put("beneficiaryBank", beneficiaryBank);
            SessionManager.updateExtraDataOfSession(context.getSessionId(), customerData);
            return template.getProceedToTransferScreen(beneficiaryBank, beneficiaryName, beneficiaryAccount);
        }

        // Here name enquiry fails. Display error message and cancel session
        SessionManager.clearSession(context.getSessionId());
        return endSessionMessage(template.getNameInquiryFailureMessage(context));
    }

    // Here handle the proceed option of the user
    @UssdSubMenuHandler("*1*3*{amount}*{input}*{beneficiaryAccount}*{bankNumber}*{proceedOption}#")
    public String proceedToPaymentOrCancel(UssdContext context, String amount, String input, String benefitsAccount, String bankNumber, String proceedOption){
        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);

        // Check if the user decides to go back.
        if(ContextManager.isGoBackOption(proceedOption)){
            BankAndIndex bankAndIndex = this.fundsTransferService.getNextBanksForSession(context.getSessionId());
            UssdMessageAndData messageAndData = template
                    .getBankListUssdMessageAndDataFromBankAndIndex(bankAndIndex, customerData, "bank");

            // Update the session and context data
            SessionManager.updateExtraDataOfSession(context.getSessionId(), messageAndData.getCustomerData());

            SessionManager sessionManager = new SessionManager();
            String originatingContextData = String.valueOf(customerData.get("bankListContextData"));
            UssdSession session = sessionManager.findBySessionId(context.getSessionId());
            session.setContextData(originatingContextData);
            sessionManager.save(session);
            return continueSessionMessage(messageAndData.getMessage());        }

        return continueSessionMessage(template.getEnterPinToProceedScreen());
    }

    // Here the user have inserted 4 digit pin. Do funds transfer
    @UssdSubMenuHandler("*1*3*{amount}*{input}*{beneficiaryAccount}*{bankNumber}*{proceedOption}*{pin}#")
    public String executeFundsTransfer(UssdContext context, String amount, String input, String benefitsAccount, String bankNumber, String proceedOption, String pin){
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
            log.info("Body to charge customer for Ussd service in Funds transfer: {}", gson.toJson(requestPayload));
            ChargeCustomerResponsePayload responsePayload = fundsTransferService.chargeCustomer(requestPayload, pin);
            if (responsePayload.isStatus()) {
                customerData.put("isCustomerCharged", true);
            }
            log.info("Customer Charge Response IFT: {}", gson.toJson(responsePayload));
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

        log.info("Funds Transfer DTO to service: " + gson.toJson(requestDTO));

        FundsTransferResponse response = fundsTransferService.processInterbankFundsTransfer(requestDTO);
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
                requestPayload.setTranNarration("Funds transfer Service Charge");
                log.info("Body to charge customer for Funds transfer service: {}", gson.toJson(requestPayload));
                ChargeCustomerResponsePayload responsePayload = fundsTransferService.chargeCustomer(requestPayload, pin);

                log.info("Customer Charge Response For Inter funds transfer: {}", gson.toJson(responsePayload));
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
