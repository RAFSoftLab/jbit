package core.bencode;

import exceptions.BencodeParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BencodeInteger Parsing Tests")
class BencodeIntegerTest {

    @Nested
    @DisplayName("Valid Parsing Scenarios")
    class ValidIntegerParsing {

        @Test
        @DisplayName("Should parse a simple integer")
        void testParseSimpleInteger() {

            String input = "i42e";
            var in = new ByteArrayInputStream(input.getBytes());
            long expected = 42L;
            long actual = BencodeInteger.parse(in)
                    .getValue();
            assertEquals(expected, actual, "Parsed value should match value 42");
        }

        @Test
        @DisplayName("Should parse a negative integer")
        void testNegativeInteger() {
            String input = "i-20e";
            var in = new ByteArrayInputStream(input.getBytes());

            long expected = -20L;
            long actual = BencodeInteger.parse(in)
                    .getValue();

            assertEquals(expected, actual, "Parsed value should match value of -20");

        }

        @Test
        @DisplayName("Should parse a integer with value of 0")
        void testZeroInteger() {
            String input = "i0e";
            var in = new ByteArrayInputStream(input.getBytes());

            long expected = 0L;
            long actual = BencodeInteger.parse(in)
                    .getValue();

            assertEquals(expected, actual, "Parsed value should match value of 0");

        }
    }


    @Nested
    @DisplayName("Error handling scenarios")
    class ErrorHandling {

        @Test
        @DisplayName("Should throw BencodeParseException if the prefix does not match required one")
        void testInvalidPrefix() {
            String input = "d42e";
            var in = new ByteArrayInputStream(input.getBytes());
            var expected = BencodeParseException.class;

            var ex = assertThrows(expected, () -> BencodeInteger.parse(in), "Should throw bencode parsing exception");
            assertTrue(ex.getMessage()
                               .contains("Expected 'i', got d"));

        }


        @Test
        @DisplayName("Should throw BencodeParseException if there is no prefix specified")
        void testWithoutPrefix() {
            String input = "42e";

            var in = new ByteArrayInputStream(input.getBytes());
            var expected = BencodeParseException.class;

            var ex = assertThrows(expected, () -> BencodeInteger.parse(in), "Should throw bencode parsing exception");
            assertTrue(ex.getMessage()
                               .contains("Expected 'i', got 4"));

        }

        @Test
        @DisplayName("Should throw BencodeParseException if there is no prefix regardless of missing suffix")
        void testWithDigitsOnly() {
            String input = "33";

            var in = new ByteArrayInputStream(input.getBytes());
            var expected = BencodeParseException.class;

            var ex = assertThrows(expected, () -> BencodeInteger.parse(in), "Should throw bencode parsing exception");
            assertTrue(ex.getMessage()
                               .contains("Expected 'i', got 3"));

        }

        @Test
        @DisplayName("Should throw BencodeParserException if there is not suffix specified")
        void testWithoutSuffix() {
            String input = "i33";

            var in = new ByteArrayInputStream(input.getBytes());
            var expected = BencodeParseException.class;

            var ex = assertThrows(expected, () -> BencodeInteger.parse(in), "Should throw bencode parsing exception");
            assertTrue(ex.getMessage()
                               .contains("Expected 'e', got -1"));
        }

        @Test
        @DisplayName("Should throw NumberFormatException if there are no digits specified")
        void testWithoutDigits(){
            String input = "ie";

            var in = new ByteArrayInputStream(input.getBytes());
            var expected = NumberFormatException.class;

            var ex = assertThrows(expected, () -> BencodeInteger.parse(in), "Should throw bencode parsing exception");
            assertTrue(ex.getMessage()
                               .contains("For input string: "));

        }

        @Test
        @DisplayName("Should throws NumberFormatException if the value between prefix and suffix is not a number")

        void testInvalidDigitValue(){
            String input = "ire";
            var in = new ByteArrayInputStream(input.getBytes());
            var expected = NumberFormatException.class;

            var ex = assertThrows(expected, () -> BencodeInteger.parse(in), "Should throw bencode parsing exception");
            assertTrue(ex.getMessage()
                               .contains("For input string: "));
        }
    }

}