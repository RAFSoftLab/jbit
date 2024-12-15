package core.bencode;

import exceptions.BencodeParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("BencodeList Parsing Tests")
class BencodeListTest {

    @Nested
    @DisplayName("Valid Parsing Scenarios")
    class ValidParsing {

        @Test
        @DisplayName("Should parse empty list correctly")
        void testEmptyList() {
            String input = "le";
            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());

            BencodeList beList = BencodeList.parse(in);

            assertEquals(List.of(), beList.value, "List should be empty");
        }

        @Test
        @DisplayName("Should parse list of strings correctly")
        void testListOfStrings() {
            String input = "l4:spam4:eggse";
            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());

            BencodeList beList = BencodeList.parse(in);

            List<BencodeElement<?>> expected =
                    List.of(new BencodeString("spam", "spam".getBytes()), new BencodeString("eggs", "eggs".getBytes()));

            assertEquals(expected, beList.value, "List should contain 'spam' and 'eggs'");
        }

        @Test
        @DisplayName("Should parse list containing integers and strings")
        void testListOfIntegersAndStrings() {
            String input = "l4:spami42ee";
            // l4:spam i42e e
            // => ["spam", 42]
            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());

            BencodeList beList = BencodeList.parse(in);

            List<BencodeElement<?>> expected =
                    List.of(new BencodeString("spam", "spam".getBytes()), new BencodeInteger(42));

            assertEquals(expected, beList.value, "List should contain 'spam' and integer 42");
        }

        @Test
        @DisplayName("Should parse nested list correctly")
        void testNestedList() {
            String input = "ll4:spamee";
            // l l4:spam e e
            // => [ ["spam"] ]
            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());

            BencodeList beList = BencodeList.parse(in);

            BencodeList innerList = new BencodeList(List.of(new BencodeString("spam", "spam".getBytes())));
            List<BencodeElement<?>> expected = List.of(innerList);

            assertEquals(expected, beList.value, "List should contain one nested list with 'spam'");
        }

        @Test
        @DisplayName("Should parse list containing a dictionary")
        void testListWithDictionary() {
            String input = "ld3:cow3:mooee";

            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());

            BencodeList beList = BencodeList.parse(in);

            BencodeDictionary dict = new BencodeDictionary(
                    Map.of(new BencodeString("cow", "cow".getBytes()), new BencodeString("moo", "moo".getBytes())));
            List<BencodeElement<?>> expected = List.of(dict);

            assertEquals(expected, beList.value, "List should contain one dictionary with cow:moo");
        }

        @Test
        @DisplayName("Should parse a complex mixed list (strings, integers, lists and dictionaries)")
        void testComplexList() {
            String input = "l4:spami42el3:foo3:bared3:cow3:mooee";
            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());

            BencodeList beList = BencodeList.parse(in);

            BencodeString str = new BencodeString("spam", "spam".getBytes());

            BencodeInteger integer = new BencodeInteger(42);

            BencodeList innerList = new BencodeList(
                    List.of(new BencodeString("foo", "foo".getBytes()), new BencodeString("bar", "bar".getBytes())));

            BencodeDictionary dict = new BencodeDictionary(
                    Map.of(new BencodeString("cow", "cow".getBytes()), new BencodeString("moo", "moo".getBytes())));

            List<BencodeElement<?>> expected = List.of(str, integer, innerList, dict);

            assertEquals(expected, beList.value, "List should contain spam, 42, ['foo','bar'], {cow:moo}");
        }

        @Test
        @DisplayName("Should produce correct string representation")
        void testToStringRepresentation() {
            String input = "l4:spam4:eggse";
            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());

            BencodeList beList = BencodeList.parse(in);

            String output = beList.toString();
            // toString should produce each element on new line
            // spam
            // eggs
            String expectedOutput = "spam\neggs";
            assertEquals(expectedOutput, output, "String representation should list elements on new lines");
        }

        @Test
        @DisplayName("Should parse list with zero-length string element")
        void testZeroLengthStringElement() {
            String input = "l0:e";
            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());

            BencodeList beList = BencodeList.parse(in);

            List<BencodeElement<?>> expected = List.of(new BencodeString("", "".getBytes()));

            assertEquals(expected, beList.value, "List should contain an empty string element");
        }

        @Test
        @DisplayName("Should handle large number elements (boundary integers)")
        void testLargeNumberElements() {

            String input = "li0ei999999999999ee";
            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());

            BencodeList beList = BencodeList.parse(in);

            List<BencodeElement<?>> expected = List.of(new BencodeInteger(0), new BencodeInteger(999999999999L));

            assertEquals(expected, beList.value, "Should parse integers including very large ones");
        }

        @Test
        @DisplayName("Should parse list with multiple nested levels")
        void testMultipleNestedLevels() {
            // l l l 4:spam e e l 3:foo 3:bar e e
            // => [ [ [ "spam" ] ], ["foo","bar"] ]
            String input = "lll4:spameel3:foo3:baree";
            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());

            BencodeList innerFirstlist = new BencodeList(List.of(new BencodeString("spam", "spam".getBytes())));
            BencodeList firstList = new BencodeList(List.of(innerFirstlist));
            BencodeList secondList = new BencodeList(
                    List.of(new BencodeString("foo", "foo".getBytes()), new BencodeString("bar", "bar".getBytes())));
            BencodeList expected = new BencodeList(List.of(firstList, secondList));

            BencodeList result = BencodeList.parse(in);

            assertEquals(expected, result, "Should correctly parse deeply nested lists and complex structures");
        }

    }

    @Nested
    @DisplayName("Invalid Parsing Scenarios")
    class ErrorHandling {

        @Test
        @DisplayName("Should throw BencodeParseException for invalid prefix")
        void testInvalidPrefix() {
            String input = "4:spam";
            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());

            assertThrows(BencodeParseException.class, () -> BencodeList.parse(in),
                         "Should throw BencodeParseException if prefix is not 'l'");
        }

        @Test
        @DisplayName("Should throw BencodeParseException for missing end marker 'e'")
        void testMissingEndMarker() {
            String input = "l4:spam4:eggs";
            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());

            assertThrows(BencodeParseException.class, () -> BencodeList.parse(in),
                         "Should throw BencodeParseException if 'e' is missing");
        }

        @Test
        @DisplayName("Should throw BencodeParseException for incomplete element")
        void testIncompleteElement() {
            String input = "l4:spa";
            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());

            assertThrows(BencodeParseException.class, () -> BencodeList.parse(in),
                         "Should throw BencodeParseException for incomplete string element");
        }

        @Test
        @DisplayName("Should throw BencodeParseException for unknown element prefix")
        void testUnknownElementPrefix() {
            String input = "lX";
            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());

            assertThrows(BencodeParseException.class, () -> BencodeList.parse(in),
                         "Should throw BencodeParseException for unknown element prefix");
        }

        @Test
        @DisplayName("Should throw BencodeParseException for unexpected EOF")
        void testUnexpectedEOF() {
            String input = "l";
            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());

            assertThrows(BencodeParseException.class, () -> BencodeList.parse(in),
                         "Should throw BencodeParseException if file ends unexpectedly");
        }

        @Test
        @DisplayName("Should throw BencodeParseException if input is empty")
        void testEmptyInput() {
            String input = "";
            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());

            assertThrows(BencodeParseException.class, () -> BencodeList.parse(in),
                         "Should throw BencodeParseException for empty input");
        }
    }
}
