package core.bencode;

import exceptions.BencodeParseException;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class BencodeElement<T> {

    final T value;


    public abstract T getValue();

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

    @SuppressWarnings("unchecked")
    public static BencodeElement<?> wrap(Object o){

        if(o instanceof String s){
            return new BencodeString(s,s.getBytes(StandardCharsets.UTF_8));
        }

        if(o instanceof Number n){
            return new BencodeInteger(n.longValue());
        }

        if(o instanceof List<?> l){
            List<BencodeElement<?>> beList = new ArrayList<>(l.size());
            for (Object item : l) {
                beList.add(wrap(item));
            }
            return new BencodeList(beList);
        }

        if(o instanceof Map<?,?> map){
            return BencodeDictionary.ofMap((Map<String, ?>) map);
        }

        throw new IllegalArgumentException("Unsupported type: " + o.getClass());


    }

    public abstract byte[] encode();

}
