package net.gotev.uploadservicedemo.utils;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper to handle the new Android 6.0 Permissions requests.
 * @author gotev (Aleksandar Gotev)
 */
public class AndroidPermissions {

    private Activity mContext;
    private String[] mRequiredPermissions;
    private List<String> mPermissionsToRequest = new ArrayList<>();

    public AndroidPermissions(Activity context, String... requiredPermissions) {
        mContext = context;
        mRequiredPermissions = requiredPermissions;
    }

    /**
     * Checks if all the required permissions are granted.
     * @return true if all the required permissions are granted, otherwise false
     */
    public boolean checkPermissions() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1)
            return true;

        for (String permission : mRequiredPermissions) {
            if (ContextCompat.checkSelfPermission(mContext, permission) != PackageManager.PERMISSION_GRANTED) {
                mPermissionsToRequest.add(permission);
            }
        }

        if (mPermissionsToRequest.isEmpty()) {
            return true;
        }

        return false;
    }

    /**
     * Requests the missing permissions.
     * The activity from which this method is called has to implement
     * {@link Activity#onRequestPermissionsResult(int, String[], int[])}
     * and then, inside it, it has to call the method
     * {@link AndroidPermissions#areAllRequiredPermissionsGranted(String[], int[])} to check that all the
     * requested permissions are granted by the user
     * @param requestCode request code used by the activity
     */
    public void requestPermissions(int requestCode) {
        String[] request = mPermissionsToRequest.toArray(new String[mPermissionsToRequest.size()]);

        StringBuilder log = new StringBuilder();
        log.append("Requesting permissions:\n");

        for (String permission : request) {
            log.append(permission).append("\n");
        }

        Log.i(getClass().getSimpleName(), log.toString());

        ActivityCompat.requestPermissions(mContext, request, requestCode);
    }

    /**
     * Method to call inside
     * {@link Activity#onRequestPermissionsResult(int, String[], int[])}, to check if the
     * required permissions are granted.
     * @param permissions permissions requested
     * @param grantResults results
     * @return true if all the required permissions are granted, otherwise false
     */
    public boolean areAllRequiredPermissionsGranted(String[] permissions, int[] grantResults) {
        if (permissions == null || permissions.length == 0
                || grantResults == null || grantResults.length == 0) {
            return false;
        }

        LinkedHashMap<String, Integer> perms = new LinkedHashMap<>();

        for (int i = 0; i < permissions.length; i++) {
            if (!perms.containsKey(permissions[i])
                    || (perms.containsKey(permissions[i]) && perms.get(permissions[i]) == PackageManager.PERMISSION_DENIED))
                perms.put(permissions[i], grantResults[i]);
        }

        for (Map.Entry<String, Integer> entry : perms.entrySet()) {
            if (entry.getValue() != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        return true;
    }
}
