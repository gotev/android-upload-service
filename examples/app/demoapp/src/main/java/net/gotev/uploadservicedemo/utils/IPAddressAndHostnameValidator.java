package net.gotev.uploadservicedemo.utils;

import java.util.regex.Pattern;

/**
 * RegEx patterns got from http://stackoverflow.com/a/106223
 *
 * @author Aleksandar Gotev
 */

public class IPAddressAndHostnameValidator {

    private Pattern ipAddressPattern;
    private Pattern hostnamePattern;

    private static final String VALID_IP_ADDRESS_REGEX =
            "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$";

    private static final String VALID_HOSTNAME_REGEX =
            "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$";

    public IPAddressAndHostnameValidator() {
        ipAddressPattern = Pattern.compile(VALID_IP_ADDRESS_REGEX);
        hostnamePattern = Pattern.compile(VALID_HOSTNAME_REGEX);
    }

    public boolean isValidIPAddress(final String ip){
        if (ip == null || ip.isEmpty())
            return false;

        return ipAddressPattern.matcher(ip).matches();
    }

    public boolean isValidHostname(final String hostname) {
        if (hostname == null || hostname.isEmpty())
            return false;

        return hostnamePattern.matcher(hostname).matches();
    }

    public boolean isValidIPorHostname(final String rawString) {
        return isValidIPAddress(rawString) || isValidHostname(rawString);
    }
}
