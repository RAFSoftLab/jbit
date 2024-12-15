package core.bencode;

import exceptions.BencodeParseException;

import java.io.InputStream;

public class Bencoder implements AutoCloseable {

    private final InputStream in;

    public Bencoder(InputStream in) {
        this.in = in;
    }

    public BencodeDictionary decode() {
        try {
            return BencodeDictionary.parse(in);
        } catch (Exception e) {
            throw new BencodeParseException("Error reading file", e);
        }
    }

    @Override
    public void close() throws Exception {
        in.close();
    }
}
