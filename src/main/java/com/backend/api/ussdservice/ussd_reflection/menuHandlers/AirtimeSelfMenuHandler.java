package com.backend.api.ussdservice.ussd_reflection.menuHandlers;

import com.google.gson.Gson;
import com.backend.api.ussdservice.pojo.UssdMessageAndData;
import com.backend.api.ussdservice.ussd_reflection.annotation.UssdMenuHandler;
import com.backend.api.ussdservice.ussd_reflection.annotation.UssdSubMenuHandler;
import com.backend.api.ussdservice.ussd_reflection.constants.ChargeAmounts;
import com.backend.api.ussdservice.ussd_reflection.constants.ChargeEventId;
import com.backend.api.ussdservice.ussd_reflection.constants.CustomerTypes;
import com.backend.api.ussdservice.ussd_reflection.context.ContextManager;
import com.backend.api.ussdservice.ussd_reflection.context.Item;
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
public class AirtimeSelfMenuHandler
{
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

    private final AirtimeMenuTemplate airtimeMenuTemplate = new AirtimeMenuTemplate();
    private final HomeMenuTemplate homeMenuTemplate = new HomeMenuTemplate();
    private static final Gson gson = new Gson();

    @UssdSubMenuHandler("*1*4#")
    public String showAccountNumberForAirtimeSelf(UssdContext context) {
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

        UssdMessageAndData messageAndData = airtimeMenuTemplate
                .getAccountNumberListUssdMessageAndDataFromCollection(accountNumbers, customerData, "Account");

        SessionManager.updateExtraDataOfSession(context.getSessionId(), messageAndData.getCustomerData());
        return continueSessionMessage(messageAndData.getMessage());
    }

    @UssdSubMenuHandler("*1*4*{accountNumber}#")
    public String selectAccountNumberForAirtimeSelfOrGoBack(UssdContext context, String accountNumber){
        String goBack = ContextManager.getItem(Item.DEFAULT_USSD_GO_BACK_OPTION, String.class);
        if(accountNumber.equalsIgnoreCase(goBack)) {
            return continueSessionMessage(homeMenuTemplate.getExistingCustomerHomeMenuListScreen());
        }

        // Wrong input
        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);
        Object selectedAccountNumber = customerData.get("Account".concat(accountNumber));
        if(selectedAccountNumber == null){
            return endSessionMessage(airtimeMenuTemplate.getErrorMessage(context));
        }

        // Correct input. Save selected account number.
        customerData.put("selectedAccountNumberForAirtimeSelf", accountNumber);
        SessionManager.updateExtraDataOfSession(context.getSessionId(), customerData);
        return continueSessionMessage(airtimeMenuTemplate.getEnterAmountToRecharge());
    }

    @UssdSubMenuHandler("*1*4*{accountNumber}*{amount}#")
    public String enterAmountForAirtimeSelfOrGoBack(UssdContext context, String acN, String amount){
        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);

        if(amount.equalsIgnoreCase("0")){
            List<String> accountNumbers = (List<String>) customerData.get("accountNumbers");

            UssdMessageAndData messageAndData = airtimeMenuTemplate
                    .getAccountNumberListUssdMessageAndDataFromCollection(accountNumbers, customerData, "Account");

            SessionManager.updateExtraDataOfSession(context.getSessionId(), messageAndData.getCustomerData());
            return continueSessionMessage(messageAndData.getMessage());
        }

        // Save the amount.
        customerData.put("amountForAirtimeSelf", amount);
        SessionManager.updateExtraDataOfSession(context.getSessionId(), customerData);
        return continueSessionMessage(airtimeMenuTemplate.getEnterPinForAirtime());
    }

    @UssdSubMenuHandler("*1*4*{accountNumber}*{amount}*{pin}#")
    public String enterPinOrGoBackForAirtimeSelf(UssdContext context, String accountNumber, String amount, String pin){
        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);
        String goBack = ContextManager.getItem(Item.DEFAULT_USSD_GO_BACK_OPTION, String.class);
        if(pin.equalsIgnoreCase(goBack)){
            return continueSessionMessage(airtimeMenuTemplate.getEnterAmountToRecharge());
        }

        // Charge the customer for the USSD service.
        Object isCustomerCharged = customerData.get("isCustomerCharged");
        if(isCustomerCharged == null || !((Boolean)isCustomerCharged)) {
            UserProfileDetailsData data = (UserProfileDetailsData) customerData.get("customerDetails");
            ChargeCustomerRequestPayload requestPayload = new ChargeCustomerRequestPayload();
            requestPayload.setAmount(ChargeAmounts.USSD_SERVICE_CHARGE);
            requestPayload.setEventId(chargeEventId.getTRANSFER_AND_TRANSACTION_FEE_EVENT_ID());
            requestPayload.setSenderName(String.join(" ", data.getFirstName(), data.getMiddleName(), data.getLastName()));
            requestPayload.setPaymentReference(Utils.generatePaymentReference());
            requestPayload.setCustomerAccountNumber((String) customerData.get("Account".concat(accountNumber)));
            requestPayload.setTranNarration("USSD Service Charge");
            log.info("Body to charge customer for Ussd service for airtime self: {}", gson.toJson(requestPayload));
            ChargeCustomerResponsePayload responsePayload = fundsTransferService.chargeCustomer(requestPayload, pin);
            if (responsePayload.isStatus()) {
                customerData.put("isCustomerCharged", true);
            }
            log.info("Customer Charge Response for airtime self: {}", gson.toJson(responsePayload));
        }

        AirtimeDataBillerData billerData = airtimeDataService.getAirtimeBillerInfoFromNetwork(context.getTelco());
        log.info("Biller Data For Self: {}", billerData);

        // Prepare payload to buy airtime.
        AirtimeDataRequestDTO requestDTO = new AirtimeDataRequestDTO();
        requestDTO.setAmount(amount);
        requestDTO.setAccountNumber((String) customerData.get("Account".concat(accountNumber)));
        requestDTO.setBillerId(billerData != null ? billerData.getBillerId() : "");
        requestDTO.setCategoryId(billerData != null ? billerData.getCategoryId() : "");
        requestDTO.setPhoneNumber(Utils.sanitizePhoneNumber(context.getMobileNumber()));
        requestDTO.setPin(pin);
        log.info("Before buying airtime: {}", new Gson().toJson(requestDTO));
        WayaPayWebResponse webResponse = airtimeDataService.processAirtimePayment(requestDTO);
        log.info("Response FROM WayaPay: " + new Gson().toJson(webResponse));

        String message;
        if(webResponse != null) {
            if (webResponse.isStatus()) {
                // Now charge the customer
                String eventIdOfAggregator = billsPaymentService.getActiveAggregator();
                String acc = String.valueOf(customerData.get("Account".concat(accountNumber)));
                String fee = fundsTransferService.getUserTransactionFee(acc, "10", eventIdOfAggregator);
                UserProfileDetailsData data = (UserProfileDetailsData) customerData.get("customerDetails");
                ChargeCustomerRequestPayload requestPayload = new ChargeCustomerRequestPayload();
                requestPayload.setAmount(fee);
                requestPayload.setEventId(eventIdOfAggregator);
                requestPayload.setSenderName(String.join(" ", data.getFirstName(), data.getMiddleName(), data.getLastName()));
                requestPayload.setPaymentReference(Utils.generatePaymentReference());
                requestPayload.setCustomerAccountNumber(String.valueOf(acc));
                requestPayload.setTranNarration("Airtime self service charge");
                log.info("Body to charge the customer for airtime self: {}", requestPayload);

                ChargeCustomerResponsePayload responsePayload = fundsTransferService.chargeCustomer(requestPayload, pin);
                log.info("Response to charge customer for airtime self: {}", gson.toJson(responsePayload));

                message = new MessageLineBuilder()
                        .addLine(endSessionMessage(String.format("Airtime for %s successful", context.getMobileNumber())))
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