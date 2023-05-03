package com.backend.api.ussdservice.ussd_reflection.menuHandlers;

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
import com.backend.api.ussdservice.ussd_reflection.templates.AirtimeMenuTemplate;
import com.backend.api.ussdservice.ussd_reflection.templates.HomeMenuTemplate;
import com.backend.api.ussdservice.ussd_reflection.utils.Utils;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.WayaPayWebResponse;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.dto.AirtimeDataRequestDTO;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.request.ChargeCustomerRequestPayload;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.response.AccountNumbersResponse;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.response.AirtimeDataBillerData;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.response.ChargeCustomerResponsePayload;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.response.UserProfileDetailsData;
import com.backend.api.ussdservice.ussd_reflection.web.services.AccountService;
import com.backend.api.ussdservice.ussd_reflection.web.services.AirtimeDataService;
import com.backend.api.ussdservice.ussd_reflection.web.services.BillsPaymentService;
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
public class AirtimeOthersMenuHandler {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AirtimeDataService airtimeDataService;

    @Autowired
    private FundsTransferService fundsTransferService;

    @Autowired
    private BillsPaymentService billsPaymentService;

    @Autowired
    private ChargeEventId chargeEventId;


    private final AirtimeMenuTemplate airtimeOthersTemplate = new AirtimeMenuTemplate();
    private final HomeMenuTemplate homeMenuTemplate = new HomeMenuTemplate();
    private static final Gson gson = new Gson();
    @UssdSubMenuHandler("*1*5#")
    public String showAccountNumberForAirtimeOthers(UssdContext context) {
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

        UssdMessageAndData messageAndData = airtimeOthersTemplate
                .getAccountNumberListUssdMessageAndDataFromCollection(accountNumbers, customerData, "Account");

        SessionManager.updateExtraDataOfSession(context.getSessionId(), messageAndData.getCustomerData());
        return continueSessionMessage(messageAndData.getMessage());
    }

    @UssdSubMenuHandler("*1*5*{accountOption}#")
    public String showBeneficiaryPhoneNumber(UssdContext context, String accountOption){
        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);
        if(ContextManager.isGoBackOption(accountOption)){
            return continueSessionMessage(showAccountNumberForAirtimeOthers(context));
        }

        // Wrong input
        Object selectedAccountNumber = customerData.get("Account".concat(accountOption));
        if(selectedAccountNumber == null){
            return endSessionMessage(airtimeOthersTemplate.getErrorMessage(context));
        }
        customerData.put("selectedAccountNumber", String.valueOf(selectedAccountNumber));
        SessionManager.updateExtraDataOfSession(context.getSessionId(), customerData);
        return continueSessionMessage(airtimeOthersTemplate.getEnterBeneficiaryPhone());
    }

    @UssdSubMenuHandler("*1*5*{accountOption}*{beneficiaryPhoneNo}#")
    public String enterBeneficiaryPhoneNumberForAirtimeOthersOrGoBack(UssdContext context, String accountOption, String beneficiaryPhoneNo){
        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);
        if(ContextManager.isGoBackOption(beneficiaryPhoneNo)) {
            List<String> accountNumbers = (List<String>) customerData.get("accountNumbers");
            UssdMessageAndData messageAndData = airtimeOthersTemplate
                    .getAccountNumberListUssdMessageAndDataFromCollection(accountNumbers, customerData, "Account");

            SessionManager.updateExtraDataOfSession(context.getSessionId(), messageAndData.getCustomerData());
            return continueSessionMessage(messageAndData.getMessage());
        }

        // Save the account number to the session manager
        customerData.put("beneficiaryPhoneNumberForAirtimeOthers", beneficiaryPhoneNo);
        SessionManager.updateExtraDataOfSession(context.getSessionId(), customerData);

        List<String> networks = airtimeDataService.getNetworkList();
        UssdMessageAndData messageAndData = airtimeOthersTemplate
                .getNetworkListUssdMessageAndDataFromCollection(networks, customerData, "network");

        SessionManager.updateExtraDataOfSession(context.getSessionId(), messageAndData.getCustomerData());

        return continueSessionMessage(messageAndData.getMessage());
    }


    @UssdSubMenuHandler("*1*5*{accountOption}*{beneficiaryPhoneNo}*{benNet}#")
    public String selectBeneficiaryNetworkScreen(UssdContext context, String accN, String benP, String benNet){
        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);
        if(ContextManager.isGoBackOption(benNet)){
            return continueSessionMessage(airtimeOthersTemplate.getEnterBeneficiaryPhone());
        }

        // Wrong input
        Object selectedNetwork = customerData.get("network".concat(benNet));
        if(selectedNetwork == null){
            return endSessionMessage(airtimeOthersTemplate.getErrorMessage(context));
        }

        String beneficiaryNetwork = String.valueOf(selectedNetwork);
        log.info("BeneficiaryNetwork: {}", beneficiaryNetwork);
        AirtimeDataBillerData billerData = airtimeDataService.getAirtimeBillerInfoFromNetwork(beneficiaryNetwork);
        log.info("Response From Service: {}", billerData);
        customerData.put("airtimeForOthersBillerData", billerData);
        customerData.put("beneficiaryNetworkForAirtimeOthers", beneficiaryNetwork);
        SessionManager.updateExtraDataOfSession(context.getSessionId(), customerData);

        return continueSessionMessage(airtimeOthersTemplate.getEnterAmountToRecharge());
    }

    @UssdSubMenuHandler("*1*5*{accountOption}*{beneficiaryPhoneNo}*{benNet}*{amount}#")
    public String enterAmountForAirtimeOthersOrGoBack(UssdContext context, String accN, String beneficiaryPhoneNo, String benNet, String amount) {
        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);
        if (ContextManager.isGoBackOption(amount)) {
            List<String> networks = airtimeDataService.getNetworkList();
            UssdMessageAndData messageAndData = airtimeOthersTemplate
                    .getNetworkListUssdMessageAndDataFromCollection(networks, customerData, "network");

            SessionManager.updateExtraDataOfSession(context.getSessionId(), messageAndData.getCustomerData());

            return continueSessionMessage(messageAndData.getMessage());
        }

        customerData.put("amountForAirtimeOthers", amount);
        SessionManager.updateExtraDataOfSession(context.getSessionId(), customerData);

        return continueSessionMessage(airtimeOthersTemplate.getEnterPinForAirtime());
    }

    @UssdSubMenuHandler("*1*5*{account}*{beneficiaryPhoneNo}*{benNet}*{amount}*{pin}#")
    public String enterPinOrGoBackForAirtimeOthers(UssdContext context, String accN, String beneficiaryPhoneNo, String benNet, String amount, String pin){
        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);
        if(ContextManager.isGoBackOption(pin)){
            return continueSessionMessage(airtimeOthersTemplate.getEnterAmountToRecharge());
        }

        String selectedAccountNumber = String.valueOf(customerData.get("selectedAccountNumber"));

        // Charge the customer for USSD service.
        Object isCustomerCharged = customerData.get("isCustomerCharged");
        if(isCustomerCharged == null || !((Boolean)isCustomerCharged)) {
            UserProfileDetailsData data = (UserProfileDetailsData) customerData.get("customerDetails");
            ChargeCustomerRequestPayload requestPayload = new ChargeCustomerRequestPayload();
            requestPayload.setAmount(ChargeAmounts.USSD_SERVICE_CHARGE);
            requestPayload.setEventId(chargeEventId.getTRANSFER_AND_TRANSACTION_FEE_EVENT_ID());
            requestPayload.setSenderName(String.join(" ", data.getFirstName(), data.getMiddleName(), data.getLastName()));
            requestPayload.setPaymentReference(Utils.generatePaymentReference());
            requestPayload.setCustomerAccountNumber(String.valueOf(selectedAccountNumber));
            requestPayload.setTranNarration("USSD Service Charge");
            log.info("Body to charge customer for Ussd service in Airtime others: {}", gson.toJson(requestPayload));
            ChargeCustomerResponsePayload responsePayload = fundsTransferService.chargeCustomer(requestPayload, pin);
            if (responsePayload != null && responsePayload.isStatus()) {
                customerData.put("isCustomerCharged", true);
            }
            log.info("Customer Charge Response in Airtime others: {}", gson.toJson(responsePayload));
        }

        AirtimeDataBillerData billerData = (AirtimeDataBillerData) customerData.get("airtimeForOthersBillerData");
        log.info("Biller Data For Others: {}", billerData);

        // Prepare payload to buy airtime.
        AirtimeDataRequestDTO requestDTO = new AirtimeDataRequestDTO();
        requestDTO.setAmount(amount);
        requestDTO.setAccountNumber((String) customerData.get("Account".concat(accN)));
        requestDTO.setBillerId(billerData != null ? billerData.getBillerId() : "");
        requestDTO.setCategoryId(billerData != null ? billerData.getCategoryId() : "");
        requestDTO.setPhoneNumber(Utils.sanitizePhoneNumber(beneficiaryPhoneNo));
        requestDTO.setPin(pin);
        log.info("Before buying data: {}", new Gson().toJson(requestDTO));
        WayaPayWebResponse webResponse = airtimeDataService.processAirtimePayment(requestDTO);
        log.info("Response FROM WayaPay: " + new Gson().toJson(webResponse));
        SessionManager.clearSession(context.getSessionId());
        String message;
        if(webResponse != null) {
            if (webResponse.isStatus()) {
                // Now charge the customer
                String eventIdOfAggregator = billsPaymentService.getActiveAggregator();
                String acc = String.valueOf(customerData.get("Account".concat(accN)));
                String fee = fundsTransferService.getUserTransactionFee(acc, "10", eventIdOfAggregator);
                UserProfileDetailsData data = (UserProfileDetailsData) customerData.get("customerDetails");
                ChargeCustomerRequestPayload requestPayload = new ChargeCustomerRequestPayload();
                requestPayload.setAmount(fee);
                requestPayload.setEventId(eventIdOfAggregator);
                requestPayload.setSenderName(String.join(" ", data.getFirstName(), data.getMiddleName(), data.getLastName()));
                requestPayload.setPaymentReference(Utils.generatePaymentReference());
                requestPayload.setCustomerAccountNumber(String.valueOf(selectedAccountNumber));
                requestPayload.setTranNarration("Airtime others service charge");
                log.info("Body to charge the customer for airtime others: {}", requestPayload);

                ChargeCustomerResponsePayload responsePayload = fundsTransferService.chargeCustomer(requestPayload, pin);
                log.info("Response to charge customer for airtime others: {}", gson.toJson(responsePayload));

                message = new MessageLineBuilder()
                        .addLine(endSessionMessage(String.format("Airtime for %s successful", beneficiaryPhoneNo)))
                        .toString();
            } else {
                message = new MessageLineBuilder()
                        .addLine(endSessionMessage(webResponse.getMessage()))
                        .toString();
            }
        }else {
            String reason = "No response from system";
            message = new MessageLineBuilder()
                    .addLine("System could not process your request.")
                    .addLine("Reason: ".concat(reason))
                    .toString();
        }
        SessionManager.clearSession(context.getSessionId());
        return endSessionMessage(message);
    }

    private String getBillerNameString(String network){
        if(network.equalsIgnoreCase("ETISALAT"))
            return "9mobilevtu";
        else
            return network.toLowerCase().concat("vtu");
    }
}
