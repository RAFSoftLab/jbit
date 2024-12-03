package core;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Bencoder {

    private final InputStream in;

    public Bencoder(InputStream in) {
        this.in = in;
    }

    public static void main(String[] args) throws FileNotFoundException {
        Bencoder torrentParser = new Bencoder(
                new BufferedInputStream(new FileInputStream("src/main/resources/torrentFiles/test2.torrent")));

        try {
            BencodeElement element = torrentParser.parse();
            torrentParser.printElement(element, 0);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static BencodeElement parseElement(InputStream in) throws IOException {
        in.mark(1);
        int peek = in.read();
        in.reset();

        if (Character.isDigit(peek)) return BencodeString.parse(in);
        if (peek == 'i') return BencodeInteger.parse(in);
        if (peek == 'l') return BencodeList.parse(in);
        if (peek == 'd') return BencodeDictionary.parse(in);

        throw new IllegalArgumentException("Unexpected element type: " + (char) peek);
    }

    private static BencodeElement processPieces(BencodeString piecesString) {
        byte[] piecesBytes = piecesString.getRawValue();
        List<String> hashes = new ArrayList<>();

        for (int i = 0; i < piecesBytes.length; i += 20) {
            if (i + 20 > piecesBytes.length) {
                throw new IllegalArgumentException("Invalid pieces length: not a multiple of 20 bytes");
            }

            byte[] pieceHash = new byte[20];
            System.arraycopy(piecesBytes, i, pieceHash, 0, 20);

            hashes.add(bytesToHex(pieceHash));
        }

        return new BencodeList(hashes.stream()
                                       .map(hash -> new BencodeString(hash.getBytes()))
                                       .toArray(BencodeElement[]::new));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public BencodeElement parse() throws IOException {
        in.mark(1);
        int peek = in.read();
        in.reset();

        if (Character.isDigit(peek)) {
            return BencodeString.parse(in);
        } else if (peek == 'i') {
            return BencodeInteger.parse(in);
        } else if (peek == 'l') {
            return BencodeList.parse(in);
        } else if (peek == 'd') {
            return BencodeDictionary.parse(in);
        }

        throw new IllegalArgumentException("Invalid root element type: " + (char) peek);
    }

    private void printElement(BencodeElement element, int indent) {
        String padding = " ".repeat(indent);

        switch (element) {
            case BencodeString bencodeString -> System.out.println(padding + "String: " + bencodeString.getValue());
            case BencodeInteger bencodeInteger -> System.out.println(padding + "Integer: " + bencodeInteger.getValue());
            case BencodeList ignored -> {
                System.out.println(padding + "List:");
                for (BencodeElement el : (BencodeElement[]) element.getValue()) {
                    printElement(el, indent + 2);
                }
            }
            case BencodeDictionary ignored -> {
                System.out.println(padding + "Dictionary:");
                Map<BencodeString, BencodeElement> map = (Map<BencodeString, BencodeElement>) element.getValue();
                for (var entry : map.entrySet()) {
                    System.out.println(padding + "  Key: " + entry.getKey()
                            .getValue());
                    printElement(entry.getValue(), indent + 2);
                }
            }
            case null, default -> System.out.println(padding + "Unknown Element");
        }
    }

    public static class BencodeElement {
        Object value;

        public BencodeElement(Object value) {
            this.value = value;
        }

        public Object getValue() {
            return value;
        }
    }

    private static class BencodeString extends BencodeElement {

        public BencodeString(byte[] value) {
            super(value);
        }

        static BencodeString parse(InputStream in) throws IOException {
            StringBuilder sb = new StringBuilder();

            int i;
            while ((i = in.read()) != -1 && i != ':') {
                sb.append((char) i);
            }

            int length = Integer.parseInt(sb.toString());
            byte[] buffer = new byte[length];
            int bytesRead = in.read(buffer);
            if (bytesRead != length) {
                throw new RuntimeException("Error reading file");
            }
            return new BencodeString(buffer);
        }

        @Override
        public String getValue() {
            return new String((byte[]) value);
        }

        public byte[] getRawValue() {
            return (byte[]) value;
        }

    }

    private static class BencodeInteger extends BencodeElement {

        public BencodeInteger(long value) {
            super(value);
        }

        static BencodeInteger parse(InputStream in) throws IOException {
            StringBuilder sb = new StringBuilder();

            int prefix = in.read();

            if (prefix != 'i') {
                throw new IllegalArgumentException("Invalid integer prefix: " + (char) prefix);
            }

            int i;
            while ((i = in.read()) != -1 && i != 'e') {
                sb.append((char) i);
            }
            return new BencodeInteger(Long.parseLong(sb.toString()));
        }

    }

    private static class BencodeList extends BencodeElement {

        public BencodeList(BencodeElement[] elements) {
            super(elements);
        }

        static BencodeList parse(InputStream in) throws IOException {

            int prefix = in.read();

            if (prefix != 'l') {
                throw new IllegalArgumentException("Invalid list prefix: " + (char) prefix);
            }

            List<BencodeElement> elements = new ArrayList<>();

            while (true) {
                in.mark(1);
                int peek = in.read();

                if (peek == -1) break;

                if (Character.isDigit(peek)) {
                    in.reset();
                    elements.add(BencodeString.parse(in));
                } else if (peek == 'i') {
                    in.reset();
                    elements.add(BencodeInteger.parse(in));
                } else if (peek == 'l') {
                    in.reset();
                    elements.add(BencodeList.parse(in));
                } else if (peek == 'd') {
                    in.reset();
                    elements.add(BencodeDictionary.parse(in));
                } else {
                    break;
                }
            }

            return new BencodeList(elements.toArray(new BencodeElement[0]));

        }
    }

    private static class BencodeDictionary extends BencodeElement {

        public BencodeDictionary(Map<BencodeString, BencodeElement> map) {
            super(map);
        }

        static BencodeDictionary parse(InputStream in) throws IOException {

            int prefix = in.read();

            if (prefix != 'd') {
                throw new IllegalArgumentException("Invalid dictionary prefix: " + (char) prefix);
            }

            Map<BencodeString, BencodeElement> map = new HashMap<>();

            while (true) {
                in.mark(1);
                int peek = in.read();

                if (peek == 'e') {
                    break;
                } else if (peek == -1) {
                    throw new IOException("Unexpected end of input while parsing dictionary");
                }

                in.reset();
                BencodeString key = BencodeString.parse(in);

                if ("pieces".equals(key.getValue())) {
                    BencodeElement piecesString = processPieces((BencodeString) parseElement(in));
                    map.put(key, piecesString);

                } else {
                    BencodeElement value = parseElement(in);
                    map.put(key, value);
                }
            }
            return new BencodeDictionary(map);

        }
    }


}
