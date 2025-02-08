package core.bencode;

import exceptions.BencodeParseException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class BencodeInteger extends BencodeElement<Long> {

    BencodeInteger(long value) {
        super(value);
    }

    /**
     * Parse an integer from the input stream
     * Integer is formatted as i<integer>e
     * Where 'i' represents the start of the integer and 'e' represents the end
     * i.e. i42e -> 42
     *
     * @param in the input stream
     * @return the parsed integer
     */
    public static BencodeInteger parse(InputStream in) {

        try {
            int prefix = in.read();
            if (prefix != 'i') {
                throw new BencodeParseException(String.format("Expected 'i', got %c", prefix));
            }
            StringBuilder sb = new StringBuilder();

            int i;
            while ((i = in.read()) != -1 && i != 'e') {
                sb.append((char) i);
            }

            if (i != 'e') throw new BencodeParseException(String.format("Expected 'e', got %d", i));

            return new BencodeInteger(Long.parseLong(sb.toString()));

        } catch (IOException e) {
            throw new BencodeParseException("Error reading file", e);
        }
    }

    @Override
    public byte[] encode() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        os.write('i');
        byte[] numberBytes = String.valueOf(value).getBytes(StandardCharsets.US_ASCII);
        os.write(numberBytes, 0, numberBytes.length);
        os.write('e');
        return os.toByteArray();
    }

    @Override
    public Long getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return this.value.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj){
            return true;
        }
        if(obj instanceof BencodeInteger other){
            return this.value.equals(other.value);
        }

        return false;
    }


    @Override
    public int hashCode() {
        return this.value.hashCode();
    }
}
