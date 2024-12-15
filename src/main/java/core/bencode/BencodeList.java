package core.bencode;

import exceptions.BencodeParseException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class BencodeList extends BencodeElement<List<BencodeElement<?>>> {

    BencodeList(List<BencodeElement<?>> value) {
        super(value);
    }

    /**
     * Parse a list from the input stream
     * List is formatted as l<element>e
     * Where 'l' represents the start of the list and 'e' represents the end
     * List can contain any bencoded element (integer, string, list, dictionary)
     * i.e. l4:spam4:eggse -> ["spam", "eggs"]
     *
     * @param in the input stream
     * @return the parsed list
     */
    public static BencodeList parse(InputStream in) {

        try {
            int prefix = in.read();

            if (prefix != 'l') {
                throw new BencodeParseException(String.format("Expected 'l', got %d", prefix));
            }

            List<BencodeElement<?>> elements = new ArrayList<>();

            while (true) {
                in.mark(1);
                int peek = in.read();
                in.reset();

                if (peek == 'e') {
                    in.read();
                    break;
                }

                BencodeElement<?> element = BencodeElement.parseElement(in);
                elements.add(element);
            }
            return new BencodeList(elements);

        } catch (IOException e) {
            throw new BencodeParseException("Error reading file", e);
        }
    }

    @Override
    public String toString() {
        return this.value.stream()
                .map(Object::toString)
                .reduce("", (a, b) -> a + b + System.lineSeparator())
                .stripTrailing();
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if(obj instanceof BencodeList other){
            return value.equals(other.value);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}