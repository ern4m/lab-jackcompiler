package br.ufma.ecp;

import static br.ufma.ecp.token.TokenType.*;

import br.ufma.ecp.token.Token;
import br.ufma.ecp.token.TokenType;

public class Parser {

    private static class ParseError extends RuntimeException {
    }

    private Scanner scan;
    private Token currentToken;
    private Token peekToken;

    private StringBuilder xmlOutput = new StringBuilder();

    public Parser (byte[] input) {
        scan = new Scanner(input);

        nextToken();
    }

    private void nextToken() {
        currentToken = peekToken;
        peekToken = scan.nextToken();
    }

    // Utility Functions

    public String XMLOutput() {
        return xmlOutput.toString();
    }


    // Formats and appends non terminal tokens to the XMLOutput
    private void printNonTerminal(String nterminal) {
        xmlOutput.append(String.format("<%s>\r\n", nterminal));
    }

    // Used to verify the next token to be parsed
    boolean peekTokenIs(TokenType type) {
        return peekToken.type == type;
    }

    // Used to verify the type of current token that's being parsed
    boolean currentTokenIs(TokenType type) {
        return currentToken.type == type;
    }


    // Verifies if the peekToken is the one expected
    private void expectPeek(TokenType... types) {
        for (TokenType type : types) {
            if (peekToken.type == type) {
                expectPeek(type);
                return;
            }
        }
        // throw new Error("Syntax error");
        throw error(peekToken, "Expected a statement");
    }

    private void expectPeek(TokenType type) {
        if (peekToken.type == type) {
            nextToken();
            xmlOutput.append(String.format("%s\r\n", currentToken.toString()));
        } else {
            // throw new Error("Syntax error - expected " + type + " found " +
            // peekToken.type);
            throw error(peekToken, "Expected " + type.value);
        }
    }

    // Error Functions

    private static void report(int line, String where, String message) {
        System.err.println(
            "[line " + line + "] Error" + where + ": " + message);
    }

    private ParseError error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.value() + "'", message);
        }
        return new ParseError();
    }
}
