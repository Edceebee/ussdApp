package com.backend.api.ussdservice.ussd_reflection.menuHandlers;

import com.backend.api.ussdservice.ussd_reflection.annotation.UssdMenuHandler;
import com.backend.api.ussdservice.ussd_reflection.annotation.UssdSubMenuHandler;
import com.google.gson.Gson;
import com.backend.api.ussdservice.pojo.UssdMessageAndData;
import com.backend.api.ussdservice.ussd_reflection.constants.ChargeAmounts;
import com.backend.api.ussdservice.ussd_reflection.constants.ChargeEventId;
import com.backend.api.ussdservice.ussd_reflection.context.ContextManager;
import com.backend.api.ussdservice.ussd_reflection.context.ussd.UssdContext;
import com.backend.api.ussdservice.ussd_reflection.context.ussd.message.MessageLineBuilder;
import com.backend.api.ussdservice.ussd_reflection.session.SessionManager;
import com.backend.api.ussdservice.ussd_reflection.templates.BettingMenuTemplate;
import com.backend.api.ussdservice.ussd_reflection.templates.HomeMenuTemplate;
import com.backend.api.ussdservice.ussd_reflection.utils.Utils;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.request.ChargeCustomerRequestPayload;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.response.ChargeCustomerResponsePayload;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.response.UserProfileDetailsData;
import com.backend.api.ussdservice.ussd_reflection.web.services.AccountService;
import com.backend.api.ussdservice.ussd_reflection.web.services.BillsPaymentService;
import com.backend.api.ussdservice.ussd_reflection.web.services.FundsTransferService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;

import static com.backend.api.ussdservice.ussd_reflection.session.SessionManager.continueSessionMessage;
import static com.backend.api.ussdservice.ussd_reflection.session.SessionManager.endSessionMessage;

@UssdMenuHandler
@Slf4j
public class BettingMenuHandler {

    @Autowired
    private AccountService accountService;

    @Autowired
    private BillsPaymentService billsPaymentService;

    @Autowired
    private FundsTransferService fundsTransferService;

    @Autowired
    private ChargeEventId chargeEventId;


    private final BettingMenuTemplate bettingMenuTemplate = new BettingMenuTemplate();
    private final HomeMenuTemplate homeMenuTemplate = new HomeMenuTemplate();
    private static final Gson gson = new Gson();

    @UssdSubMenuHandler("*1*9#")
    public String payForBetting(UssdContext context) {
        return endSessionMessage(new MessageLineBuilder()
                .addLine("This service is currently unavailable")
                .toString());
//        List<String> accountNumbers = new ArrayList<>();
//        try{
//            accountNumbers = accountService.getAccountNumbersForCustomer(context.getMobileNumber())
//                    .getData().stream()
//                    .map(AccountNumbersResponse::getAccountNo)
//                    .collect(Collectors.toList());
//        }catch (Exception e){
//            SessionManager.clearSession(context.getSessionId());
//            return endSessionMessage(new MessageLineBuilder()
//                    .addLine("Sorry you don't have any account associated with your profile though you are a registered customer.")
//                    .addLine("Please contact administrator or support.")
//                    .addLine("Thank you.")
//                    .toString());
//        }
//
//        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);;
//        customerData.put("customerType", CustomerTypes.EXISTING.name());
//
//        UssdMessageAndData messageAndData = bettingMenuTemplate
//                .getAccountNumberListUssdMessageAndDataFromCollection(accountNumbers, customerData, "Account");
//
//        SessionManager.updateExtraDataOfSession(context.getSessionId(), messageAndData.getCustomerData());
//        return continueSessionMessage(messageAndData.getMessage());
    }

    @UssdSubMenuHandler("*1*9*{accountNumber}#")
    public String selectAccountNumberForBetting(UssdContext context, String account) {
        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);

        if(ContextManager.isGoBackOption(account)){
            return continueSessionMessage(homeMenuTemplate.getExistingCustomerHomeMenuListScreen());
        }

        // wrong input
        Object selectedAccountNumber = customerData.get("selectedAccountNumber");
        if(selectedAccountNumber == null){
            return bettingMenuTemplate.getErrorMessage(context);
        }

        String selectedCustomerAccountNumber = String.valueOf(selectedAccountNumber);
        customerData.put("selectedAccountNumber", selectedCustomerAccountNumber);

        // TODO: 30/03/2023 endpoint to get betting billers
        List<String> betBillers = List.of("Bet9ja", "Access bet", "Sporty bet", "Kings bet");

        UssdMessageAndData messageAndData = bettingMenuTemplate
                .getBettingBillerListUssdMessageAndDataFromCollection(betBillers, customerData, "betting");

        SessionManager.updateExtraDataOfSession(context.getSessionId(), messageAndData.getCustomerData());

        return continueSessionMessage(messageAndData.getMessage());
    }

    @UssdSubMenuHandler("*1*9*{accountNumber}*{bet}#")
    public String selectBetForRecharge(UssdContext context, String account, String bet) {
        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);

        if(ContextManager.isGoBackOption(bet)){
            List<String> accountNumbers = (List<String>) customerData.get("accountNumbers");
            UssdMessageAndData messageAndData = bettingMenuTemplate
                    .getAccountNumberListUssdMessageAndDataFromCollection(accountNumbers, customerData, "Account");

            SessionManager.updateExtraDataOfSession(context.getSessionId(), messageAndData.getCustomerData());
            return continueSessionMessage(messageAndData.getMessage());
        }

        // Wrong input
        Object selectedBetBiller = customerData.get("betting".concat(bet));
        if(selectedBetBiller == null){
            return endSessionMessage(bettingMenuTemplate.getErrorMessage(context));
        }

        // Save bet biller saved and navigate to the amount screen.
        customerData.put("selectedBillerForBetting", String.valueOf(selectedBetBiller));
        SessionManager.updateExtraDataOfSession(context.getSessionId(), customerData);

        return continueSessionMessage(bettingMenuTemplate.getEnterAmountForBetting());
    }

    @UssdSubMenuHandler("*1*9*{accountNumber}*{bet}*{amount}#")
    public String selectAmountForRecharge(UssdContext context, String account, String bet, String amount) {
        if(ContextManager.isGoBackOption(amount)){
            return continueSessionMessage(selectBetForRecharge(context, account, bet));
        }

        // Save the amount and navigate to the enter PIN screen.
        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);
        customerData.put("amountForBetting", amount);
        SessionManager.updateExtraDataOfSession(context.getSessionId(), customerData);

        return continueSessionMessage(bettingMenuTemplate.getEnterPINScreen());
    }

    @UssdSubMenuHandler("*1*9*{accountNumber}*{bet}*{amount}*{pin}#")
    public String enterCodeForBetting(UssdContext context, String account, String bet, String amount, String pin) {
        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);
        if(ContextManager.isGoBackOption(pin)){
            return continueSessionMessage(bettingMenuTemplate.getEnterAmountForBetting());
        }
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
            log.info("Body to charge customer for Ussd service for betting: {}", gson.toJson(requestPayload));
            ChargeCustomerResponsePayload responsePayload = fundsTransferService.chargeCustomer(requestPayload, pin);
            if (responsePayload.isStatus()) {
                customerData.put("isCustomerCharged", true);
            }
            log.info("Customer Charge Response for betting: {}", gson.toJson(responsePayload));
        }

        String message = new MessageLineBuilder()
                    .addLine("Recharge successful")
                    .toString();
        SessionManager.clearSession(context.getSessionId());
         return endSessionMessage(message);
    }

}
