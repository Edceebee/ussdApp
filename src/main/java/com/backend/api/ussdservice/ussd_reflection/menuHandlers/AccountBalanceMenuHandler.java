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
import com.backend.api.ussdservice.ussd_reflection.templates.AccountBalanceMenuTemplate;
import com.backend.api.ussdservice.ussd_reflection.templates.HomeMenuTemplate;
import com.backend.api.ussdservice.ussd_reflection.utils.Utils;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.dto.AccountBalanceResponseDTO;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.request.ChargeCustomerRequestPayload;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.response.AccountNumbersResponse;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.response.ChargeCustomerResponsePayload;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.response.UserProfileDetailsData;
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
public class AccountBalanceMenuHandler {


    @Autowired
    private AccountService accountService;

    @Autowired
    private FundsTransferService fundsTransferService;

    @Autowired
    private ChargeEventId chargeEventId;

    private final AccountBalanceMenuTemplate accountBalanceMenuTemplate = new AccountBalanceMenuTemplate();
    private final HomeMenuTemplate homeMenuTemplate = new HomeMenuTemplate();
    private static final Gson gson = new Gson();

    @UssdSubMenuHandler("*1*10#")
    public String checkAccountBalance(UssdContext context) {
        return continueSessionMessage(accountBalanceMenuTemplate.getAccountBalanceScreen());
    }

    @UssdSubMenuHandler("*1*10*{pin}#")
    public String userEntersPinForAccountBalance(UssdContext context, String pin) {
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

        UssdMessageAndData messageAndData = accountBalanceMenuTemplate
                .getAccountNumberListUssdMessageAndDataFromCollection(accountNumbers, customerData, "Account");

        SessionManager.updateExtraDataOfSession(context.getSessionId(), messageAndData.getCustomerData());

        return continueSessionMessage(messageAndData.getMessage());
    }

    @UssdSubMenuHandler("*1*10*{pin}*{account}#")
    public String enterAccountScreenOrGoBack(UssdContext context, String pin, String account){
        String goBackOption = ContextManager.getItem(Item.DEFAULT_USSD_GO_BACK_OPTION, String.class);
        if(account.equalsIgnoreCase(goBackOption)) {
            return continueSessionMessage(accountBalanceMenuTemplate.getEnterPinOrGoBackScreen());
        }

        // Wrong input
        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);
        Object selectedAccountNumber = customerData.get("Account".concat(account));
        if(selectedAccountNumber == null){
            return endSessionMessage(accountBalanceMenuTemplate.getErrorMessage(context));
        }

        // Charge the customer
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
            log.info("Body to charge customer for Ussd service in Account Balance: {}", gson.toJson(requestPayload));
            ChargeCustomerResponsePayload responsePayload = fundsTransferService.chargeCustomer(requestPayload, pin);
            if (responsePayload != null && responsePayload.isStatus()) {
                customerData.put("isCustomerCharged", true);
            }
            log.info("Customer Charge Response: {}", gson.toJson(responsePayload));
        }

        customerData.put("selectedAccountNumber", String.valueOf(selectedAccountNumber));
        SessionManager.updateExtraDataOfSession(context.getSessionId(), customerData);
        return continueSessionMessage(accountBalanceMenuTemplate.getServiceChargeScreen());
    }

    @UssdSubMenuHandler("*1*10*{pin}*{account}*{input}#")
    public String selectIfCustomerWishToContinue(UssdContext context, String pin, String account, String input){
        String goBackOption = ContextManager.getItem(Item.DEFAULT_USSD_GO_BACK_OPTION, String.class);
        if(input.equalsIgnoreCase(goBackOption)){
            return continueSessionMessage(userEntersPinForAccountBalance(context, pin));
        }

        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);
        customerData.put("accountBalanceAccountNumber", account);

        String selectedAccountNumber = String.valueOf(customerData.get("selectedAccountNumber"));

        AccountBalanceResponseDTO webResponse = accountService.getCustomerAccountBalanceByAccountNumber(selectedAccountNumber, pin);
        SessionManager.clearSession(context.getSessionId());
        String message;
        if(webResponse != null && webResponse.isStatus()){
            // Call the fee endpoint to charge the customer for the service.
            String fee = fundsTransferService.getUserTransactionFee(selectedAccountNumber, "10", chargeEventId.getTRANSFER_AND_TRANSACTION_FEE_EVENT_ID());
            UserProfileDetailsData data = (UserProfileDetailsData) customerData.get("customerDetails");
            ChargeCustomerRequestPayload requestPayload = new ChargeCustomerRequestPayload();
            requestPayload.setAmount(fee);
            requestPayload.setEventId(chargeEventId.getTRANSFER_AND_TRANSACTION_FEE_EVENT_ID());
            requestPayload.setSenderName(String.join(" ", data.getFirstName(), data.getMiddleName(), data.getLastName()));
            requestPayload.setPaymentReference(Utils.generatePaymentReference());
            requestPayload.setCustomerAccountNumber(String.valueOf(selectedAccountNumber));
            requestPayload.setTranNarration("Account balance charges");
            log.info("Body to charge the customer for account balance: {}", requestPayload);

            ChargeCustomerResponsePayload responsePayload = fundsTransferService.chargeCustomer(requestPayload, pin);
            log.info("Response to charge customer for checking balance: {}", gson.toJson(responsePayload));

            String accountBalance = String.valueOf(webResponse.getData().getBalance());
            message = accountBalanceMenuTemplate.getAccountBalanceCheckMessage(selectedAccountNumber, accountBalance);
        }else{
            message = webResponse.getMessage();
        }
        SessionManager.clearSession(context.getSessionId());
        return endSessionMessage(message);
    }


    @UssdSubMenuHandler("*1*10*0#")
    public String goBackToGoBack(UssdContext context){
        return continueSessionMessage(homeMenuTemplate.getExistingCustomerHomeMenuListScreen());
    }
}
