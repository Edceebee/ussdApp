package com.backend.api.ussdservice.ussd_reflection.menuHandlers;

import com.backend.api.ussdservice.ussd_reflection.web.pojo.response.*;
import com.google.gson.Gson;
import com.backend.api.ussdservice.pojo.UssdMessageAndData;
import com.backend.api.ussdservice.ussd_reflection.annotation.UssdMenuHandler;
import com.backend.api.ussdservice.ussd_reflection.annotation.UssdSubMenuHandler;
import com.backend.api.ussdservice.ussd_reflection.constants.ChargeAmounts;
import com.backend.api.ussdservice.ussd_reflection.constants.ChargeEventId;
import com.backend.api.ussdservice.ussd_reflection.context.ContextManager;
import com.backend.api.ussdservice.ussd_reflection.context.Item;
import com.backend.api.ussdservice.ussd_reflection.context.ussd.UssdContext;
import com.backend.api.ussdservice.ussd_reflection.context.ussd.message.MessageLineBuilder;
import com.backend.api.ussdservice.ussd_reflection.session.SessionManager;
import com.backend.api.ussdservice.ussd_reflection.templates.CableTvMenuTemplate;
import com.backend.api.ussdservice.ussd_reflection.templates.HomeMenuTemplate;
import com.backend.api.ussdservice.ussd_reflection.utils.Utils;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.WayaPayWebResponse;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.dto.CableTvRequestDTO;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.request.ChargeCustomerRequestPayload;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.request.ValidateBillDataParam;
import com.backend.api.ussdservice.ussd_reflection.web.pojo.request.ValidateBillRequestPayload;
import com.backend.api.ussdservice.ussd_reflection.web.services.AccountService;
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
public class CableTvMenuHandler {

    @Autowired
    private BillsPaymentService billsPaymentService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private FundsTransferService fundsTransferService;

    @Autowired
    private ChargeEventId chargeEventId;


    private final CableTvMenuTemplate cableTvMenuTemplate = new CableTvMenuTemplate();
    private final HomeMenuTemplate homeMenuTemplate = new HomeMenuTemplate();

    private static final Gson gson = new Gson();

    @UssdSubMenuHandler("*1*8#")
    public String payForCableTv(UssdContext context) {
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

        UssdMessageAndData messageAndData = cableTvMenuTemplate
                .getAccountNumberListUssdMessageAndDataFromCollection(accountNumbers, customerData, "Account");

        SessionManager.updateExtraDataOfSession(context.getSessionId(), messageAndData.getCustomerData());
        return continueSessionMessage(messageAndData.getMessage());
    }

    @UssdSubMenuHandler("*1*8*{accountNumber}#")
    public String selectAccountNumber(UssdContext context, String accountNumber) {
        String goBack = ContextManager.getItem(Item.DEFAULT_USSD_GO_BACK_OPTION, String.class);
        if(accountNumber.equalsIgnoreCase(goBack)){
            return continueSessionMessage(homeMenuTemplate.getExistingCustomerHomeMenuListScreen());
        }

        // Wrong input.
        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);
        Object selectedAccountNumber = customerData.get("Account".concat(accountNumber));
        if(selectedAccountNumber == null){
            return endSessionMessage(cableTvMenuTemplate.getErrorMessage(context));
        }

        List<CableTvResponseData> list = billsPaymentService.getCableTvObjects();
        customerData.put("billerObjects", list);

        List<String> cableTvBillers = billsPaymentService.getCableTvBillerNames();

        customerData.put("selectedAccountNumber", String.valueOf(selectedAccountNumber));
        UssdMessageAndData messageAndData = cableTvMenuTemplate
                .showCableTvOptionsScreen(cableTvBillers, customerData, "cableTv");

        SessionManager.updateExtraDataOfSession(context.getSessionId(), messageAndData.getCustomerData());
        return continueSessionMessage(messageAndData.getMessage());
    }

    @UssdSubMenuHandler("*1*8*{accountNumber}*{cableTv}#")
    public String enterCodeForCableTvBiller(UssdContext context, String accountNumber, String cableTv) {

        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);

        if (ContextManager.isGoBackOption(cableTv)) {
            List<String> accountNumbers = (List<String>) customerData.get("accountNumbers");

            UssdMessageAndData messageAndData = cableTvMenuTemplate
                    .getAccountNumberListUssdMessageAndDataFromCollection(accountNumbers, customerData, "Account");

            SessionManager.updateExtraDataOfSession(context.getSessionId(), messageAndData.getCustomerData());
            return continueSessionMessage(messageAndData.getMessage());
        }

        // Wrong input
        Object selectedCableTv = customerData.get("cableTv".concat(cableTv));
        if(selectedCableTv == null){
            return endSessionMessage(cableTvMenuTemplate.getErrorMessage(context));
        }

        List<CableTvResponseData> list = (List<CableTvResponseData>) customerData.get("billerObjects");
        CableTvResponseData selectedObject = list.get(Integer.parseInt(cableTv) - 1);
        String selectedCableTvBiller = String.valueOf(selectedCableTv);
        customerData.put("selectedCableTvBiller", selectedCableTvBiller);
        customerData.put("selectedObject", selectedObject);
        SessionManager.updateExtraDataOfSession(context.getSessionId(), customerData);
        return continueSessionMessage(cableTvMenuTemplate.getEnterIUCNumberScreen(customerData, cableTv));
    }

    @UssdSubMenuHandler("*1*8*{accountNumber}*{cableTv}*{iUcInput}#")
    public String enterIUCNumberOrGoBack(UssdContext context, String accountNumber, String cableTv, String iucInput){
        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);
        if(ContextManager.isGoBackOption(iucInput)){
            List<String> cableTvBillers = (List<String>) customerData.get("cableTvBillers");
            UssdMessageAndData messageAndData = cableTvMenuTemplate
                    .showCableTvOptionsScreen(cableTvBillers, customerData, "cableTv");

            SessionManager.updateExtraDataOfSession(context.getSessionId(), messageAndData.getCustomerData());
            return continueSessionMessage(messageAndData.getMessage());
        }

        String selectedBiller = String.valueOf(customerData.get("selectedCableTvBiller"));
        List<String> subscriptions = billsPaymentService.getCableTvPackageNameAndAmountConcatenated(selectedBiller);
        if(subscriptions != null){
            UssdMessageAndData messageAndData = cableTvMenuTemplate
                    .getSubscriptionPackagesForABiller(subscriptions, customerData, "subscription");

            SessionManager.updateExtraDataOfSession(context.getSessionId(), messageAndData.getCustomerData());
            return continueSessionMessage(messageAndData.getMessage());
        }else{
            SessionManager.clearSession(context.getSessionId());
            return endSessionMessage(new MessageLineBuilder()
                    .addLine("Unable to fetch packages for Biller. Please try again later.")
                    .addLine("Thank you.")
                    .toString());
        }
    }

    @UssdSubMenuHandler("*1*8*{accountNumber}*{cableTv}*{iUcInput}*{package}#")
    public String enterPackageOrGoBack(UssdContext context, String accountNumber, String cableTv, String iUcInput, String p){
        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);
        if(ContextManager.isGoBackOption(p)){
            return continueSessionMessage(cableTvMenuTemplate.getEnterIUCNumberScreen(customerData, cableTv));
        }

        // Wrong input
        Object selectedPackage = customerData.get("subscription".concat(p));
        if(selectedPackage == null){
            return endSessionMessage(cableTvMenuTemplate.getErrorMessage(context));
        }

        // Save the package.
        customerData.put("cablePackage", p);
        SessionManager.updateExtraDataOfSession(context.getSessionId(), customerData);

        return continueSessionMessage(cableTvMenuTemplate.getEnterPINScreen());
    }

    @UssdSubMenuHandler("*1*8*{accountNumber}*{cableTv}*{iUcInput}*{package}*{pin}#")
    public String enterPinOrGoBack(UssdContext context, String accountNumber, String cableTv, String iucInput, String pack, String pin){
        HashMap<String, Object> customerData = SessionManager.getExtraDataOfSession(context.getSessionId(), HashMap.class);

        if(ContextManager.isGoBackOption(pin)){
            String selectedBiller = String.valueOf(customerData.get("selectedCableTvBiller"));
            List<String> subscriptions = billsPaymentService.getCableTvPackageNameAndAmountConcatenated(selectedBiller);
            if(subscriptions != null){
                UssdMessageAndData messageAndData = cableTvMenuTemplate
                        .getSubscriptionPackagesForABiller(subscriptions, customerData, "subscription");

                SessionManager.updateExtraDataOfSession(context.getSessionId(), messageAndData.getCustomerData());
                return continueSessionMessage(messageAndData.getMessage());
            }else{
                SessionManager.clearSession(context.getSessionId());
                return endSessionMessage(new MessageLineBuilder()
                        .addLine("Unable to fetch packages for Biller. Please try again later.")
                        .addLine("Thank you.")
                        .toString());
            }
        }
        else{
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
                log.info("Body to charge customer for Ussd service for cable tv: {}", gson.toJson(requestPayload));
                ChargeCustomerResponsePayload responsePayload = fundsTransferService.chargeCustomer(requestPayload, pin);
                if (responsePayload.isStatus()) {
                    customerData.put("isCustomerCharged", true);
                }
                log.info("Customer Charge Response in cable tv: {}", gson.toJson(responsePayload));
            }

            // Prepare the request to service
            CableTvResponseData selectedObject = (CableTvResponseData) customerData.get("selectedObject");
            String packg = String.valueOf(customerData.get("subscription".concat(pack)));
            String amount = billsPaymentService.getAmountFromConcat(packg);
            String packageName = billsPaymentService.getCableTvPackageNameFromConcat(packg);
            String billerId = selectedObject.getBillerId();
            String categoryId = selectedObject.getCategoryId();
            CableTvRequestDTO requestDTO = new CableTvRequestDTO();
            requestDTO.setPin(pin);
            requestDTO.setAmount(String.valueOf(amount));
            requestDTO.setBillerId(billerId);
            requestDTO.setCategoryId(categoryId);
            requestDTO.setAccountNumber(String.valueOf(customerData.get("Account".concat(accountNumber))));
            requestDTO.setPhoneNumber(context.getMobileNumber());

            ValidateBillRequestPayload validateBillRequestPayload = new ValidateBillRequestPayload();
            ValidateBillDataParam dataParam = new ValidateBillDataParam();
            dataParam.setName("amount");
            dataParam.setValue(amount);

            ValidateBillDataParam dataParam1 = new ValidateBillDataParam();
            dataParam1.setName("product_monthsPaidFor");
            dataParam1.setValue("1");

            ValidateBillDataParam dataParam2 = new ValidateBillDataParam();
            dataParam2.setName("smartcard_number");
            dataParam2.setValue(iucInput);

            ValidateBillDataParam dataParam3 = new ValidateBillDataParam();
            dataParam3.setName("total_amount");
            dataParam3.setValue(amount);

            ValidateBillDataParam dataParam4 = new ValidateBillDataParam();
            dataParam4.setName("plan");
            dataParam4.setValue(billsPaymentService.getSubItemByPackageName(packageName, selectedObject.getBillerName()).getId());

            validateBillRequestPayload.setAmount(amount);
            validateBillRequestPayload.setBillerId(billerId);
            validateBillRequestPayload.setCategoryId(categoryId);
            validateBillRequestPayload.setData(List.of(dataParam1, dataParam2, dataParam3, dataParam4));
            validateBillRequestPayload.setSourceWalletAccountNumber(String.valueOf(customerData.get("Account".concat(accountNumber))));

            log.info("Validate Bill Request: {}", gson.toJson(validateBillRequestPayload));
            ValidateBillResponsePayload validation = billsPaymentService.validateBill(validateBillRequestPayload, pin);

            if(validation != null){
                if(!validation.isStatus()){
                    String message = validation.getMessage() == null ? new MessageLineBuilder()
                            .addLine("Unable to perform validation for request.")
                            .addLine("Please contact administrator or support and try again later")
                            .addLine("Thank you.")
                            .toString() : validation.getMessage();
                    SessionManager.clearSession(context.getSessionId());
                    return endSessionMessage(message);
                }
            }else{
                SessionManager.clearSession(context.getSessionId());
                return endSessionMessage(new MessageLineBuilder()
                        .addLine("Validation of IUC number failed.")
                        .addLine("System was unable to validate your request.")
                        .addLine("Please try again later. Thank you")
                        .toString());
            }

            log.info("CableTv Payment request to Service: {}", gson.toJson(requestDTO));

            requestDTO.setPlan(dataParam4.getValue());
            WayaPayWebResponse response = billsPaymentService.processCablePayment(requestDTO);
            if(response != null){
                if(response.isStatus()){
                    // Now charge the customer for Cable TV service.
                    String eventIdOfAggregator = billsPaymentService.getActiveAggregator();
                    String accN = String.valueOf(customerData.get("Account".concat(accountNumber)));
                    String fee = fundsTransferService.getUserTransactionFee(accN, "10", eventIdOfAggregator);
                    UserProfileDetailsData data = (UserProfileDetailsData) customerData.get("customerDetails");
                    ChargeCustomerRequestPayload requestPayload = new ChargeCustomerRequestPayload();
                    requestPayload.setAmount(fee);
                    requestPayload.setEventId(eventIdOfAggregator);
                    requestPayload.setSenderName(String.join(" ", data.getFirstName(), data.getMiddleName(), data.getLastName()));
                    requestPayload.setPaymentReference(Utils.generatePaymentReference());
                    requestPayload.setCustomerAccountNumber(String.valueOf(selectedAccountNumber));
                    requestPayload.setTranNarration("Cable tv service charges");
                    log.info("Body to charge the customer for cable tv service: {}", requestPayload);

                    ChargeCustomerResponsePayload responsePayload = fundsTransferService.chargeCustomer(requestPayload, pin);
                    log.info("Response to charge customer for cable tv: {}", gson.toJson(responsePayload));

                    SessionManager.clearSession(context.getSessionId());
                    return endSessionMessage(new MessageLineBuilder()
                            .addLine(String.format("Bill payment for package %s successful", packg))
                            .addLine("Thank you.")
                            .toString());
                }
                else{
                    SessionManager.clearSession(context.getSessionId());
                    return endSessionMessage(new MessageLineBuilder()
                            .addLine(response.getMessage())
                            .toString());
                }
            }else{
                String message = new MessageLineBuilder()
                                .addLine("Sorry, service could not process your request at the moment.")
                                .addLine("Reason: System failed to respond")
                                .toString();
                SessionManager.clearSession(context.getSessionId());
                return endSessionMessage(message);
            }
        }
    }

}
