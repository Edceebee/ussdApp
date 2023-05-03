package com.backend.api.ussdservice.ussd_reflection.utils;


public class Utils
{
    public static String generatePaymentReference(){
        String[] numbers = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};
        String[] alphabets = new String[]{
                "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K",
                "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V",
                "W", "X", "Y", "Z"
            };
        StringBuilder builder = new StringBuilder("ATRF");
        for(int i = 0; i < 10; i++){
            int alphaRandNumber = (int)(Math.random() * 10) + 14;
            int numRandNumber = (int)(Math.random() * 10);
            String combined = numbers[numRandNumber].concat(alphabets[alphaRandNumber]);
            builder.append(combined);
        }
        return builder.toString();
    }

    public static String sanitizePhoneNumber(String phoneNumber){
        if(phoneNumber.startsWith("+234") && phoneNumber.trim().length() == 14){
            return phoneNumber;
        }
        if (phoneNumber.startsWith("0") && phoneNumber.length() == 11){
            phoneNumber =  "+234".concat(phoneNumber.substring(1));
        }
        return phoneNumber;
    }
}
