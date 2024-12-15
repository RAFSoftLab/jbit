package exceptions;

public class BencodeParseException extends RuntimeException {

    public BencodeParseException(String message, Throwable t) {
        super(message, t);
    }
    public BencodeParseException(String message) {
        super(message);
    }
}
