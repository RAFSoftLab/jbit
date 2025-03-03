package exceptions;

public class NoAvailableBlock extends RuntimeException {

    public NoAvailableBlock(String message) {
        super(message);
    }
}
