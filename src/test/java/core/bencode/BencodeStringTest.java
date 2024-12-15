package core.bencode;

import exceptions.BencodeParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BencodeString Parsing Tests")
class BencodeStringTest {

    @Nested
    @DisplayName("Valid Parsing Scenarios")
    class ValidParsing {

        @Test
        @DisplayName("Should parse a simple string with correct length")
        void testParseSimpleString() {
            String input = "4:spam";
            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());

            BencodeString beString = BencodeString.parse(in);

            assertEquals("spam", beString.getValue(), "Parsed value should match 'spam'");
            assertArrayEquals("spam".getBytes(), beString.getBytes(),
                              "Raw bytes should match the original input bytes");
        }

        @Test
        @DisplayName("Should parse an empty string (0-length)")
        void testParseEmptyString() {
            String input = "0:";
            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());

            BencodeString beString = BencodeString.parse(in);

            assertEquals("", beString.getValue(), "Parsed value should be empty string");
            assertArrayEquals("".getBytes(), beString.getBytes(), "Raw bytes should be empty");
        }

        @Test
        @DisplayName("Should parse a string with special characters")
        void testParseStringWithSpecialCharacters() {
            String expected = "hello world!";
            String input = expected.length() + ":" + expected; // e.g. "12:hello world!"
            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());

            BencodeString beString = BencodeString.parse(in);

            assertEquals(expected, beString.getValue(), "Parsed value should match the input string");
            assertArrayEquals(expected.getBytes(), beString.getBytes(),
                              "Raw bytes should match the special character string bytes");
        }

        @Test
        @DisplayName("Should handle strings with newline characters")
        void testStringWithNewline() {
            // A string containing a newline character
            String expected = "line1\nline2";
            String input = expected.length() + ":" + expected;
            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());

            BencodeString beString = BencodeString.parse(in);

            assertEquals(expected, beString.getValue(), "Parsed value should include newline");
            assertArrayEquals(expected.getBytes(), beString.getBytes(), "Raw bytes should include newline character");
        }
    }

    @Nested
    @DisplayName("Error Handling in Parsing")
    class ErrorHandling {

        @Test
        @DisplayName("Should throw BencodeParseException for non-numeric length")
        void testParseInvalidLengthNonNumeric() {
            // "x:spam" - 'x' is not a digit
            String input = "x:spam";
            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());

            NumberFormatException ex = assertThrows(NumberFormatException.class, () -> BencodeString.parse(in));
            assertEquals(NumberFormatException.class, ex.getClass(),
                         "Exception message should indicate an error in parsing the file.");
        }

        @Test
        @DisplayName("Should throw BencodeParseException if bytes read are fewer than specified length")
        void testParseTruncatedData() {
            // "4:spa" claims length=4 but only provides "spa" (3 bytes)
            String input = "4:spa";
            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());

            BencodeParseException ex = assertThrows(BencodeParseException.class, () -> BencodeString.parse(in));
            assertTrue(ex.getMessage()
                               .contains("Expected 4 bytes, got 3"),
                       "Exception message should indicate expected and actual byte count.");
        }

        @Test
        @DisplayName("Should throw NumberFormatException if colon is missing")
        void testParseMissingColon() {
            // "4spam" does not have a colon to separate length and data
            String input = "4spam";
            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());

            NumberFormatException ex = assertThrows(NumberFormatException.class, () -> BencodeString.parse(in));
            assertEquals(NumberFormatException.class, ex.getClass(),
                         "Exception message should indicate an error in parsing the file.");
        }

        @Test
        @DisplayName("Should throw BencodeParseException if end of stream before reading full length")
        void testParseEndOfStreamEarly() {
            // "4:" but no data after colon - should have 4 bytes following
            String input = "4:";
            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());

            BencodeParseException ex = assertThrows(BencodeParseException.class, () -> BencodeString.parse(in));
            assertTrue(ex.getMessage()
                               .contains("Expected 4 bytes, got -1"),
                       "Exception should indicate we did not get the needed bytes.");
        }

        @Test
        @DisplayName("Should throw NumberFormatException if input is completely empty")
        void testEmptyInputStream() {
            ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);

            NumberFormatException ex = assertThrows(NumberFormatException.class, () -> BencodeString.parse(in));
            assertEquals(NumberFormatException.class, ex.getClass(),
                         "Parsing empty input should fail due to missing length and colon.");
        }
    }

    @Nested
    @DisplayName("Equality, HashCode, and toString")
    class EqualityHashCodeAndToString {

        @Test
        @DisplayName("Equal BencodeStrings should be equal and have same hashCode")
        void testEqualsAndHashCode() {
            String input1 = "4:spam";
            String input2 = "4:spam";

            BencodeString beStr1 = BencodeString.parse(new ByteArrayInputStream(input1.getBytes()));
            BencodeString beStr2 = BencodeString.parse(new ByteArrayInputStream(input2.getBytes()));

            assertEquals(beStr1, beStr2, "Both BencodeStrings should be equal");
            assertEquals(beStr1.hashCode(), beStr2.hashCode(), "HashCodes should match for equal strings");
        }

        @Test
        @DisplayName("BencodeString should not be equal to arbitrary objects")
        void testNotEqualToDifferentObjects() {
            String input = "4:spam";
            BencodeString beString = BencodeString.parse(new ByteArrayInputStream(input.getBytes()));

            assertNotEquals(new Object(), beString, "BencodeString should not equal a random object");
        }

        @Test
        @DisplayName("toString() should return the parsed string value")
        void testToStringOverride() {
            String input = "4:spam";
            BencodeString beString = BencodeString.parse(new ByteArrayInputStream(input.getBytes()));

            assertEquals("spam", beString.toString(), "toString() should return the parsed value");
        }
    }

    @Nested
    @DisplayName("Byte Array Checks")
    class ByteArrayChecks {

        @Test
        @DisplayName("getBytes() should return raw bytes exactly")
        void testGetBytesContent() {
            String input = "4:spam";
            BencodeString beString = BencodeString.parse(new ByteArrayInputStream(input.getBytes()));

            assertArrayEquals("spam".getBytes(), beString.getBytes(), "getBytes() should match 'spam'");
        }

        @Test
        @DisplayName("Strings with multi-byte chars should also be handled correctly")
        void testMultiByteCharacters() {
            // Multibyte UTF-8 character: 'ß' (Eszett) is 2 bytes in UTF-8.
            String expected = "straß";
            String input = expected.getBytes().length + ":" + expected;
            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());

            BencodeString beString = BencodeString.parse(in);

            assertEquals(expected, beString.getValue(), "Value should match multi-byte string");
            assertArrayEquals(expected.getBytes(), beString.getBytes(),
                              "Bytes should match the multi-byte UTF-8 encoded string");
        }
    }
}
