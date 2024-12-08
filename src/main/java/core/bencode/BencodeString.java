package core.bencode;

import exceptions.BencodeParseException;

import java.io.InputStream;

public class BencodeString extends BencodeElement<String> {

    private final byte[] bytes;

    BencodeString(String value, byte[] rawValue) {
        super(value);
        this.bytes = rawValue;
    }

    /**
     * Parse a string from the input stream
     * String is formatted as <length>:<string>
     * i.e. 4:spam -> "spam"
     *
     * @param in the input stream
     * @return BeString object containing the parsed string
     */
    public static BencodeString parse(InputStream in) {
        StringBuilder sb = new StringBuilder();

        try {
            int c;

            while ((c = in.read()) != -1 && c != ':') {
                sb.append((char) c);
            }

            int length = Integer.parseInt(sb.toString());
            byte[] buffer = new byte[length];

            int bytesRead = in.read(buffer);

            if (bytesRead != length) {
                throw new BencodeParseException(String.format("Expected %d bytes, got %d", length, bytesRead));
            }

            return new BencodeString(new String(buffer), buffer);

        } catch (Exception e) {
            throw new BencodeParseException("Error reading file", e);
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof BencodeString && this.value.equals(((BencodeString) obj).value);
    }

    @Override
    public int hashCode() {
        return this.value.hashCode();
    }

    @Override
    public String toString() {
        return this.value;
    }

    public byte[] getBytes() {
        return this.bytes;
    }

}
