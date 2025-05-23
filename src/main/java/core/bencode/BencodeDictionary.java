package core.bencode;

import exceptions.BencodeParseException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BencodeDictionary extends BencodeElement<Map<BencodeString, BencodeElement<?>>> {

    public BencodeDictionary(Map<BencodeString, BencodeElement<?>> value) {
        super(value);
    }

    public static BencodeDictionary ofMap(Map<String, ?> dict) {
        int initialCapacity = Math.max(8, (int)(dict.size() / 0.75f) + 1);
        Map<BencodeString, BencodeElement<?>> dictMap = new LinkedHashMap<>(initialCapacity);

        for(Map.Entry<String, ?> entry : dict.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            BencodeString beString = new BencodeString(key,key.getBytes(StandardCharsets.UTF_8));

            dictMap.put(beString,wrap(value));

        }

        return new BencodeDictionary(dictMap);
    }

    /**
     * Parse a dictionary from the input stream
     * Dictionary is formatted as d<key><value>e
     * Where 'd' represents the start of the dictionary, 'e' represents the end
     * Key is a bencoded string and value can be any bencoded element (integer, string, list, dictionary)
     * i.e. d3:cow3:moo4:spam4:eggse -> {"cow": "moo", "spam": "eggs"}
     *
     * @param in the input stream
     * @return the parsed dictionary
     */
    public static BencodeDictionary parse(InputStream in) {

        try {
            int prefix = in.read();

            if (prefix != 'd') {
                throw new BencodeParseException(String.format("Expected 'd', got %d", prefix));
            }

            Map<BencodeString, BencodeElement<?>> elements = new LinkedHashMap<>();

            while (true) {
                in.mark(1);
                int peek = in.read();
                in.reset();

                if (peek == 'e') {
                    in.read();
                    break;
                }

                if (peek == -1) {
                    throw new BencodeParseException("Unexpected end of file");
                }

                BencodeString key = BencodeString.parse(in);
                BencodeElement<?> value = BencodeElement.parseElement(in);

                elements.put(key, value);
            }
            return new BencodeDictionary(elements);

        } catch (IOException e) {
            throw new BencodeParseException("Error reading file", e);
        }
    }

    private static int compareBencodeStringByBytes(BencodeString a, BencodeString b) {
        // Compare raw bytes
        byte[] aBytes = a.getBytes();
        byte[] bBytes = b.getBytes();

        int minLen = Math.min(aBytes.length, bBytes.length);
        for (int i = 0; i < minLen; i++) {
            int diff = (aBytes[i] & 0xff) - (bBytes[i] & 0xff);
            if (diff != 0) {
                return diff;
            }
        }
        return aBytes.length - bBytes.length;
    }

    /**
     * Process the pieces string into a readable list of strings
     * Each string is a 20-byte SHA-1 hash
     *
     * @param piecesString the pieces string
     * @return the list of SHA-1 hashes
     */
    private static BencodeList processPieces(BencodeString piecesString) {

        byte[] piecesBytes = piecesString.getBytes();
        List<String> hashes = new ArrayList<>();

        for (int i = 0; i < piecesBytes.length; i += 20) {
            if (i + 20 > piecesBytes.length) {
                throw new IllegalArgumentException("Invalid pieces length: not a multiple of 20 bytes");
            }

            byte[] pieceHash = new byte[20];
            System.arraycopy(piecesBytes, i, pieceHash, 0, 20);

            hashes.add(bytesToHex(pieceHash));
        }

        List<BencodeElement<?>> beElements = hashes.stream()
                .map(hash -> new BencodeString(new String(hash.getBytes()), hash.getBytes()))
                .collect(Collectors.toList());

        return new BencodeList(beElements);
    }

    /**
     * Convert a byte array to a hexadecimal string
     *
     * @param bytes the byte array
     * @return the hexadecimal string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public String getInfoHash() {
        BencodeDictionary info = (BencodeDictionary) this.value.get(new BencodeString("info", "info".getBytes()));
        return toSha1(info.encode());
    }

    public byte[] getInfoHashBytes(){
        BencodeDictionary info = (BencodeDictionary) this.value.get(new BencodeString("info", "info".getBytes()));
        return sha1(info.encode());
    }

    public byte[] sha1(byte[] bytes){
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            return sha1.digest(bytes);
        } catch (Exception e) {
            throw new BencodeParseException("Error converting hash to bytes", e);
        }

    }

    @Override
    public byte[] encode() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        out.write('d');
        this.value.forEach((key, value1) -> {
            try {
                byte[] keyBytes = key.encode();
                out.write(keyBytes);

                byte[] valBytes = value1.encode();
                out.write(valBytes);
            } catch (IOException ex) {
                throw new RuntimeException("Error encoding dictionary entry", ex);
            }
        });

        out.write('e');

        return out.toByteArray();
    }

    @Override
    public Map<BencodeString, BencodeElement<?>> getValue() {
        return this.value;
    }

    private String toSha1(byte[] bytes) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] sha1Bytes = sha1.digest(bytes);

            StringBuilder sb = new StringBuilder();
            for (byte b : sha1Bytes) {
                if ((b >= 0x30 && b <= 0x39) || // 0-9
                        (b >= 0x41 && b <= 0x5A) || // A-Z
                        (b >= 0x61 && b <= 0x7A) || // a-z
                        b == '.' || b == '-' || b == '_' || b == '~') {
                    sb.append((char) b);
                } else {
                    sb.append(String.format("%%%02X", b & 0xFF));
                }
            }
            return sb.toString();
        } catch (Exception e) {
            throw new BencodeParseException("Error converting hash to bytes", e);
        }
    }

    /**
     * Retrieves bencode element from the dictionary
     *
     * @param key the key of the element
     * @return the element
     */
    public BencodeElement<?> get(String key) {
        return this.value.get(new BencodeString(key, key.getBytes()));
    }

    public <T> T get(String key, Class<T> tClass) {
        BencodeElement<?> element = this.value.get(new BencodeString(key, key.getBytes()));
        return element != null ? tClass.cast(element.getValue()) : null;
    }

    public String getAsString(String key) {
        System.out.println("getting key: " + key);

        BencodeElement<?> value = this.value.get(new BencodeString(key, key.getBytes()));
        return value != null ? (String) value.getValue() : null;
    }

    public long getAsLong(String key) {
        return (long) (this.value.get(new BencodeString(key, key.getBytes()))
                .getValue());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<BencodeString, BencodeElement<?>> entry : this.value.entrySet()) {
            sb.append(entry.getKey()
                              .toString());
            sb.append(":");
            sb.append(entry.getValue()
                              .toString());
            sb.append("\n");
        }

        return sb.toString();
    }

    public int size() {
        return this.value.size();
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BencodeDictionary other) {
            return this.value.equals(other.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.value.hashCode();
    }

}
