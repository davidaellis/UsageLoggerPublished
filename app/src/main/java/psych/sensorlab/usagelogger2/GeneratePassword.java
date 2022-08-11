package psych.sensorlab.usagelogger2;

import java.util.Random;

/**
 * Generate a random password
 * added tk 17.06.2022
 */
public class GeneratePassword {

    public static final String DATA = "123456789ABCDEFGHKMNOPQRSTUVWXYZabcdefghkmnopqrstuvwxyz!$%&@#?";
    public static Random RANDOM = new Random();
    public static String randomString(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(DATA.charAt(RANDOM.nextInt(DATA.length())));
        }
        return sb.toString();
    }
}
