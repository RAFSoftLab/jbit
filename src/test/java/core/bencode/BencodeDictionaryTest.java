package core.bencode;

import exceptions.BencodeParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("BencodeDictionary Parsing Tests")
class BencodeDictionaryTest {


    @Nested
    @DisplayName("Valid Parsing Scenarios")
    class ValidParsing {

        @Test
        @DisplayName("Should return value of 2 for dictionary with 2 key-value pairs")
        void testDictionarySizeWithMultipleStringValues() {
            String input = "d3:cow3:moo4:spam4:eggse";
            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());

            BencodeDictionary beDictionary = BencodeDictionary.parse(in);

            assertEquals(2, beDictionary.size(), "Dictionary should have 2 key-value pairs");
        }


        @Test
        @DisplayName("Should return value for key 'cow' as bencoded 'moo', for spam as 'eggs'")
        void testWithMultipleStringValues() {
            String input = "d3:cow3:moo4:spam4:eggse";
            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());

            BencodeDictionary beDictionary = BencodeDictionary.parse(in);

            assertEquals(new BencodeString("moo", "moo".getBytes()), beDictionary.get("cow"),
                         "Value for key 'cow' should be 'moo'");
            assertEquals(new BencodeString("eggs", "eggs".getBytes()), beDictionary.get("spam"),
                         "Value for key 'spam' should be 'eggs'");
        }

        @Test
        @DisplayName("Should return bencoded list for key 'spam' with values 'a' and 'b'")
        void testWithListAsValue() {
            String input = "d4:spaml1:a1:bee";
            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());

            BencodeDictionary beDictionary = BencodeDictionary.parse(in);

            BencodeList expected = new BencodeList(
                    List.of(new BencodeString("a", "a".getBytes()), new BencodeString("b", "b".getBytes())));

            assertEquals(expected, beDictionary.get("spam"),
                         "Value for key 'spam' should be a list of strings 'a' and 'b'");
        }

        @Test
        @DisplayName("Should return size of 0 for empty dictionary")
        void testWithEmptyDictionary() {
            String input = "de";
            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());

            BencodeDictionary beDictionary = BencodeDictionary.parse(in);

            assertEquals(0, beDictionary.size(), "Dictionary should be empty");
        }


        @Test
        @DisplayName("Should return value as a dictionary with key 'moo' and value 'spam' for key 'cow'")
        void testWithOtherDictionaryAsValue() {
            String input = "d3:cowd3:moo4:spamee";
            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());

            BencodeDictionary beDictionary = BencodeDictionary.parse(in);

            BencodeDictionary expected = new BencodeDictionary(new HashMap<>(
                    Map.of(new BencodeString("cow", "cow".getBytes()), new BencodeDictionary(new HashMap<>(
                            Map.of(new BencodeString("moo", "moo".getBytes()),
                                   new BencodeString("spam", "spam".getBytes())))))));

            assertEquals(expected, beDictionary,
                         "Dictionary should have key 'cow' with value as another dictionary with key 'moo' and value 'spam'");
        }

        @Test
        @DisplayName("Should return value for zero-length key")
        void testZeroLengthKey() {
            String input = "d0:3:fooe";
            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());

            BencodeDictionary dictionary = BencodeDictionary.parse(in);

            assertEquals(new BencodeString("foo", "foo".getBytes()), dictionary.get(""),
                         "Value for empty key should be 'foo'");
        }

        @Test
        @DisplayName("Should handle duplicate keys by overwriting them")
        void testDuplicateKeys() {
            String input = "d3:key3:foo3:key3:bare";
            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());

            BencodeDictionary dictionary = BencodeDictionary.parse(in);
            assertEquals(new BencodeString("bar", "bar".getBytes()), dictionary.get("key"),
                         "Last value should overwrite previous ones");
        }


        @Test
        @DisplayName("Should handle deeply nested structures")
        void testDeeplyNestedDictionary() {
            String input = "d3:foo" + "d3:bar" + "d3:baz" + "3:qux" + "e" + "e" + "e";
            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());
            BencodeDictionary dictionary = BencodeDictionary.parse(in);

            BencodeDictionary innerMost = new BencodeDictionary(
                    Map.of(new BencodeString("baz", "baz".getBytes()), new BencodeString("qux", "qux".getBytes())));

            BencodeDictionary inner =
                    new BencodeDictionary(Map.of(new BencodeString("bar", "bar".getBytes()), innerMost));

            BencodeDictionary expected =
                    new BencodeDictionary(Map.of(new BencodeString("foo", "foo".getBytes()), inner));

            assertEquals(expected, dictionary, "Should correctly parse a deeply nested dictionary");
        }

        @Test
        @DisplayName("Should correctly parse dictionaries with binary data in keys")
        void testBinaryDataInKeys() {
            byte[] binaryKey = new byte[]{(byte) 0x00, (byte) 0xFF, (byte) 0x7F};
            String keyLength = String.valueOf(binaryKey.length);
            byte[] inputBytes = ("d" + keyLength + ":").getBytes();

            // 'd' + "3:" + <binaryKey> + "3:foo" + "e"
            byte[] combined = new byte[inputBytes.length + binaryKey.length + "3:foo".getBytes().length + 1];
            System.arraycopy(inputBytes, 0, combined, 0, inputBytes.length);
            System.arraycopy(binaryKey, 0, combined, inputBytes.length, binaryKey.length);
            System.arraycopy("3:foo".getBytes(), 0, combined, inputBytes.length + binaryKey.length,
                             "3:foo".getBytes().length);
            combined[combined.length - 1] = 'e';

            ByteArrayInputStream in = new ByteArrayInputStream(combined);

            BencodeDictionary dictionary = BencodeDictionary.parse(in);

            BencodeElement<?> val = dictionary.get(new String(binaryKey));
            assertEquals(new BencodeString("foo", "foo".getBytes()), val, "Value for the binary key should be 'foo'");
        }

    }

    @Nested
    @DisplayName("Invalid Parsing Scenarios")
    class ErrorHandling {

        @Test
        @DisplayName("Should throw BencodeParseException for invalid prefix")
        void testWithInvalidPrefix() {
            String input = "i42e";

            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());

            assertThrows(BencodeParseException.class, () -> BencodeDictionary.parse(in),
                         "Should throw BencodeParseException for invalid prefix");
        }

        @Test
        @DisplayName("Should throw BencodeParseException for invalid suffix")
        void testWithInvalidSuffix() {

            String input = "d3cow:moo4:spamf";

            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());

            assertThrows(NumberFormatException.class, () -> BencodeDictionary.parse(in),
                         "Should throw BencodeParseException for invalid suffix");

        }

        @Test
        @DisplayName("Should throw BencodeParseException for invalid suffix if the suffix is dictionary")
        void testWithInvalidSuffixForDictionary() {

            String input = "d3cow:moo4:spamd";

            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());

            assertThrows(NumberFormatException.class, () -> BencodeDictionary.parse(in),
                         "Should throw BencodeParseException for invalid suffix");

        }


        @Test
        @DisplayName("Should throw BencodeParseException for unexpected end of file")
        void testEmptyStringAsInput() {
            String input = "";
            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());

            assertThrows(BencodeParseException.class, () -> BencodeDictionary.parse(in),
                         "Should throw BencodeParseException for empty input");
        }

        @Test
        @DisplayName("Should throw exception if dictionary is not ended properly")
        void testMissingEndMarker() {
            String input = "d3:cow3:moo";
            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());

            assertThrows(BencodeParseException.class, () -> BencodeDictionary.parse(in),
                         "Should throw BencodeParseException if 'e' is missing at the end");
        }


    }

}