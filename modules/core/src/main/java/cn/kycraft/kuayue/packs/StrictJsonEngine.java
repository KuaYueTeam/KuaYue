package cn.kycraft.kuayue.packs;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Strict RFC 8259 parser and deterministic writer for the small JSON tree.
 */
final class StrictJsonEngine {
    private StrictJsonEngine() {
    }

    static JsonValue parse(String json) {
        return new Parser(json).parseDocument();
    }

    static JsonValue parseObject(String json) {
        JsonValue value = parse(json);
        if (!value.isObject()) {
            throw new IllegalArgumentException("Expected a JSON object");
        }
        return value;
    }

    static String toJson(JsonValue value) {
        StringBuilder output = new StringBuilder();
        write(value, output, 0);
        return output.toString();
    }

    private static void write(JsonValue value, StringBuilder output, int depth) {
        if (value.isObject()) {
            output.append('{');
            if (!value.entries().isEmpty()) {
                output.append('\n');
                int index = 0;
                for (var entry : value.entries().entrySet()) {
                    indent(output, depth + 1);
                    writeString(entry.getKey(), output);
                    output.append(": ");
                    write(entry.getValue(), output, depth + 1);
                    if (++index < value.entries().size()) {
                        output.append(',');
                    }
                    output.append('\n');
                }
                indent(output, depth);
            }
            output.append('}');
        } else if (value.isArray()) {
            output.append('[');
            if (!value.elements().isEmpty()) {
                output.append('\n');
                for (int index = 0; index < value.elements().size(); index++) {
                    indent(output, depth + 1);
                    write(value.elements().get(index), output, depth + 1);
                    if (index + 1 < value.elements().size()) {
                        output.append(',');
                    }
                    output.append('\n');
                }
                indent(output, depth);
            }
            output.append(']');
        } else if (value.isString()) {
            writeString(value.asString(), output);
        } else if (value.isNumber()) {
            output.append(value.asNumber().toPlainString());
        } else if (value.isBoolean()) {
            output.append(value.asBoolean());
        } else if (value.isNull()) {
            output.append("null");
        }
    }

    private static void writeString(String value, StringBuilder output) {
        output.append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> output.append("\\\"");
                case '\\' -> output.append("\\\\");
                case '\b' -> output.append("\\b");
                case '\f' -> output.append("\\f");
                case '\n' -> output.append("\\n");
                case '\r' -> output.append("\\r");
                case '\t' -> output.append("\\t");
                default -> {
                    if (character < 0x20) {
                        output.append(String.format("\\u%04x", (int) character));
                    } else {
                        output.append(character);
                    }
                }
            }
        }
        output.append('"');
    }

    private static void indent(StringBuilder output, int depth) {
        output.append("  ".repeat(depth));
    }

    private static final class Parser {
        private static final int MAX_DEPTH = 256;
        private final String input;
        private int index;

        private Parser(String input) {
            if (input == null) {
                throw new IllegalArgumentException("JSON input is required");
            }
            this.input = input;
        }

        JsonValue parseDocument() {
            skipWhitespace();
            JsonValue value = parseValue(0);
            skipWhitespace();
            if (index != input.length()) {
                throw error("Trailing content after JSON value");
            }
            return value;
        }

        private JsonValue parseValue(int depth) {
            if (depth > MAX_DEPTH) {
                throw error("JSON nesting exceeds " + MAX_DEPTH);
            }
            if (index >= input.length()) {
                throw error("Unexpected end of JSON");
            }
            return switch (input.charAt(index)) {
                case '{' -> parseObject(depth + 1);
                case '[' -> parseArray(depth + 1);
                case '"' -> JsonValue.string(parseString());
                case 't' -> {
                    expectLiteral("true");
                    yield JsonValue.bool(true);
                }
                case 'f' -> {
                    expectLiteral("false");
                    yield JsonValue.bool(false);
                }
                case 'n' -> {
                    expectLiteral("null");
                    yield JsonValue.nullValue();
                }
                default -> {
                    char character = input.charAt(index);
                    if (character == '-' || isDigit(character)) {
                        yield parseNumber();
                    }
                    throw error("Unexpected character: " + character);
                }
            };
        }

        private JsonValue parseObject(int depth) {
            index++;
            skipWhitespace();
            JsonValue object = JsonValue.object();
            Set<String> names = new HashSet<>();
            if (consume('}')) {
                return object;
            }
            while (true) {
                if (index >= input.length() || input.charAt(index) != '"') {
                    throw error("Object keys must be strings");
                }
                String name = parseString();
                if (!names.add(name)) {
                    throw error("Duplicate object key: " + name);
                }
                skipWhitespace();
                expect(':');
                skipWhitespace();
                object.put(name, parseValue(depth));
                skipWhitespace();
                if (consume('}')) {
                    return object;
                }
                expect(',');
                skipWhitespace();
                if (index < input.length() && input.charAt(index) == '}') {
                    throw error("Trailing comma in object");
                }
            }
        }

        private JsonValue parseArray(int depth) {
            index++;
            skipWhitespace();
            JsonValue array = JsonValue.array();
            if (consume(']')) {
                return array;
            }
            while (true) {
                array.add(parseValue(depth));
                skipWhitespace();
                if (consume(']')) {
                    return array;
                }
                expect(',');
                skipWhitespace();
                if (index < input.length() && input.charAt(index) == ']') {
                    throw error("Trailing comma in array");
                }
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder value = new StringBuilder();
            while (index < input.length()) {
                char character = input.charAt(index++);
                if (character == '"') {
                    return value.toString();
                }
                if (character == '\\') {
                    if (index >= input.length()) {
                        throw error("Unterminated string escape");
                    }
                    char escaped = input.charAt(index++);
                    switch (escaped) {
                        case '"' -> value.append('"');
                        case '\\' -> value.append('\\');
                        case '/' -> value.append('/');
                        case 'b' -> value.append('\b');
                        case 'f' -> value.append('\f');
                        case 'n' -> value.append('\n');
                        case 'r' -> value.append('\r');
                        case 't' -> value.append('\t');
                        case 'u' -> value.append(parseUnicodeEscape());
                        default -> throw error("Invalid string escape: \\" + escaped);
                    }
                } else {
                    if (character < 0x20) {
                        throw error("Unescaped control character in string");
                    }
                    value.append(character);
                }
            }
            throw error("Unterminated string");
        }

        private char parseUnicodeEscape() {
            if (index + 4 > input.length()) {
                throw error("Incomplete unicode escape");
            }
            int code = 0;
            for (int count = 0; count < 4; count++) {
                int digit = Character.digit(input.charAt(index++), 16);
                if (digit < 0) {
                    throw error("Invalid unicode escape");
                }
                code = code * 16 + digit;
            }
            return (char) code;
        }

        private JsonValue parseNumber() {
            int start = index;
            if (consume('-') && index >= input.length()) {
                throw error("Incomplete number");
            }
            if (consume('0')) {
                if (index < input.length() && isDigit(input.charAt(index))) {
                    throw error("Leading zero in number");
                }
            } else {
                if (index >= input.length() || input.charAt(index) < '1'
                        || input.charAt(index) > '9') {
                    throw error("Invalid number");
                }
                while (index < input.length() && isDigit(input.charAt(index))) {
                    index++;
                }
            }
            if (consume('.')) {
                int fractionStart = index;
                while (index < input.length() && isDigit(input.charAt(index))) {
                    index++;
                }
                if (index == fractionStart) {
                    throw error("Fraction requires at least one digit");
                }
            }
            if (index < input.length()
                    && (input.charAt(index) == 'e' || input.charAt(index) == 'E')) {
                index++;
                if (index < input.length()
                        && (input.charAt(index) == '+' || input.charAt(index) == '-')) {
                    index++;
                }
                int exponentStart = index;
                while (index < input.length() && isDigit(input.charAt(index))) {
                    index++;
                }
                if (index == exponentStart) {
                    throw error("Exponent requires at least one digit");
                }
            }
            try {
                return JsonValue.number(new BigDecimal(input.substring(start, index)));
            } catch (NumberFormatException exception) {
                throw error("Invalid number");
            }
        }

        private void expectLiteral(String literal) {
            if (!input.startsWith(literal, index)) {
                throw error("Expected " + literal);
            }
            index += literal.length();
        }

        private void skipWhitespace() {
            while (index < input.length()) {
                char character = input.charAt(index);
                if (character == ' ' || character == '\n'
                        || character == '\r' || character == '\t') {
                    index++;
                } else {
                    return;
                }
            }
        }

        private boolean consume(char expected) {
            if (index < input.length() && input.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private void expect(char expected) {
            if (!consume(expected)) {
                throw error("Expected '" + expected + "'");
            }
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(
                    message + " at character " + index);
        }

        private static boolean isDigit(char character) {
            return character >= '0' && character <= '9';
        }
    }
}
