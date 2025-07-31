package çizgikod;

import java.util.*;

/**
 * Lexer class is responsible for converting source lines into tokens
 * according to the defined keyword and symbol mappings.
 * Now, it also creates 'Value' objects for literal tokens.
 */
public class Lexer {

    // Map of reserved keywords and symbols to token types
    private static final Map<String, TokenType> keywordMap = new HashMap<>();
    // Scanner for user input
    private static final Scanner scanner = new Scanner(System.in);

    static {
        // Control Structures & Keywords
        keywordMap.put("döfenşimos", TokenType.IF);
        keywordMap.put("ornitorenk", TokenType.ELSE);
        keywordMap.put("pepe", TokenType.WHILE);
        keywordMap.put("bebe", TokenType.FOR);
        keywordMap.put("dede", TokenType.RETURN);
        keywordMap.put("huysuz", TokenType.SWITCH);
        keywordMap.put("uzun", TokenType.CASE);
        keywordMap.put("tontiş", TokenType.BREAK);
        keywordMap.put("şapşik", TokenType.CONTINUE);
        keywordMap.put("marsupilami", TokenType.INPUT);
        keywordMap.put("tospik", TokenType.PRINT);

        // Symbols and Operators
        keywordMap.put("?", TokenType.QUESTION);
        keywordMap.put("!", TokenType.EXCLAMATION);
        keywordMap.put("_", TokenType.ASSIGN);
        keywordMap.put("._.", TokenType.PLUS);
        keywordMap.put(",_,", TokenType.MINUS);
        keywordMap.put("\\", TokenType.DIVIDE);
        keywordMap.put(":", TokenType.COLON);
        keywordMap.put(";", TokenType.SEMICOLON);
        keywordMap.put("*_*", TokenType.POWER);
        keywordMap.put("%", TokenType.MOD);

        // Data Types & Literals
        keywordMap.put("keloğlan", TokenType.VAR);
        keywordMap.put("rik", TokenType.TRUE);
        keywordMap.put("morti", TokenType.FALSE);
        keywordMap.put("mordekay", TokenType.INT_TYPE);
        keywordMap.put("rigbi", TokenType.STRING_TYPE);
        keywordMap.put("çakbeşlik", TokenType.BOOLEAN_TYPE);
        keywordMap.put("finyıs", TokenType.FLOAT_TYPE);
        keywordMap.put("förb", TokenType.CHAR_TYPE);

        // Comparison Operators
        keywordMap.put("__", TokenType.EQUAL);
        keywordMap.put("!_", TokenType.NOT_EQUAL);
        keywordMap.put("-:", TokenType.LESS_THAN);
        keywordMap.put(":-", TokenType.GREATER_THAN);
        keywordMap.put("-:_", TokenType.LESS_EQUAL);
        keywordMap.put(":-_", TokenType.GREATER_EQUAL);

        // Logical Operators
        keywordMap.put("#", TokenType.AND);
        keywordMap.put("##", TokenType.AND_AND);
        keywordMap.put("$", TokenType.OR);
        keywordMap.put("$$", TokenType.OR_OR);

        // Brackets and Structure Symbols
        keywordMap.put(")", TokenType.LPAREN);
        keywordMap.put("(", TokenType.RPAREN);
        keywordMap.put("}", TokenType.LBRACE); // Curly brace for block start
        keywordMap.put("{", TokenType.RBRACE); // Curly brace for block end (BNF reversed)
        keywordMap.put("]", TokenType.LBRACKET);
        keywordMap.put("[", TokenType.RBRACKET);
        keywordMap.put("._._", TokenType.PLUS_ASSIGN);
        keywordMap.put(",_,_", TokenType.MINUS_ASSIGN);

        // Line control
        keywordMap.put("^_^", TokenType.START_LINE);
    }

    /**
     * Converts a line of source code into a list of tokens.
     * For literals (numbers, strings, booleans), creates a Token with an associated Value object.
     *
     * @param line The source line
     * @return List of tokens extracted from the line
     */
    public static List<Token> tokenize(String line) {
        List<Token> tokens = new ArrayList<>();
        // Split by one or more whitespace characters while preserving quoted strings
        // This regex ensures that spaces inside "..." are not split
        String[] parts = line.split("\\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

        for (String part : parts) {
            if (part.isEmpty()) { // Skip empty parts that might result from multiple spaces
                continue;
            }

            if (keywordMap.containsKey(part)) {
                // Handle TRUE/FALSE literals separately to create Value objects
                if (part.equals("rik")) {
                    tokens.add(new Token(TokenType.TRUE, part, new Value(ÇizgiKodType.BOOLEAN, true)));
                } else if (part.equals("morti")) {
                    tokens.add(new Token(TokenType.FALSE, part, new Value(ÇizgiKodType.BOOLEAN, false)));
                } else {
                    tokens.add(new Token(keywordMap.get(part), part));
                }
            } else if (part.matches("\".*\"")) {
                // String literal: remove quotes and create a String Value
                String stringValue = part.substring(1, part.length() - 1);
                tokens.add(new Token(TokenType.STRING_LITERAL, part, new Value(ÇizgiKodType.STRING, stringValue)));
            } else if (part.matches("[0-9]+")) {
                // Integer literal
                try {
                    Integer intValue = Integer.parseInt(part);
                    tokens.add(new Token(TokenType.NUMBER, part, new Value(ÇizgiKodType.INT, intValue)));
                } catch (NumberFormatException e) {
                    tokens.add(new Token(TokenType.UNKNOWN, part)); // Should not happen with this regex
                }
            } else if (part.matches("[0-9]+\\.[0-9]+")) {
                // Float literal
                try {
                    Double floatValue = Double.parseDouble(part);
                    tokens.add(new Token(TokenType.NUMBER, part, new Value(ÇizgiKodType.FLOAT, floatValue)));
                } catch (NumberFormatException e) {
                    tokens.add(new Token(TokenType.UNKNOWN, part));
                }
            } else if (part.matches("[a-zA-ZçğıöşüÇĞİÖŞÜ_][a-zA-Z0-9çğıöşüÇĞİÖŞÜ_]*")) {
                // Identifier
                tokens.add(new Token(TokenType.IDENTIFIER, part));
            } else {
                // Unknown token
                tokens.add(new Token(TokenType.UNKNOWN, part));
            }
        }
        return tokens;
    }

    // Static method to get a line of input from the user
    public static String getUserInput() {
        System.out.print("ÇizgiKod input bekleniyor: ");
        return scanner.nextLine();
    }
}
