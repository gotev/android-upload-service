package net.gotev.uploadservice.ftp;

import java.util.Locale;

/**
 * Utility class to work with UNIX permissions.
 *
 * @author Aleksandar Gotev
 */
public class UnixPermissions {

    private boolean ownerCanRead;
    private boolean ownerCanWrite;
    private boolean ownerCanExecute;

    private boolean groupCanRead;
    private boolean groupCanWrite;
    private boolean groupCanExecute;

    private boolean worldCanRead;
    private boolean worldCanWrite;
    private boolean worldCanExecute;

    /**
     * Creates a new UNIX permissions object.
     * The default permissions set is 644 (Owner can read and write. Group and World can only read)
     */
    public UnixPermissions() {
        ownerCanRead = true;
        ownerCanWrite = true;
        groupCanRead = true;
        worldCanRead = true;
    }

    /**
     * Creates a new UNIX permissions object.
     * @param string UNIX permissions string (e.g. 700, 400, 644, 777)
     */
    public UnixPermissions(String string) {
        if (string == null || string.length() != 3) {
            throw new IllegalArgumentException("UNIX permissions string length must be 3!");
        }

        for (int i = 0; i < 3; i++) {
            if (!Character.isDigit(string.charAt(i)))
                throw new IllegalArgumentException("UNIX permissions string must be numeric");
        }

        for (int i = 0; i < string.length(); i++) {
            char ch = string.charAt(i);
            boolean read = false, write = false, execute = false;

            if (ch == '1') {
                execute = true;

            } else if (ch == '2') {
                write = true;

            } else if (ch == '3') {
                write = execute = true;

            } else if (ch == '4') {
                read = true;

            } else if (ch == '5') {
                read = execute = true;

            } else if (ch == '6') {
                read = write = true;

            } else if (ch == '7') {
                read = write = execute = true;

            }

            if (i == 0) {
                ownerCanRead = read;
                ownerCanWrite = write;
                ownerCanExecute = execute;

            } else if (i == 1) {
                groupCanRead = read;
                groupCanWrite = write;
                groupCanExecute = execute;

            } else {
                worldCanRead = read;
                worldCanWrite = write;
                worldCanExecute = execute;
            }
        }
    }

    @Override
    public String toString() {
        int owner = (ownerCanRead ? 4 : 0) + (ownerCanWrite ? 2 : 0) + (ownerCanExecute ? 1 : 0);
        int group = (groupCanRead ? 4 : 0) + (groupCanWrite ? 2 : 0) + (groupCanExecute ? 1 : 0);
        int world = (worldCanRead ? 4 : 0) + (worldCanWrite ? 2 : 0) + (worldCanExecute ? 1 : 0);

        return String.format(Locale.getDefault(), "%1$d%2$d%3$d", owner, group, world);
    }

    public boolean isOwnerCanRead() {
        return ownerCanRead;
    }

    public UnixPermissions setOwnerCanRead(boolean ownerCanRead) {
        this.ownerCanRead = ownerCanRead;
        return this;
    }

    public boolean isOwnerCanWrite() {
        return ownerCanWrite;
    }

    public UnixPermissions setOwnerCanWrite(boolean ownerCanWrite) {
        this.ownerCanWrite = ownerCanWrite;
        return this;
    }

    public boolean isOwnerCanExecute() {
        return ownerCanExecute;
    }

    public UnixPermissions setOwnerCanExecute(boolean ownerCanExecute) {
        this.ownerCanExecute = ownerCanExecute;
        return this;
    }

    public boolean isGroupCanRead() {
        return groupCanRead;
    }

    public UnixPermissions setGroupCanRead(boolean groupCanRead) {
        this.groupCanRead = groupCanRead;
        return this;
    }

    public boolean isGroupCanWrite() {
        return groupCanWrite;
    }

    public UnixPermissions setGroupCanWrite(boolean groupCanWrite) {
        this.groupCanWrite = groupCanWrite;
        return this;
    }

    public boolean isGroupCanExecute() {
        return groupCanExecute;
    }

    public UnixPermissions setGroupCanExecute(boolean groupCanExecute) {
        this.groupCanExecute = groupCanExecute;
        return this;
    }

    public boolean isWorldCanRead() {
        return worldCanRead;
    }

    public UnixPermissions setWorldCanRead(boolean worldCanRead) {
        this.worldCanRead = worldCanRead;
        return this;
    }

    public boolean isWorldCanWrite() {
        return worldCanWrite;
    }

    public UnixPermissions setWorldCanWrite(boolean worldCanWrite) {
        this.worldCanWrite = worldCanWrite;
        return this;
    }

    public boolean isWorldCanExecute() {
        return worldCanExecute;
    }

    public UnixPermissions setWorldCanExecute(boolean worldCanExecute) {
        this.worldCanExecute = worldCanExecute;
        return this;
    }
}
