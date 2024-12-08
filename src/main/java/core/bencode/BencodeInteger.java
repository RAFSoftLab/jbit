package core.bencode;

import exceptions.BencodeParseException;

import java.io.InputStream;

public class BencodeInteger extends BencodeElement<Long> {

    BencodeInteger(Long value) {
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
            if (in.read() != 'i') {
                throw new BencodeParseException(String.format("Expected 'i', got %c", in.read()));
            }
            StringBuilder sb = new StringBuilder();

            int i;
            while ((i = in.read()) != -1 && i != 'e') {
                sb.append((char) i);
            }
            return new BencodeInteger(Long.parseLong(sb.toString()));

        } catch (Exception e) {
            throw new BencodeParseException("Error reading file", e);
        }
    }

    @Override
    public String toString() {
        return this.value.toString();
    }


}
