package net.gotev.uploadservice;

/**
 * Contains all the placeholders that is possible to use in the notification text strings.
 * @author Aleksandar Gotev
 */
public class Placeholders {

    /**
     * Placeholder to display the total elapsed upload time in minutes and seconds.
     * E.g.: 34s, 4m 33s, 45m 21s
     */
    public static final String ELAPSED_TIME = "[[ELAPSED_TIME]]";

    /**
     * Placeholder to display the average upload rate. E.g.: 6 Mbit/s, 634 Kbit/s, 232 bit/s
     */
    public static final String UPLOAD_RATE = "[[UPLOAD_RATE]]";

    /**
     * Placeholder to display the integer progress percent from 0 to 100. E.g.: 75%
     */
    public static final String PROGRESS = "[[PROGRESS]]";
}
