package net.imatruck.betterweather;

/**
 * Invalid location exception class
 */
public class InvalidLocationException extends Exception {
    private static final long serialVersionUID = 1L;

    public InvalidLocationException() {
    }

    public InvalidLocationException(String detailMessage) {
        super(detailMessage);
    }

    public InvalidLocationException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public InvalidLocationException(Throwable throwable) {
        super(throwable);
    }
}
