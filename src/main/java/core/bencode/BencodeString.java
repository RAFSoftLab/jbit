package core.bencode;

import exceptions.BencodeParseException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class BencodeString extends BencodeElement<String> {

    private final byte[] bytes;

    public BencodeString(String value, byte[] rawValue) {
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
            if (length == 0) return new BencodeString("", new byte[0]);

            byte[] buffer = new byte[length];
            int bytesRead = in.read(buffer);

            if (bytesRead != length) {
                throw new BencodeParseException(String.format("Expected %d bytes, got %d", length, bytesRead));
            }

            return new BencodeString(new String(buffer), buffer);

        } catch (IOException e) {
            throw new BencodeParseException("Error reading file", e);
        }
    }

    @Override
    public byte[] encode() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] lengthBytes = String.valueOf(bytes.length).getBytes(StandardCharsets.US_ASCII);
        out.write(lengthBytes, 0, lengthBytes.length);
        out.write(':');
        out.write(bytes, 0, bytes.length);

        return out.toByteArray();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof BencodeString && this.value.equals(((BencodeString) obj).value) && Arrays.equals(this.bytes, ((BencodeString) obj).bytes);
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

    @Override
    public String getValue() {
        return this.value;
    }

}
