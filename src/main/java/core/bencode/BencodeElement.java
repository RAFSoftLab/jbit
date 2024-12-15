package core.bencode;

import exceptions.BencodeParseException;

import java.io.InputStream;

public abstract class BencodeElement<T> {

    final T value;

    BencodeElement(T value) {
        this.value = value;
    }

    protected static BencodeElement<?> parseElement(InputStream in) {
        try {
            in.mark(1);
            int prefix = in.read();
            in.reset();

            return switch (prefix) {
                case 'i' -> BencodeInteger.parse(in);
                case 'l' -> BencodeList.parse(in);
                case 'd' -> BencodeDictionary.parse(in);
                default -> BencodeString.parse(in);
            };

        } catch (Exception e) {
            throw new BencodeParseException("Error reading file", e);
        }
    }

}
