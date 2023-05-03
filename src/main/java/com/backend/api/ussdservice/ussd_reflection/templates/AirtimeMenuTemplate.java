package com.backend.api.ussdservice.ussd_reflection.templates;

import com.backend.api.ussdservice.pojo.UssdMessageAndData;
import com.backend.api.ussdservice.ussd_reflection.context.ussd.UssdContext;
import com.backend.api.ussdservice.ussd_reflection.context.ussd.message.MenuOptionBuilder;
import com.backend.api.ussdservice.ussd_reflection.context.ussd.message.MessageLineBuilder;
import com.backend.api.ussdservice.ussd_reflection.session.SessionManager;

import java.util.HashMap;
import java.util.List;

public class AirtimeMenuTemplate
{
    public UssdMessageAndData getAccountNumberListUssdMessageAndDataFromCollection(List<String> collection, HashMap<String, Object> customerData, String key){
        MenuOptionBuilder menuOptionBuilder = new MenuOptionBuilder()
                .addOption("", "Select account");

        for(int i = 1; i <= collection.size(); i++){
            String accountNumber = collection.get(i - 1);
            String accountKey = key.concat(String.valueOf(i));
            customerData.put(accountKey, accountNumber);
            menuOptionBuilder.addOption(i, accountNumber);
        }

        menuOptionBuilder.addOption(0, "Go back");
        customerData.put("accountNumbers", collection);

        return UssdMessageAndData.builder()
                .message(menuOptionBuilder.toString())
                .customerData(customerData)
                .build();
    }

    public UssdMessageAndData getNetworkListUssdMessageAndDataFromCollection(List<String> collection, HashMap<String, Object> customerData, String key){
        MenuOptionBuilder menuOptionBuilder = new MenuOptionBuilder()
                .addOption("", "Select beneficiary network");

        for(int i = 1; i <= collection.size(); i++){
            String networkNumber = collection.get(i - 1);
            String networkKey = key.concat(String.valueOf(i));
            customerData.put(networkKey, networkNumber);
            menuOptionBuilder.addOption(i, networkNumber);
        }

        menuOptionBuilder.addOption(0, "Go back");
        customerData.put("networks", collection);

        return UssdMessageAndData.builder()
                .message(menuOptionBuilder.toString())
                .customerData(customerData)
                .build();
    }
    public String getEnterBeneficiaryPhone(){
        return new MessageLineBuilder()
                .addLine("Enter beneficiary phone number")
                .toString();
    }

    public String getEnterPinForAirtime(){
        return new MessageLineBuilder()
                .addLine("Enter your four (4) digit PIN or press 0 to go back")
                .toString();
    }

    public String getEnterAmountToRecharge(){
        return new MessageLineBuilder()
                .addLine("Enter amount to recharge or press 0 to go back")
                .toString();
    }

    public String getErrorMessage(UssdContext context){
        String message = new MessageLineBuilder()
                .addLine("Oops!. You entered an invalid input. Please try again")
                .addLine("Thank you")
                .toString();
        SessionManager.clearSession(context.getSessionId());
        return message;
    }
}
