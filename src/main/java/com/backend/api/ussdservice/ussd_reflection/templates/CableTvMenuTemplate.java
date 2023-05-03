package com.backend.api.ussdservice.ussd_reflection.templates;

import com.backend.api.ussdservice.pojo.UssdMessageAndData;
import com.backend.api.ussdservice.ussd_reflection.context.ussd.UssdContext;
import com.backend.api.ussdservice.ussd_reflection.context.ussd.message.MenuOptionBuilder;
import com.backend.api.ussdservice.ussd_reflection.context.ussd.message.MessageLineBuilder;
import com.backend.api.ussdservice.ussd_reflection.session.SessionManager;

import java.util.HashMap;
import java.util.List;

public class CableTvMenuTemplate
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

    public UssdMessageAndData showCableTvOptionsScreen(List<String> collection, HashMap<String, Object> customerData, String key){
        MenuOptionBuilder menuOptionBuilder = new MenuOptionBuilder()
                .addOption("", "Select Cable Tv biller ");

        for(int i = 1; i <= collection.size(); i++){
            String cableNumber = collection.get(i - 1);
            String cableKey = key.concat(String.valueOf(i));
            customerData.put(cableKey, cableNumber);
            menuOptionBuilder.addOption(i, cableNumber);
        }

        menuOptionBuilder.addOption(0, "Go back");
        customerData.put("cableTvBillers", collection);

        return UssdMessageAndData.builder()
                .customerData(customerData)
                .message(menuOptionBuilder.toString())
                .build();
    }

    public String getEnterPINScreen(){
       return new MessageLineBuilder()
                .addLine("Enter four (4) digit PIN or press 0 to go back.")
                .toString();
    }
    public String getEnterIUCNumberScreen(HashMap<String, Object> customerData, String cableTv){
        return new MessageLineBuilder()
                .addLine("Enter "+customerData.get("cableTv".concat(cableTv)) +" IUC number or press 0 to go back.")
                .toString();

    }

    public UssdMessageAndData getSubscriptionPackagesForABiller(List<String> collection, HashMap<String, Object> customerData, String key){
        MenuOptionBuilder menuOptionBuilder = new MenuOptionBuilder()
                .addOption("", "Select Package plan");

        for(int i = 1; i <= collection.size(); i++){
            String packageOption = collection.get(i - 1);
            String subscription = key.concat(String.valueOf(i));
            customerData.put(subscription, packageOption);
            menuOptionBuilder.addOption(i, packageOption);
        }

        menuOptionBuilder.addOption(0, "Go back");
        customerData.put("billerSubscription", collection);

        return UssdMessageAndData.builder()
                .customerData(customerData)
                .message(menuOptionBuilder.toString())
                .build();
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
