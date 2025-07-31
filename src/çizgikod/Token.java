package çizgikod;

/**
 * Represents a lexical token in the ÇizgiKod language.
 * Now includes an optional 'value' field for literals (numbers, strings, booleans).
 */
public class Token {
    TokenType type;  // The type of the token (e.g., IDENTIFIER, NUMBER)
    String lexeme;   // The raw text of the token (e.g., "keloğlan", "123", "\"hello\"")
    Value value;     // For literals, stores the parsed value (e.g., Integer 123, String "hello")

    // Constructor for tokens without an immediate literal value (e.g., keywords, operators, identifiers)
    public Token(TokenType type, String lexeme) {
        this.type = type;
        this.lexeme = lexeme;
        this.value = null; // No specific value for non-literals
    }

    // Constructor for literal tokens (numbers, strings, booleans)
    public Token(TokenType type, String lexeme, Value value) {
        this.type = type;
        this.lexeme = lexeme;
        this.value = value;
    }

    public TokenType getType() {
        return type;
    }

    public String getLexeme() {
        return lexeme;
    }

    public Value getValue() {
        return value;
    }

    @Override
    public String toString() {
        if (value != null) {
            return "Token{type=" + type + ", lexeme='" + lexeme + "', value=" + value + "}";
        }
        return "Token{type=" + type + ", lexeme='" + lexeme + "'}";
    }
}
