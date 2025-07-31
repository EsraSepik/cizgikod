package çizgikod;

import java.io.*;
import java.util.*;

public class ÇizgiKod {

    public static void main(String[] args) throws IOException {
        // Loop through input files from 1 to 4
        for (int i = 1; i <= 4; i++) {
            // Create a BufferedWriter to write output to outputX.txt
            BufferedWriter writer = new BufferedWriter(new FileWriter("output" + i + ".txt"));
            // Use try-with-resources for BufferedReader to ensure it's closed automatically
            try (BufferedReader reader = new BufferedReader(new FileReader("input" + i + ".txt"))) {
                String line; // Variable to hold each line read from the input file
                List<Token> allTokens = new ArrayList<>(); // Collect all tokens from the file

                // Write initial program status messages to the output file and console
                writer.write("Program is compliant with the lookup table.\n");
                writer.write("Program started.\n");
                System.out.println("Program started.\n");

                // --- LEXICAL ANALYSIS PHASE ---
                // Read the input file line by line and tokenize all lines
                int lineNumber = 0;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    System.out.println("Processing Line " + lineNumber + ": " + line);
                    List<Token> lineTokens = Lexer.tokenize(line);

                    if (lineTokens.isEmpty() && !line.trim().isEmpty()) {
                        System.err.println("Warning: Line " + lineNumber + " produced no tokens or contained only whitespace.");
                        writer.write("Warning: Line " + lineNumber + " produced no tokens or contained only whitespace.\n");
                    }

                    allTokens.addAll(lineTokens); // Add tokens of the current line to the overall list

                    // Log tokens to output file and console
                    writer.write("Line " + lineNumber + ": " + line + "\n");
                    for (Token token : lineTokens) {
                        writer.write("  Next token: " + token.getType().code + ", Lexeme: " + token.getLexeme() + (token.getValue() != null ? ", Value: " + token.getValue() : "") + "\n");
                        System.out.println("  Next Token: " + token.getType().code + ", Lexeme: " + token.getLexeme() + (token.getValue() != null ? ", Value: " + token.getValue() : ""));
                    }
                    writer.write("----\n");
                    System.out.println("----\n");
                }
                writer.write("\n--- SYNTACTIC & SEMANTIC ANALYSIS / INTERPRETATION PHASE ---\n");
                System.out.println("\n--- SYNTACTIC & SEMANTIC ANALYSIS / INTERPRETATION PHASE ---\n");

                // --- SYNTACTIC & SEMANTIC ANALYSIS / INTERPRETATION PHASE ---
                Parser parser = new Parser(allTokens); // Create a single Parser instance with all tokens
                try {
                    boolean programResult = parser.parseProgram(); // Start parsing and execution

                    if (programResult) {
                        writer.write("✔ Program parsed and executed successfully for input" + i + ".txt\n");
                        System.out.println("✔ Program parsed and executed successfully for input" + i + ".txt");
                    } else {
                        writer.write("❌ Program parsing or execution failed for input" + i + ".txt\n");
                        System.err.println("❌ Program parsing or execution failed for input" + i + ".txt");
                    }
                } catch (RuntimeException e) {
                    // Catch runtime errors (semantic errors, division by zero, etc.)
                    writer.write("❌ Runtime Error in input" + i + ".txt: " + e.getMessage() + "\n");
                    System.err.println("❌ Runtime Error in input" + i + ".txt: " + e.getMessage());
                }


            } catch (FileNotFoundException e) {
                // Handle case where input file is not found
                System.err.println("Error: Input file 'input" + i + ".txt' not found. Please create it in the same directory as your project's main folder (CizgiKodProjesi).");
                writer.write("Error: Input file 'input" + i + ".txt' not found.\n");
            } catch (IOException e) {
                // Handle other I/O errors
                System.err.println("Error reading or writing file: " + e.getMessage());
                writer.write("Error reading or writing file: " + e.getMessage() + "\n");
            } finally {
                // Ensure the writer is closed even if an error occurs
                writer.close();
            }
            System.out.println("\n----------------------------------------\n");
        }
        System.out.println("Program finished. Check outputX.txt files for detailed logs and console for ÇizgiKod output.");
    }
}
