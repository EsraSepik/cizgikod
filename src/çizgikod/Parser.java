package çizgikod;

import java.util.*;

/**
 * Parser class is now responsible for both syntax analysis and interpretation (execution)
 * of the ÇizgiKod language. It uses a symbol table to store variable values.
 */
public class Parser {

    private List<Token> tokens; // List of tokens to parse
    private int index;          // Current position in the token list
    // Symbol table to store declared variables and their current values
    private Map<String, Value> symbolTable = new HashMap<>();
    private boolean hadReturn = false; // Flag to track if a return statement was encountered
    private boolean hadBreak = false;  // Flag to track if a break statement was encountered
    private boolean hadContinue = false; // Flag to track if a continue statement was encountered

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.index = 0;
    }

    /**
     * Parses and executes the entire ÇizgiKod program.
     *
     * @return true if parsing and execution completed without critical errors, false otherwise.
     */
    public boolean parseProgram() {
        hadReturn = false; // Reset return flag for a new program run
        while (index < tokens.size() && !hadReturn && !hadBreak && !hadContinue) {
            // Each statement must start with START_LINE token (^_^).
            // If the next token is not START_LINE, it's a syntax error unless it's EOF or RBRACE.
            if (peek().type != TokenType.START_LINE && peek().type != TokenType.RBRACE && peek().type != TokenType.UNKNOWN) {
                return error("Expected START_LINE token (^_^)." + (index < tokens.size() ? " Found: " + currentToken().getLexeme() : "EOF"));
            }

            // Attempt to parse and execute a single statement
            if (!parseStatement()) {
                // If a statement parsing/execution fails, stop the program.
                System.err.println("Program execution failed at statement.");
                return false;
            }
        }
        return true;
    }

    /**
     * Parses and executes a single statement.
     *
     * @return true if the statement was successfully parsed and executed, false otherwise.
     */
    private boolean parseStatement() {
        // All statements must start with ^_^ (START_LINE)
        if (!match(TokenType.START_LINE)) {
            return error("Line must start with ^_^.");
        }

        // Determine the type of statement based on the next token and call the appropriate handler
        TokenType next = peek().type;
        boolean statementParsed = false; // Flag to check if a known statement type was handled

        if (next == TokenType.PRINT) {
            statementParsed = parsePrint();
        } else if (next == TokenType.VAR) {
            statementParsed = parseDeclaration();
        } else if (next == TokenType.IDENTIFIER) {
            // Could be assignment or an expression that's just evaluated
            statementParsed = parseAssignment();
            if (!statementParsed) { // If it wasn't an assignment, try parsing it as a standalone expression (though not typical for side effects)
                try {
                    evaluateExpression(); // Just evaluate, but discard the result if not assigned
                    statementParsed = true;
                } catch (RuntimeException e) {
                    System.err.println("Error evaluating standalone expression: " + e.getMessage());
                    statementParsed = false;
                }
            }
        } else if (next == TokenType.IF) {
            statementParsed = parseIfStructure();
        } else if (next == TokenType.WHILE) {
            statementParsed = parseWhileStructure();
        } else if (next == TokenType.INPUT) {
            statementParsed = parseInput();
        } else if (next == TokenType.RETURN) {
            statementParsed = parseReturn();
        } else if (next == TokenType.BREAK) {
            statementParsed = parseBreak();
        } else if (next == TokenType.CONTINUE) {
            statementParsed = parseContinue();
        } else {
            // If none of the known statement types match, it's an invalid expression
            return error("Invalid expression or statement type: " + currentToken().getLexeme());
        }

        if (!statementParsed) {
            return false; // If the specific statement parsing/execution failed
        }

        // All statements must end with ^_^ (START_LINE)
        if (!match(TokenType.START_LINE)) {
            return error("Line must end with ^_^.");
        }

        return true;
    }

    /**
     * Parses and executes a print statement (tospik).
     * Syntax: tospik ( Expression )
     *
     * @return true if successful, false otherwise.
     */
    private boolean parsePrint() {
        if (!match(TokenType.PRINT)) return false;
        if (!match(TokenType.LPAREN)) return error("Expected '(' after 'tospik'.");

        Value valueToPrint;
        try {
            valueToPrint = evaluateExpression(); // Evaluate the expression to get its value
        } catch (RuntimeException e) {
            return error("Error in print expression: " + e.getMessage());
        }

        if (valueToPrint == null) {
            return error("Print statement evaluated to null.");
        }

        System.out.println("Çıktı: " + valueToPrint.toString()); // Print the evaluated value
        if (!match(TokenType.RPAREN)) return error("Expected ')' after print expression.");
        return true;
    }

    /**
     * Parses and executes a variable declaration (keloğlan).
     * Syntax: keloğlan Identifier _ Expression
     *
     * @return true if successful, false otherwise.
     */
    private boolean parseDeclaration() {
        if (!match(TokenType.VAR)) return false; // keloğlan

        if (peek().type != TokenType.IDENTIFIER) return error("Expected identifier after 'keloğlan'.");
        String varName = consume().getLexeme(); // Get variable name

        if (symbolTable.containsKey(varName)) {
            return error("Variable '" + varName + "' already declared.");
        }

        if (!match(TokenType.ASSIGN)) return error("Expected '_' after variable name in declaration.");

        Value assignedValue;
        try {
            assignedValue = evaluateExpression(); // Evaluate the initial value
        } catch (RuntimeException e) {
            return error("Error in declaration assignment expression: " + e.getMessage());
        }

        if (assignedValue == null) {
            return error("Declaration assignment evaluated to null.");
        }

        symbolTable.put(varName, assignedValue); // Add variable and its value to symbol table
        System.out.println("➡ Değişken tanımlandı: " + varName + " = " + assignedValue);
        return true;
    }

    /**
     * Parses and executes an assignment statement.
     * Syntax: Identifier _ Expression
     *
     * @return true if successful, false otherwise.
     */
    private boolean parseAssignment() {
        if (peek().type != TokenType.IDENTIFIER) return error("Expected identifier for assignment.");
        String varName = consume().getLexeme(); // Get variable name

        if (!symbolTable.containsKey(varName)) {
            return error("Variable '" + varName + "' not declared before assignment.");
        }

        if (!match(TokenType.ASSIGN)) return error("Expected '_' for assignment.");

        Value assignedValue;
        try {
            assignedValue = evaluateExpression(); // Evaluate the new value
        } catch (RuntimeException e) {
            return error("Error in assignment expression: " + e.getMessage());
        }

        if (assignedValue == null) {
            return error("Assignment evaluated to null.");
        }

        // Basic type compatibility check (can be expanded)
        Value existingValue = symbolTable.get(varName);
        if (existingValue.getType() != ÇizgiKodType.UNKNOWN && assignedValue.getType() != ÇizgiKodType.UNKNOWN &&
            existingValue.getType() != assignedValue.getType()) {
            // Allow INT to FLOAT assignment, but warn
            if (!((existingValue.getType() == ÇizgiKodType.FLOAT && assignedValue.getType() == ÇizgiKodType.INT) ||
                  (existingValue.getType() == ÇizgiKodType.INT && assignedValue.getType() == ÇizgiKodType.FLOAT))) {
                System.err.println("Uyarı: Değişken '" + varName + "' tür uyumsuzluğu! Mevcut: " + existingValue.getType() + ", Atanan: " + assignedValue.getType());
                // return error("Type mismatch for variable '" + varName + "'"); // Could be a hard error
            }
        }

        symbolTable.put(varName, assignedValue); // Update variable's value
        System.out.println("➡ Değişken değeri güncellendi: " + varName + " = " + assignedValue);
        return true;
    }

    /**
     * Parses and executes an input statement (marsupilami).
     * Syntax: marsupilami ( Identifier )
     *
     * @return true if successful, false otherwise.
     */
    private boolean parseInput() {
        if (!match(TokenType.INPUT)) return false; // marsupilami
        if (!match(TokenType.LPAREN)) return error("Expected '(' after 'marsupilami'.");

        if (peek().type != TokenType.IDENTIFIER) return error("Expected identifier for input variable.");
        String varName = consume().getLexeme(); // Get variable name

        if (!symbolTable.containsKey(varName)) {
            return error("Variable '" + varName + "' not declared for input.");
        }

        // Get user input using the Lexer's static method
        String userInput = Lexer.getUserInput();
        Value targetVar = symbolTable.get(varName);
        Value inputValue;

        // Attempt to convert user input to the variable's declared type
        try {
            switch (targetVar.getType()) {
                case INT:
                    inputValue = new Value(ÇizgiKodType.INT, Integer.parseInt(userInput));
                    break;
                case FLOAT:
                    inputValue = new Value(ÇizgiKodType.FLOAT, Double.parseDouble(userInput));
                    break;
                case BOOLEAN:
                    if (userInput.equalsIgnoreCase("rik")) inputValue = new Value(ÇizgiKodType.BOOLEAN, true);
                    else if (userInput.equalsIgnoreCase("morti")) inputValue = new Value(ÇizgiKodType.BOOLEAN, false);
                    else return error("Invalid boolean input. Expected 'rik' or 'morti'.");
                    break;
                case STRING:
                    inputValue = new Value(ÇizgiKodType.STRING, userInput);
                    break;
                case CHAR: // Simple char handling for now, treats as string input
                    if (userInput.length() == 1) {
                         inputValue = new Value(ÇizgiKodType.CHAR, userInput.charAt(0));
                    } else {
                        return error("Invalid char input. Expected single character.");
                    }
                    break;
                default:
                    return error("Unsupported type for input: " + targetVar.getType());
            }
            symbolTable.put(varName, inputValue); // Update symbol table with input value
            System.out.println("➡ Kullanıcı girdisi alındı ve '" + varName + "' değişkenine atandı: " + inputValue);
        } catch (NumberFormatException e) {
            return error("Input type mismatch for variable '" + varName + "'. Expected " + targetVar.getType() + ".");
        }

        if (!match(TokenType.RPAREN)) return error("Expected ')' after input variable.");
        return true;
    }

    /**
     * Parses and executes a return statement (dede).
     * Syntax: dede Expression
     *
     * @return true if successful, false otherwise.
     */
    private boolean parseReturn() {
        if (!match(TokenType.RETURN)) return false; // dede
        Value returnValue;
        try {
            returnValue = evaluateExpression(); // Evaluate the expression to get the return value
        } catch (RuntimeException e) {
            return error("Error in return expression: " + e.getMessage());
        }
        System.out.println("➡ Return ifadesi yürütüldü. Değer: " + returnValue);
        hadReturn = true; // Set flag to stop further execution in the current scope/program
        return true;
    }

    /**
     * Parses a break statement (tontiş).
     * Sets the hadBreak flag to true to signal outer loops/blocks to terminate.
     * Syntax: tontiş
     *
     * @return true if successful, false otherwise.
     */
    private boolean parseBreak() {
        if (!match(TokenType.BREAK)) return false; // tontiş
        hadBreak = true; // Signal break to parent loop/block
        System.out.println("➡ 'tontiş' (break) ifadesi yürütüldü.");
        return true;
    }

    /**
     * Parses a continue statement (şapşik).
     * Sets the hadContinue flag to true to signal outer loops/blocks to skip to next iteration.
     * Syntax: şapşik
     *
     * @return true if successful, false otherwise.
     */
    private boolean parseContinue() {
        if (!match(TokenType.CONTINUE)) return false; // şapşik
        hadContinue = true; // Signal continue to parent loop/block
        System.out.println("➡ 'şapşik' (continue) ifadesi yürütüldü.");
        return true;
    }

    /**
     * Parses and executes an if control structure (döfenşimos).
     * Syntax: döfenşimos ( ComparisonExpression ) } StatementList { tontiş
     *
     * @return true if successful, false otherwise.
     */
    private boolean parseIfStructure() {
        if (!match(TokenType.IF)) return false; // döfenşimos
        if (!match(TokenType.LPAREN)) return error("Expected '(' after 'döfenşimos'.");

        Value conditionResult;
        try {
            conditionResult = evaluateExpression(); // Evaluate the condition
        } catch (RuntimeException e) {
            return error("Error in 'if' condition: " + e.getMessage());
        }

        if (conditionResult == null || conditionResult.getType() != ÇizgiKodType.BOOLEAN) {
            return error("If condition must evaluate to a boolean value (rik/morti).");
        }

        if (!match(TokenType.RPAREN)) return error("Expected ')' after 'if' condition.");
        if (!match(TokenType.LBRACE)) return error("Expected '}' (LBRACE) for 'if' block.");

        boolean conditionMet = conditionResult.asBoolean();
        int savedIndex = index; // Save current index to potentially skip the block

        // Parse and execute the 'if' block if the condition is true
        if (conditionMet) {
            System.out.println("➡ 'döfenşimos' koşulu DOĞRU. Blok yürütülüyor.");
            if (!parseBlock()) { // Parse and execute statements within the block
                return false;
            }
        } else {
            System.out.println("➡ 'döfenşimos' koşulu YANLIŞ. Blok atlanıyor.");
            // If condition is false, skip the 'if' block
            if (!skipBlock()) {
                return error("Error skipping 'if' block.");
            }
        }

        // After 'if' block, check for optional 'else' (ornitorenk)
        if (peek().type == TokenType.ELSE) {
            match(TokenType.ELSE); // Consume 'ornitorenk'
            if (!match(TokenType.LBRACE)) return error("Expected '}' (LBRACE) for 'else' block.");

            if (!conditionMet) { // Execute 'else' block only if 'if' condition was false
                System.out.println("➡ 'ornitorenk' (else) bloğu yürütülüyor.");
                if (!parseBlock()) { // Parse and execute statements within the 'else' block
                    return false;
                }
            } else {
                System.out.println("➡ 'ornitorenk' (else) bloğu atlanıyor.");
                // If 'if' condition was true, skip the 'else' block
                if (!skipBlock()) {
                    return error("Error skipping 'else' block.");
                }
            }
        }

        if (!match(TokenType.BREAK)) return error("Expected 'tontiş' (BREAK) after 'if'/'else' structure.");
        System.out.println("➡ 'döfenşimos' yapısı tamamlandı.");
        return true;
    }

    /**
     * Parses and executes a while control structure (pepe).
     * Syntax: pepe ( ComparisonExpression ) } StatementList { tontiş
     *
     * @return true if successful, false otherwise.
     */
    private boolean parseWhileStructure() {
        if (!match(TokenType.WHILE)) return false; // pepe
        if (!match(TokenType.LPAREN)) return error("Expected '(' after 'pepe'.");

        int conditionStartIndex = index; // Save index to re-evaluate condition in loop

        if (!match(TokenType.RPAREN)) return error("Expected ')' after 'while' condition.");
        if (!match(TokenType.LBRACE)) return error("Expected '}' (LBRACE) for 'while' block.");

        int blockStartIndex = index; // Save index for block content

        boolean loopActive = true;
        while (loopActive) {
            // Reset continue/break flags for each iteration
            hadContinue = false;
            hadBreak = false;

            // Re-evaluate the condition
            index = conditionStartIndex; // Reset index to condition
            Value conditionResult;
            try {
                conditionResult = evaluateExpression();
            } catch (RuntimeException e) {
                return error("Error in 'while' condition: " + e.getMessage());
            }

            if (conditionResult == null || conditionResult.getType() != ÇizgiKodType.BOOLEAN || !conditionResult.asBoolean()) {
                loopActive = false; // Condition false, exit loop
                System.out.println("➡ 'pepe' koşulu YANLIŞ. Döngü sonlandırılıyor.");
            } else {
                System.out.println("➡ 'pepe' koşulu DOĞRU. Döngü yürütülüyor.");
                // Execute the block
                index = blockStartIndex; // Reset index to start of block
                if (!parseBlock()) {
                    return false; // Error within the block
                }
            }

            if (hadBreak) {
                System.out.println("➡ 'tontiş' (break) ile döngüden çıkıldı.");
                break; // Exit the while loop
            }
            // If hadContinue is true, loop will restart after this iteration
        }

        // After loop, find and consume the closing brace and break token
        // We need to skip to the end of the block if the loop exited early due to condition or break.
        if (peek().type != TokenType.RBRACE) { // If not at the closing brace, means we exited loop early
             // Find the end of the block by manually advancing
            int balance = 1; // For the already consumed LBRACE
            while (index < tokens.size()) {
                if (tokens.get(index).type == TokenType.LBRACE) { // '}'
                    balance++;
                } else if (tokens.get(index).type == TokenType.RBRACE) { // '{'
                    balance--;
                }
                index++;
                if (balance == 0) break; // Found the matching closing brace
            }
        }
        if (!match(TokenType.RBRACE)) return error("Expected closing brace '{' for 'while' block.");
        if (!match(TokenType.BREAK)) return error("Expected 'tontiş' (BREAK) after 'while' structure.");

        System.out.println("➡ 'pepe' yapısı tamamlandı.");
        return true;
    }

    /**
     * Parses and executes a block of statements within control structures.
     * This method assumes it's called after the opening brace '{' has been consumed.
     * It will consume statements until a matching closing brace '}' is found, or an error/return/break/continue occurs.
     *
     * @return true if the block was successfully parsed and executed, false otherwise.
     */
    private boolean parseBlock() {
        int originalIndex = index; // Save index before parsing the block
        boolean blockSuccess = true;

        // Loop through statements within the block
        while (index < tokens.size() && peek().type != TokenType.RBRACE && !hadReturn && !hadBreak && !hadContinue) {
            // Each statement inside a block still needs to start with ^_^
            if (peek().type != TokenType.START_LINE) {
                return error("Expected START_LINE token (^_^)." + (index < tokens.size() ? " Found: " + currentToken().getLexeme() : "EOF"));
            }
            if (!parseStatement()) {
                blockSuccess = false;
                break; // Exit block parsing on error
            }
        }
        return blockSuccess;
    }

    /**
     * Skips tokens until the matching closing brace '}' for the current block.
     * Used when an 'if' condition is false or an 'else' block is skipped.
     *
     * @return true if the block was successfully skipped, false otherwise.
     */
    private boolean skipBlock() {
        int balance = 1; // We assume LBRACE (}) was just matched
        while (index < tokens.size() && balance > 0) {
            if (tokens.get(index).type == TokenType.LBRACE) { // '}'
                balance++;
            } else if (tokens.get(index).type == TokenType.RBRACE) { // '{'
                balance--;
            }
            index++;
        }
        if (balance != 0) {
            return error("Unmatched curly brace while skipping block.");
        }
        return true;
    }


    /**
     * Evaluates an expression and returns its Value. This is the core of the interpreter.
     * It handles arithmetic, comparison, and boolean expressions.
     *
     * @return The Value object representing the result of the expression.
     * @throws RuntimeException if a semantic error (e.g., type mismatch, undefined variable) occurs.
     */
    private Value evaluateExpression() throws RuntimeException {
        // Start with boolean expression (lowest precedence for logical operators)
        Value left = parseComparisonExpr(); // Handles arithmetic and comparison

        while (peek().type == TokenType.AND || peek().type == TokenType.AND_AND ||
               peek().type == TokenType.OR || peek().type == TokenType.OR_OR) {
            TokenType operator = consume().getType(); // Consume logical operator
            Value right = parseComparisonExpr();

            if (left.getType() != ÇizgiKodType.BOOLEAN || right.getType() != ÇizgiKodType.BOOLEAN) {
                throw new RuntimeException("Logical operator '" + operator.name() + "' expects boolean operands.");
            }

            Boolean leftBool = left.asBoolean();
            Boolean rightBool = right.asBoolean();

            if (operator == TokenType.AND || operator == TokenType.AND_AND) {
                left = new Value(ÇizgiKodType.BOOLEAN, leftBool && rightBool);
            } else if (operator == TokenType.OR || operator == TokenType.OR_OR) {
                left = new Value(ÇizgiKodType.BOOLEAN, leftBool || rightBool);
            }
        }
        return left;
    }

    /**
     * Parses and evaluates a comparison expression.
     * Syntax: Expression ComparisonOperator Expression
     * Operators: __, !_, -:, :-, -:_, :-_
     *
     * @return A Value object of type BOOLEAN.
     * @throws RuntimeException if types are incompatible for comparison.
     */
    private Value parseComparisonExpr() throws RuntimeException {
        Value left = parseArithmeticExpr(); // Evaluate left side of comparison

        // Check for comparison operators
        TokenType operator = peek().type;
        if (operator == TokenType.EQUAL || operator == TokenType.NOT_EQUAL ||
            operator == TokenType.LESS_THAN || operator == TokenType.GREATER_THAN ||
            operator == TokenType.LESS_EQUAL || operator == TokenType.GREATER_EQUAL) {

            consume(); // Consume the comparison operator
            Value right = parseArithmeticExpr(); // Evaluate right side

            // Perform comparison based on types
            if (left.getType() == ÇizgiKodType.INT && right.getType() == ÇizgiKodType.INT) {
                return compareInts(left.asInt(), right.asInt(), operator);
            } else if (left.getType() == ÇizgiKodType.FLOAT && right.getType() == ÇizgiKodType.FLOAT) {
                return compareFloats(left.asFloat(), right.asFloat(), operator);
            } else if ((left.getType() == ÇizgiKodType.INT && right.getType() == ÇizgiKodType.FLOAT)) {
                return compareFloats(left.asInt().doubleValue(), right.asFloat(), operator);
            } else if ((left.getType() == ÇizgiKodType.FLOAT && right.getType() == ÇizgiKodType.INT)) {
                return compareFloats(left.asFloat(), right.asInt().doubleValue(), operator);
            } else if (left.getType() == ÇizgiKodType.STRING && right.getType() == ÇizgiKodType.STRING) {
                return compareStrings(left.asString(), right.asString(), operator);
            } else if (left.getType() == ÇizgiKodType.BOOLEAN && right.getType() == ÇizgiKodType.BOOLEAN) {
                return compareBooleans(left.asBoolean(), right.asBoolean(), operator);
            } else if (left.getType() == ÇizgiKodType.CHAR && right.getType() == ÇizgiKodType.CHAR) {
                return compareChars(left.getData().toString().charAt(0), right.getData().toString().charAt(0), operator);
            } else {
                throw new RuntimeException("Unsupported types for comparison: " + left.getType() + " and " + right.getType());
            }
        }
        return left; // If no comparison operator, return the evaluated left expression
    }

    // Helper methods for comparisons
    private Value compareInts(Integer l, Integer r, TokenType op) {
        boolean result;
        switch (op) {
            case EQUAL: result = (l.equals(r)); break;
            case NOT_EQUAL: result = (!l.equals(r)); break;
            case LESS_THAN: result = (l < r); break;
            case GREATER_THAN: result = (l > r); break;
            case LESS_EQUAL: result = (l <= r); break;
            case GREATER_EQUAL: result = (l >= r); break;
            default: throw new RuntimeException("Invalid integer comparison operator: " + op);
        }
        return new Value(ÇizgiKodType.BOOLEAN, result);
    }

    private Value compareFloats(Double l, Double r, TokenType op) {
        boolean result;
        switch (op) {
            case EQUAL: result = (l.equals(r)); break;
            case NOT_EQUAL: result = (!l.equals(r)); break;
            case LESS_THAN: result = (l < r); break;
            case GREATER_THAN: result = (l > r); break;
            case LESS_EQUAL: result = (l <= r); break;
            case GREATER_EQUAL: result = (l >= r); break;
            default: throw new RuntimeException("Invalid float comparison operator: " + op);
        }
        return new Value(ÇizgiKodType.BOOLEAN, result);
    }

    private Value compareStrings(String l, String r, TokenType op) {
        boolean result;
        switch (op) {
            case EQUAL: result = (l.equals(r)); break;
            case NOT_EQUAL: result = (!l.equals(r)); break;
            case LESS_THAN: result = (l.compareTo(r) < 0); break; // Lexicographical
            case GREATER_THAN: result = (l.compareTo(r) > 0); break; // Lexicographical
            case LESS_EQUAL: result = (l.compareTo(r) <= 0); break; // Lexicographical
            case GREATER_EQUAL: result = (l.compareTo(r) >= 0); break; // Lexicographical
            default: throw new RuntimeException("Invalid string comparison operator: " + op);
        }
        return new Value(ÇizgiKodType.BOOLEAN, result);
    }

    private Value compareBooleans(Boolean l, Boolean r, TokenType op) {
        boolean result;
        switch (op) {
            case EQUAL: result = (l.equals(r)); break;
            case NOT_EQUAL: result = (!l.equals(r)); break;
            default: throw new RuntimeException("Invalid boolean comparison operator: " + op);
        }
        return new Value(ÇizgiKodType.BOOLEAN, result);
    }

    private Value compareChars(Character l, Character r, TokenType op) {
        boolean result;
        switch (op) {
            case EQUAL: result = (l.equals(r)); break;
            case NOT_EQUAL: result = (!l.equals(r)); break;
            case LESS_THAN: result = (l < r); break;
            case GREATER_THAN: result = (l > r); break;
            case LESS_EQUAL: result = (l <= r); break;
            case GREATER_EQUAL: result = (l >= r); break;
            default: throw new RuntimeException("Invalid char comparison operator: " + op);
        }
        return new Value(ÇizgiKodType.BOOLEAN, result);
    }

    /**
     * Parses and evaluates an arithmetic expression (addition, subtraction).
     * Syntax: Term ( (._. | ,_,) Term )*
     * Operators: ._. (PLUS), ,_, (MINUS)
     *
     * @return The Value object representing the result.
     * @throws RuntimeException if type mismatches or unsupported operations occur.
     */
    private Value parseArithmeticExpr() throws RuntimeException {
        Value left = parseTerm(); // Start with a term

        while (peek().type == TokenType.PLUS || peek().type == TokenType.MINUS) {
            TokenType operator = consume().getType(); // Consume operator

            Value right = parseTerm(); // Evaluate right side

            if (left.getType() == ÇizgiKodType.INT && right.getType() == ÇizgiKodType.INT) {
                if (operator == TokenType.PLUS) return new Value(ÇizgiKodType.INT, left.asInt() + right.asInt());
                if (operator == TokenType.MINUS) return new Value(ÇizgiKodType.INT, left.asInt() - right.asInt());
            } else if (left.getType() == ÇizgiKodType.FLOAT && right.getType() == ÇizgiKodType.FLOAT) {
                if (operator == TokenType.PLUS) return new Value(ÇizgiKodType.FLOAT, left.asFloat() + right.asFloat());
                if (operator == TokenType.MINUS) return new Value(ÇizgiKodType.FLOAT, left.asFloat() - right.asFloat());
            } else if ((left.getType() == ÇizgiKodType.INT && right.getType() == ÇizgiKodType.FLOAT) ||
                       (left.getType() == ÇizgiKodType.FLOAT && right.getType() == ÇizgiKodType.INT)) {
                // Promote int to float for mixed operations
                double lVal = (left.getType() == ÇizgiKodType.INT) ? left.asInt().doubleValue() : left.asFloat();
                double rVal = (right.getType() == ÇizgiKodType.INT) ? right.asInt().doubleValue() : right.asFloat();
                if (operator == TokenType.PLUS) return new Value(ÇizgiKodType.FLOAT, lVal + rVal);
                if (operator == TokenType.MINUS) return new Value(ÇizgiKodType.FLOAT, lVal - rVal);
            } else if (left.getType() == ÇizgiKodType.STRING && operator == TokenType.PLUS) { // String concatenation
                return new Value(ÇizgiKodType.STRING, left.asString() + right.toString()); // Convert right to string
            } else {
                throw new RuntimeException("Unsupported types for arithmetic operation '" + operator.name() + "': " + left.getType() + " and " + right.getType());
            }
        }
        return left; // Return the result of the arithmetic expression
    }

    /**
     * Parses and evaluates a term (multiplication, division, modulo, power).
     * Syntax: Factor ( (\ | *_* | %) Factor )*
     * Operators: \ (DIVIDE), *_* (POWER), % (MOD)
     *
     * @return The Value object representing the result.
     * @throws RuntimeException if type mismatches or unsupported operations occur.
     */
    private Value parseTerm() throws RuntimeException {
        Value left = parseFactor(); // Start with a factor

        while (peek().type == TokenType.DIVIDE || peek().type == TokenType.POWER || peek().type == TokenType.MOD) {
            TokenType operator = consume().getType(); // Consume operator

            Value right = parseFactor(); // Evaluate right side

            if (left.getType() == ÇizgiKodType.INT && right.getType() == ÇizgiKodType.INT) {
                if (operator == TokenType.DIVIDE) {
                    if (right.asInt() == 0) throw new RuntimeException("Division by zero.");
                    return new Value(ÇizgiKodType.INT, left.asInt() / right.asInt());
                }
                if (operator == TokenType.POWER) return new Value(ÇizgiKodType.INT, (int) Math.pow(left.asInt(), right.asInt()));
                if (operator == TokenType.MOD) {
                    if (right.asInt() == 0) throw new RuntimeException("Modulo by zero.");
                    return new Value(ÇizgiKodType.INT, left.asInt() % right.asInt());
                }
            } else if (left.getType() == ÇizgiKodType.FLOAT && right.getType() == ÇizgiKodType.FLOAT) {
                if (operator == TokenType.DIVIDE) {
                    if (right.asFloat() == 0.0) throw new RuntimeException("Division by zero.");
                    return new Value(ÇizgiKodType.FLOAT, left.asFloat() / right.asFloat());
                }
                if (operator == TokenType.POWER) return new Value(ÇizgiKodType.FLOAT, Math.pow(left.asFloat(), right.asFloat()));
                if (operator == TokenType.MOD) {
                    if (right.asFloat() == 0.0) throw new RuntimeException("Modulo by zero.");
                    return new Value(ÇizgiKodType.FLOAT, left.asFloat() % right.asFloat());
                }
            } else if ((left.getType() == ÇizgiKodType.INT && right.getType() == ÇizgiKodType.FLOAT) ||
                       (left.getType() == ÇizgiKodType.FLOAT && right.getType() == ÇizgiKodType.INT)) {
                // Promote int to float for mixed operations
                double lVal = (left.getType() == ÇizgiKodType.INT) ? left.asInt().doubleValue() : left.asFloat();
                double rVal = (right.getType() == ÇizgiKodType.INT) ? right.asInt().doubleValue() : right.asFloat();
                if (operator == TokenType.DIVIDE) {
                    if (rVal == 0.0) throw new RuntimeException("Division by zero.");
                    return new Value(ÇizgiKodType.FLOAT, lVal / rVal);
                }
                if (operator == TokenType.POWER) return new Value(ÇizgiKodType.FLOAT, Math.pow(lVal, rVal));
                if (operator == TokenType.MOD) {
                    if (rVal == 0.0) throw new RuntimeException("Modulo by zero.");
                    return new Value(ÇizgiKodType.FLOAT, lVal % rVal);
                }
            } else {
                throw new RuntimeException("Unsupported types for arithmetic operation '" + operator.name() + "': " + left.getType() + " and " + right.getType());
            }
        }
        return left;
    }

    /**
     * Parses and evaluates a factor (number, string, boolean literal, identifier, or parenthesized expression).
     *
     * @return The Value object representing the result.
     * @throws RuntimeException if an undefined variable or invalid factor is encountered.
     */
    private Value parseFactor() throws RuntimeException {
        Token token = peek();
        if (token.type == TokenType.NUMBER || token.type == TokenType.STRING_LITERAL ||
            token.type == TokenType.TRUE || token.type == TokenType.FALSE) {
            consume(); // Consume the literal token
            return token.getValue(); // Return its pre-parsed Value
        } else if (token.type == TokenType.IDENTIFIER) {
            consume(); // Consume identifier
            if (!symbolTable.containsKey(token.getLexeme())) {
                throw new RuntimeException("Undefined variable: '" + token.getLexeme() + "'");
            }
            return symbolTable.get(token.getLexeme()); // Return variable's value
        } else if (match(TokenType.LPAREN)) { // Handle parentheses for grouping expressions
            Value value = evaluateExpression(); // Recursively evaluate the expression inside
            if (!match(TokenType.RPAREN)) {
                throw new RuntimeException("Expected ')' after expression in parentheses.");
            }
            return value;
        }
        throw new RuntimeException("Unexpected token in expression: " + token.getLexeme() + " (Type: " + token.getType() + ")");
    }

    /**
     * Consumes the current token if it matches the expected type.
     *
     * @param type The expected TokenType.
     * @return true if the token matched and was consumed, false otherwise.
     */
    private boolean match(TokenType type) {
        if (index < tokens.size() && tokens.get(index).getType() == type) {
            index++;
            return true;
        }
        return false;
    }

    /**
     * Consumes the current token and returns it.
     *
     * @return The consumed Token.
     * @throws RuntimeException if trying to consume past the end of tokens.
     */
    private Token consume() {
        if (index >= tokens.size()) {
            throw new RuntimeException("Unexpected end of input.");
        }
        return tokens.get(index++);
    }

    /**
     * Returns the current token without consuming it.
     *
     * @return The current Token or a dummy UNKNOWN token if at EOF.
     */
    private Token peek() {
        return index < tokens.size() ? tokens.get(index) : new Token(TokenType.UNKNOWN, "EOF");
    }

    /**
     * Returns the previous token.
     *
     * @return The previous Token.
     */
    private Token previousToken() {
        if (index > 0 && index <= tokens.size()) {
            return tokens.get(index - 1);
        }
        return new Token(TokenType.UNKNOWN, "N/A"); // Should not happen in normal flow
    }

    /**
     * Returns the token at the current index.
     *
     * @return The current Token.
     */
    private Token currentToken() {
        return index < tokens.size() ? tokens.get(index) : new Token(TokenType.UNKNOWN, "EOF");
    }

    /**
     * Prints an error message to System.err and returns false for parsing/execution failure.
     *
     * @param message The error description.
     * @return Always returns false to indicate failure.
     */
    private boolean error(String message) {
        System.err.println("❌ Hata: " + message + " (Satır: " + (index < tokens.size() ? currentToken().getLexeme() : "EOF") + " - Tip: " + (index < tokens.size() ? currentToken().getType() : "EOF") + ")");
        return false;
    }
}
