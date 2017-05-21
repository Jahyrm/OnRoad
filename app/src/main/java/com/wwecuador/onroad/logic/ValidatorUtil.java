package com.wwecuador.onroad.logic;

/**
 * Created by Jahyr on 17/5/2017.
 */
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValidatorUtil {
    private static final String PATTERN_EMAIL = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
            + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    /**
     * Validate given email with regular expression.
     *
     * @param email
     *            email for validation
     * @return true valid email, otherwise false
     */
    public static boolean validateEmail(String email) {

        // Compiles the given regular expression into a pattern.
        Pattern pattern = Pattern.compile(PATTERN_EMAIL);

        // Match the given input against this pattern
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();

    }

    public static boolean validatePass(String pass) {
        char caracterActual;
        int numMayus=0, numMin=0, numNumbers=0;
        for(int i=0; i<pass.length(); i++){
            caracterActual = pass.charAt(i);
            String passValue = String.valueOf(caracterActual);

            if (passValue.matches("[A-Z]")) {

                numMayus++;

            } else if (passValue.matches("[a-z]")) {

                numMin++;

            } else if (passValue.matches("[0-9]")) {

                numNumbers++;
            }
        }

        if(numMayus>=1 && numMin >=1 && numNumbers >=1){
            return true;
        } else {
            return false;
        }
    }
}
