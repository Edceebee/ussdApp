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
public class DataOthersMenuHandler {

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


    private final DataMenuTemplate dataMenuTemplate = new DataMenuTemplate();
    private final HomeMenuTemplate homeMenuTemplate = new HomeMenuTemplate();
    private final AirtimeMenuTemplate airtimeMenuTemplate = new AirtimeMenuTemplate();

    private static final Gson gson = new Gson();

    @UssdSubMenuHandler("*1*7#")
    public String showAccountNumberForDataOthers(UssdContext context) {
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

        UssdMessageAndData messageAndData = dataMenuTemplate
                .getAccountNumberListUssdMessageAndDataFromCollection(accountNumbers, customerData, "Account");

        SessionManager.updateExtraDataOfSession(context.getSessionId(), messageAndData.getCustomerData());
        return continueSessionMessage(messageAndData.getMessage());
    }

    @UssdSubMenuHandler("*1*7*{accountNumber}#")
    public String enterAmountForDataOthersOrGoBack(UssdContext context, String accountNumber){
        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);
        if(ContextManager.isGoBackOption(accountNumber)){
            return continueSessionMessage(homeMenuTemplate.getExistingCustomerHomeMenuListScreen());
        }

        // Wrong input
        Object selectedAccountNumber = customerData.get("Account".concat(accountNumber));
        if(selectedAccountNumber == null){
            return endSessionMessage(dataMenuTemplate.getErrorMessage(context));
        }

        // Save the account number and navigate to the beneficiary phone number page.
        customerData.put("selectedAccountNumber", String.valueOf(selectedAccountNumber));
        SessionManager.updateExtraDataOfSession(context.getSessionId(), customerData);

        return continueSessionMessage(dataMenuTemplate.getEnterBeneficiaryPhoneNumber());
    }

    @UssdSubMenuHandler("*1*7*{accountNumber}*{beneficiaryPhoneNumber}#")
    public String selectAccountNumberForDataOthersOrGoBack(UssdContext context, String accN, String phoneNumber){
        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);
        if(ContextManager.isGoBackOption(phoneNumber)) {
            List<String> accountNumbers = (List<String>) customerData.get("accountNumbers");

            UssdMessageAndData messageAndData = dataMenuTemplate
                    .getAccountNumberListUssdMessageAndDataFromCollection(accountNumbers, customerData, "Account");

            SessionManager.updateExtraDataOfSession(context.getSessionId(), messageAndData.getCustomerData());
            return continueSessionMessage(messageAndData.getMessage());
        }

        // Save the account number to the session manager
        customerData.put("beneficiaryPhoneNumberForDataOthers", phoneNumber);
        SessionManager.updateExtraDataOfSession(context.getSessionId(), customerData);

        List<String> networks = airtimeDataService.getNetworkList();
        UssdMessageAndData messageAndData = airtimeMenuTemplate
                .getNetworkListUssdMessageAndDataFromCollection(networks, customerData, "network");

        SessionManager.updateExtraDataOfSession(context.getSessionId(), messageAndData.getCustomerData());

        return continueSessionMessage(messageAndData.getMessage());

    }

    @UssdSubMenuHandler("*1*7*{accountNumber}*{beneficiaryPhoneNumber}*{benNet}#")
    public String showEnterBeneficiaryNetwork(UssdContext context, String accN, String benPhone, String benNet){
        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);
        if(ContextManager.isGoBackOption(benNet)){
            return continueSessionMessage(airtimeMenuTemplate.getEnterBeneficiaryPhone());
        }

        // Wrong input
        Object selectedNetwork = customerData.get("network".concat(benNet));
        if(selectedNetwork == null){
            return endSessionMessage(airtimeMenuTemplate.getErrorMessage(context));
        }

        String beneficiaryNetwork = String.valueOf(selectedNetwork);
        log.info("BeneficiaryNetwork: {}", beneficiaryNetwork);
        customerData.put("beneficiaryNetworkForDataOthers", beneficiaryNetwork);
        SessionManager.updateExtraDataOfSession(context.getSessionId(), customerData);

        List<String> dataPlansForDataOthers = airtimeDataService.getDataPlansFromNetwork(beneficiaryNetwork);

        UssdMessageAndData messageAndData = dataMenuTemplate
                .getDataPlanListUssdMessageAndDataFromCollection(dataPlansForDataOthers, customerData, "dataPlan");

        SessionManager.updateExtraDataOfSession(context.getSessionId(), messageAndData.getCustomerData());
        return continueSessionMessage(messageAndData.getMessage());

    }

    @UssdSubMenuHandler("*1*7*{accountNumber}*{beneficiaryPhoneNo}*{benNet}*{package}#")
    public String enterPackageForDataOthersOrGoBack(UssdContext context, String accN, String benPhone, String benNet, String p) {
        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);
        if (ContextManager.isGoBackOption(p)) {
            List<String> networks = airtimeDataService.getNetworkList();
            UssdMessageAndData messageAndData = airtimeMenuTemplate
                    .getNetworkListUssdMessageAndDataFromCollection(networks, customerData, "network");

            SessionManager.updateExtraDataOfSession(context.getSessionId(), messageAndData.getCustomerData());

            return continueSessionMessage(messageAndData.getMessage());
        }

        // Wrong input.
        Object selectedPlan = customerData.get("dataPlan".concat(p));
        if(selectedPlan == null){
            return endSessionMessage(dataMenuTemplate.getErrorMessage(context));
        }

        String dataPlan = String.valueOf(customerData.get("dataPlan".concat(p)));
        customerData.put("selectedDataPlanForOthers", dataPlan);
        SessionManager.updateExtraDataOfSession(context.getSessionId(), customerData);

        return continueSessionMessage(dataMenuTemplate.getEnterPINMessageScreen());
    }

    @UssdSubMenuHandler("*1*7*{accountNumber}*{beneficiaryPhoneNo}*{benNet}*{package}*{pin}#")
    public String enterPinOrGoBackForDataOthers(UssdContext context, String accN, String benPhone, String benNet, String p, String pin) {
        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);
        if (ContextManager.isGoBackOption(pin)) {
            List<String> dataPlansForDataOthers = (List<String>) customerData.get("dataPlansForOthers");

            UssdMessageAndData messageAndData = dataMenuTemplate
                    .getDataPlanListUssdMessageAndDataFromCollection(dataPlansForDataOthers, customerData, "dataPlan");

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
            log.info("Body to charge customer for Ussd service for data others: {}", gson.toJson(requestPayload));
            ChargeCustomerResponsePayload responsePayload = fundsTransferService.chargeCustomer(requestPayload, pin);
            if (responsePayload.isStatus()) {
                customerData.put("isCustomerCharged", true);
            }
            log.info("Customer Charge Response for data others: {}", gson.toJson(responsePayload));
        }

        // Prepare payload to buy airtime.
        String beneficiaryNetwork = String.valueOf(customerData.get("beneficiaryNetworkForDataOthers"));
        AirtimeDataBillerData billerData = airtimeDataService.getDataBillerInfoFromNetwork(beneficiaryNetwork);
        String selectedDataPlan = String.valueOf(customerData.get("selectedDataPlanForOthers"));
        String amount = airtimeDataService.getAmountFromConcat(selectedDataPlan);
        String beneficiaryPhoneNumber = String.valueOf(customerData.get("beneficiaryPhoneNumberForDataOthers"));

        AirtimeDataRequestDTO requestDTO = new AirtimeDataRequestDTO();
        requestDTO.setAmount(amount);
        requestDTO.setAccountNumber((String) customerData.get("Account".concat(accN)));
        requestDTO.setBillerId(billerData != null ? billerData.getBillerId() : "");
        requestDTO.setCategoryId(billerData != null ? billerData.getCategoryId() : "");
        requestDTO.setPhoneNumber(Utils.sanitizePhoneNumber(beneficiaryPhoneNumber != null ? beneficiaryPhoneNumber : benPhone));
        requestDTO.setPin(pin);
        log.info("Before buying data for others: {}", new Gson().toJson(requestDTO));
        WayaPayWebResponse webResponse = airtimeDataService.processAirtimeOrDataPayment(requestDTO);
        log.info("Response FROM WayaPay: " + new Gson().toJson(webResponse));

        SessionManager.clearSession(context.getSessionId());

        if(webResponse.isStatus()){
            // Now charge the customer for Buying data for others
            String eventIdOfAggregator = billsPaymentService.getActiveAggregator();
            String acc = String.valueOf(customerData.get("Account".concat(accN)));
            String fee = fundsTransferService.getUserTransactionFee(accN, "10", eventIdOfAggregator);
            UserProfileDetailsData data = (UserProfileDetailsData) customerData.get("customerDetails");
            ChargeCustomerRequestPayload requestPayload = new ChargeCustomerRequestPayload();
            requestPayload.setAmount(fee);
            requestPayload.setEventId(eventIdOfAggregator);
            requestPayload.setSenderName(String.join(" ", data.getFirstName(), data.getMiddleName(), data.getLastName()));
            requestPayload.setPaymentReference(Utils.generatePaymentReference());
            requestPayload.setCustomerAccountNumber(String.valueOf(selectedAccountNumber));
            requestPayload.setTranNarration("Data for others services.");
            log.info("Body to charge the customer for data others: {}", requestPayload);

            ChargeCustomerResponsePayload responsePayload = fundsTransferService.chargeCustomer(requestPayload, pin);
            log.info("Response to charge customer for data others: {}", gson.toJson(responsePayload));

            SessionManager.clearSession(context.getSessionId());
            return new MessageLineBuilder()
                    .addLine(endSessionMessage(webResponse.getMessage() != null ? webResponse.getMessage() : "Successful operation"))
                    .toString();
        }else{
            SessionManager.clearSession(context.getSessionId());
            return new MessageLineBuilder()
                    .addLine(endSessionMessage(webResponse.getMessage()))
                    .toString();
        }
    }

}
