package br.ufma.ecp;

import java.util.Arrays;
import java.util.List;

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

    // // Expression Parsing

    // Will parse an expression
    void parseExpression() {
        printNonTerminal("expression");
        parseTerm(); // an expression is given in the shape of: expr => term (op term)*
        while (isOperator(peekToken: type)) {
            var op = peekToken.type;
            expectPeek(peekToken.type);
            parseTerm();

        }
        printNonTerminal("/expression")
    }

    // Since one expression is defined by terms, we have to parse terms accordingly to the syntax
    void parseTerm() {
        printNonTerminal("term");
        switch (peekToken.type) {
            case INTEGER:
                expectPeek(INTEGER);
                break;
            case STRING:
                expectPeek(STRING);
                break;
            case NULL:
            case TRUE:
            case FALSE:
                expectPeek(FALSE, NULL, TRUE);
                break;
            case THIS:
                expectPeek(THIS);
            case IDENTIFIER:
                expectPeek(IDENTIFIER);
                if (peekTokenIs(LPAREN) || peekTokenIs(DOT)) {
                    parseSubroutineCall();
                } else {
                    if (peekTokenIs(LBRACKET)) {
                        expectPeek(LBRACKET);
                        parseExpression();
                        expectPeek(RBRACKET);
                    }
                }
            case LPAREN:
                expectPeek(LPAREN);
                parseExpression();
                expectPeek(RPAREN);
                break;
            case MINUS:
            case NOT:
                expectPeek(MINUS, NOT);
                var op  = currentToken.type;
                parseTerm();
                break;
            default:
                throw error(peekToken,  "term expected");
        }
    }

    // In order to parse terms we have to parse Subroutine calls:
    void parseSubroutineCall() {
        var nArgs = 0;
        var ident = currentToken.value();

        if(peekTokenIs(LPAREN)) { // case for classe's own method
            expectPeek(LPAREN); // method(expressionList)
            nArgs = parseExpressionList() + 1;
            expectPeek(RPAREN);
        } else { // case for an method of other object or an function
            expectPeek(DOT); // .funcName(expressionList)
            expectPeek(IDENTIFIER);
            expectPeek(LPAREN);
            nArgs += parseExpressionList();
            expectPeek(RPAREN);
        }
    }

    // In order to parse subroutine calls we have to parse expression lists:
   int parseExpressionList() {
        printNonTerminal("expressionList");
        var nArgs = 0;

        if (!peekTokenIs(RPAREN)) { // verifies if next token isn't an RPAREN
            parseExpression();
            nArgs = 1;
        }

        while (peekTokenIs(COMMA)) {
            expectPeek(COMMA);
            parseExpression();
            nArgs++;
        }

        printNonTerminal("/expressionList");

        return nArgs;
    }

    // // Statement Parsing

    // Functions to mannage statement parsing
    void parseStatement() {
        switch (peekTokenIs.type) { // will match any possible value for an statement
            case LET:
                parseLet();
                break;
            case WHILE:
                parseWhile();
                break;
            case IF:
                parseIf();
                break;
            case RETURN:
                parseReturn();
                break;
            case DO:
                parseDo();
                break;
            default:
                throw error(peekToken, "Expected an statement");
        }
    }

    //  Will parse statements as long the peekToken is an valid statement
    void parseStatements() {
        printNonTerminal("statements");

        List<TokenType> validStatements = Arrays.asList( // current valid statements
                                            TokenType.WHILE,
                                            TokenType.IF,
                                            TokenType.LET,
                                            TokenType.DO,
                                            TokenType.RETURN);

        while (validStatements.contains(peekToken.type)) {
            parseStatement();
        }
        printNonTerminal("/statements");
    }

    // parsing an LET statement
    void parseLet() {
        var isArray = false;

        printNonTerminal("letStatement"); // LET => LET IDENTIFIER ([] || = EXP SEMICOLON)

        expectPeek(LET);
        expectPeek(IDENTIFIER);

        if (peekTokenIs(LBRACKET)) { // if next token after the IDENTIFIER is an LBRACKET will be an array 'definition'
            expectPeek(LBRACKET);
            parseExpression();
            expectPeek(RBRACKET);
            isArray = true;
        }
        expectPeek(EQ);
        parseExpression();
        expectPeek(SEMICOLON);

        printNonTerminal("/letStatement");

    }

    // // Utility Functions

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
