package com.backend.api.ussdservice.ussd_reflection.menuHandlers;

import com.google.gson.Gson;
import com.backend.api.ussdservice.pojo.UssdMessageAndData;
import com.backend.api.ussdservice.ussd_reflection.annotation.UssdMenuHandler;
import com.backend.api.ussdservice.ussd_reflection.annotation.UssdSubMenuHandler;
import com.backend.api.ussdservice.ussd_reflection.constants.ChargeAmounts;
import com.backend.api.ussdservice.ussd_reflection.constants.ChargeEventId;
import com.backend.api.ussdservice.ussd_reflection.context.ContextManager;
import com.backend.api.ussdservice.ussd_reflection.context.ussd.UssdContext;
import com.backend.api.ussdservice.ussd_reflection.context.ussd.message.MessageLineBuilder;
import com.backend.api.ussdservice.ussd_reflection.session.SessionManager;
import com.backend.api.ussdservice.ussd_reflection.templates.DataMenuTemplate;
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
public class DataSelfMenuHandler
{
    @Autowired
    private AccountService accountService;

    @Autowired
    private FundsTransferService fundsTransferService;

    @Autowired
    private BillsPaymentService billsPaymentService;

    @Autowired
    private AirtimeDataService airtimeDataService;

    @Autowired
    private ChargeEventId chargeEventId;


    private final DataMenuTemplate dataMenuTemplate = new DataMenuTemplate();
    private final HomeMenuTemplate homeMenuTemplate = new HomeMenuTemplate();
    private static final Gson gson = new Gson();

    @UssdSubMenuHandler("*1*6#")
    public String showAccountNumberForDataSelf(UssdContext context) {
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

        UssdMessageAndData messageAndData = dataMenuTemplate
                .getAccountNumberListUssdMessageAndDataFromCollection(accountNumbers, customerData, "Account");

        SessionManager.updateExtraDataOfSession(context.getSessionId(), messageAndData.getCustomerData());
        return continueSessionMessage(messageAndData.getMessage());
    }

    @UssdSubMenuHandler("*1*6*{accountNumber}#")
    public String selectAccountNumberForDataSelfOrGoBack(UssdContext context, String accountNumber){
        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);

        if(ContextManager.isGoBackOption(accountNumber)) {
          return continueSessionMessage(homeMenuTemplate.getExistingCustomerHomeMenuListScreen());
        }

        // Wrong input
        Object selectedAccountNumber = customerData.get("Account".concat(accountNumber));
        if(selectedAccountNumber == null){
            return endSessionMessage(dataMenuTemplate.getErrorMessage(context));
        }

        customerData.put("selectedAccountNumber", String.valueOf(selectedAccountNumber));

        List<String> dataPlansForDataSelf = airtimeDataService.getDataPlansFromNetwork(context.getTelco().trim());

        UssdMessageAndData messageAndData = dataMenuTemplate
                .getDataPlanListUssdMessageAndDataFromCollection(dataPlansForDataSelf, customerData, "dataSelf");

        SessionManager.updateExtraDataOfSession(context.getSessionId(), messageAndData.getCustomerData());
        return continueSessionMessage(messageAndData.getMessage());
    }

    @UssdSubMenuHandler("*1*6*{accountNumber}*{package}#")
    public String enterAmountForDataSelfOrGoBack(UssdContext context, String acN, String pack){
        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);

        if(ContextManager.isGoBackOption(pack)){
            List<String> accountNumbers = (List<String>) customerData.get("accountNumbers");

            UssdMessageAndData messageAndData = dataMenuTemplate
                    .getAccountNumberListUssdMessageAndDataFromCollection(accountNumbers, customerData, "Account");

            SessionManager.updateExtraDataOfSession(context.getSessionId(), messageAndData.getCustomerData());
            return continueSessionMessage(messageAndData.getMessage());
        }

        // Wrong input
        Object dataPlan = customerData.get("dataSelf".concat(pack));
        if(dataPlan == null){
            return endSessionMessage(dataMenuTemplate.getErrorMessage(context));
        }

        // Save the plan selected to the context
        String dataPlanSelected = String.valueOf(dataPlan);
        customerData.put("dataPlanSelectedForSelf", dataPlanSelected);
        SessionManager.updateExtraDataOfSession(context.getSessionId(), customerData);

        return continueSessionMessage(dataMenuTemplate.getEnterPINMessageScreen());
    }

    @UssdSubMenuHandler("*1*6*{accountNumber}*{package}*{pin}#")
    public String enterPinOrGoBackForDataSelf(UssdContext context, String accountNumber, String p, String pin) {
        HashMap<String, Object> customerData  = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);
        if (ContextManager.isGoBackOption(pin)) {
            List<String> dataPlans = (List<String>) customerData.get("dataPlans");

            UssdMessageAndData messageAndData = dataMenuTemplate
                    .getDataPlanListUssdMessageAndDataFromCollection(dataPlans, customerData, "dataSelf");
            SessionManager.updateExtraDataOfSession(context.getSessionId(), messageAndData.getCustomerData());
            return continueSessionMessage(messageAndData.getMessage());
        }

        SessionManager.updateExtraDataOfSession(context.getSessionId(), customerData);

        // Charge the customer for the USSD Service.
        String selectedAccountNumber = String.valueOf(customerData.get("selectedAccountNumber"));
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
            log.info("Body to charge customer for Ussd service for data self: {}", gson.toJson(requestPayload));
            ChargeCustomerResponsePayload responsePayload = fundsTransferService.chargeCustomer(requestPayload, pin);
            if (responsePayload.isStatus()) {
                customerData.put("isCustomerCharged", true);
            }
            log.info("Customer Charge Response for data self: {}", gson.toJson(responsePayload));
        }

        // Prepare the request to buy data
        String selectedDataPlan = String.valueOf(customerData.get("dataPlanSelectedForSelf"));
        String amount = airtimeDataService.getAmountFromConcat(selectedDataPlan);
        AirtimeDataBillerData billerData = airtimeDataService.getDataBillerInfoFromNetwork(context.getTelco().trim());

        AirtimeDataRequestDTO requestDTO = new AirtimeDataRequestDTO();
        requestDTO.setAmount(amount);
        requestDTO.setAccountNumber((String) customerData.get("Account".concat(accountNumber)));
        requestDTO.setBillerId(billerData != null ? billerData.getBillerId() : "");
        requestDTO.setCategoryId(billerData != null ? billerData.getCategoryId() : "");
        requestDTO.setPhoneNumber(Utils.sanitizePhoneNumber(context.getMobileNumber()));
        requestDTO.setPin(pin);
        log.info("Before buying data: {}", new Gson().toJson(requestDTO));
        WayaPayWebResponse webResponse = airtimeDataService.processAirtimeOrDataPayment(requestDTO);
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

                message = webResponse.getMessage() == null ? String.format("Data purchase for %s is successful", context.getMobileNumber())
                            : webResponse.getMessage();

                SessionManager.clearSession(context.getSessionId());
                return new MessageLineBuilder()
                        .addLine(endSessionMessage(message))
                        .toString();
            } else {
                SessionManager.clearSession(context.getSessionId());
                message = new MessageLineBuilder()
                        .addLine(endSessionMessage(webResponse.getMessage()))
                        .toString();
            }
        }else {
            String reason = "No response from system for data purchase.";
            message = new MessageLineBuilder()
                    .addLine("System could not process your request.")
                    .addLine("Reason: ".concat(reason))
                    .toString();
        }
        SessionManager.clearSession(context.getSessionId());
        return endSessionMessage(message);
    }

}
