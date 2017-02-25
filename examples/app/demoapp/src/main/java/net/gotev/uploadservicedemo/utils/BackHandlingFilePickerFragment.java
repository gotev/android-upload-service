package net.gotev.uploadservicedemo.utils;

import com.nononsenseapps.filepicker.FilePickerFragment;

import java.io.File;

/**
 * @author Aleksandar Gotev
 */
public class BackHandlingFilePickerFragment extends FilePickerFragment {

    /**
     * For consistency, the top level the back button checks against should be the start path.
     * But it will fall back on /.
     */
    public File getBackTop() {
        if (getArguments().containsKey(KEY_START_PATH)) {
            String keyStartPath = getArguments().getString(KEY_START_PATH);

            if (keyStartPath == null)
                return new File("/");

            return getPath(keyStartPath);
        } else {
            return new File("/");
        }
    }

    /**
     *
     * @return true if the current path is the startpath or /
     */
    public boolean isBackTop() {
        return 0 == compareFiles(mCurrentPath, getBackTop()) || 0 == compareFiles(mCurrentPath, new File("/"));
    }

    /**
     * Go up on level, same as pressing on "..".
     */
    public void goUp() {
        mCurrentPath = getParent(mCurrentPath);
        mCheckedItems.clear();
        mCheckedVisibleViewHolders.clear();
        refresh(mCurrentPath);
    }
}
