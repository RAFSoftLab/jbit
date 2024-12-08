package core.bencode;

import exceptions.BencodeParseException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class BencodeList extends BeElement<List<BeElement<?>>> {

    BencodeList(List<BeElement<?>> value) {
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
                throw new BencodeParseException(String.format("Expected 'l', got %c", prefix));
            }

            List<BeElement<?>> elements = new ArrayList<>();

            while (true) {
                in.mark(1);
                int peek = in.read();
                in.reset();

                if (peek == 'e') {
                    in.read();
                    break;
                }

                BeElement<?> element = BeElement.parseElement(in);
                elements.add(element);
            }
            return new BencodeList(elements);

        } catch (Exception e) {
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


}