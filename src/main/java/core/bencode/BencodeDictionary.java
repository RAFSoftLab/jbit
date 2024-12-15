package core.bencode;

import exceptions.BencodeParseException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BencodeDictionary extends BencodeElement<Map<BencodeString, BencodeElement<?>>> {

    BencodeDictionary(Map<BencodeString, BencodeElement<?>> value) {
        super(value);
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

            Map<BencodeString, BencodeElement<?>> elements = new HashMap<>();

            while (true) {
                in.mark(1);
                int peek = in.read();
                in.reset();

                if (peek == 'e') {
                    in.read();
                    break;
                }

                if(peek == -1){
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


    /**
     * Retrieves bencode element from the dictionary
     *
     * @param key the key of the element
     * @return the element
     */
    public BencodeElement<?> get(String key) {
        return this.value.get(new BencodeString(key, key.getBytes()));
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

    public int size(){
        return this.value.size();
    }


    @Override
    public boolean equals(Object obj) {
        if(obj instanceof BencodeDictionary other){
            return this.value.equals(other.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.value.hashCode();
    }

}
