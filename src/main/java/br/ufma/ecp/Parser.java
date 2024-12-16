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
        while (isOperator(peekToken.type)) {
            var op = peekToken.type;
            expectPeek(peekToken.type);
            parseTerm();
        }
        printNonTerminal("/expression");
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
                } else if (peekTokenIs(LBRACKET)) {
                    expectPeek(LBRACKET);
                    parseExpression();
                    expectPeek(RBRACKET);
                }
                break;
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
        printNonTerminal("/term");
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
        switch (peekToken.type) { // will match any possible value for an statement
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

    // parsing While

    void parseWhile() {
        printNonTerminal("whileStatement");
    
        expectPeek(WHILE);
        expectPeek(LPAREN);
        parseExpression();
    
        expectPeek(RPAREN);
        expectPeek(LBRACE);
        parseStatements();
    
        expectPeek(RBRACE);
        printNonTerminal("/whileStatement");
    }

    // Parsing If

    void parseIf() {
        printNonTerminal("ifStatement");
    
        expectPeek(IF);
        expectPeek(LPAREN);
        parseExpression();
        expectPeek(RPAREN);
    
        expectPeek(LBRACE);
        parseStatements();
        expectPeek(RBRACE);
    
        if (peekTokenIs(ELSE))
        {
            expectPeek(ELSE);
            expectPeek(LBRACE);
            parseStatements();
            expectPeek(RBRACE);
        }
    
        printNonTerminal("/ifStatement");
    }

    // Parsing Return

    void parseReturn() {
        printNonTerminal("returnStatement");
        expectPeek(RETURN);
        if (!peekTokenIs(SEMICOLON)) {
            parseExpression();
        }
        expectPeek(SEMICOLON);
        printNonTerminal("/returnStatement");
    }

    //Parsing Do

    void parseDo() {
        printNonTerminal("doStatement");
        expectPeek(DO);
        expectPeek(IDENTIFIER);
        parseSubroutineCall();
        expectPeek(SEMICOLON);
        printNonTerminal("/doStatement");
    }

    //Parsing VarDec

    // 'var' type varName ( ',' varName)* ';'

    void parseVarDec() {
        printNonTerminal("varDec");
        expectPeek(VAR);

        // 'int' | 'char' | 'boolean' | className
        expectPeek(INT, CHAR, BOOLEAN, IDENTIFIER);

        expectPeek(IDENTIFIER);

        while (peekTokenIs(COMMA)) {
            expectPeek(COMMA);
            expectPeek(IDENTIFIER);
        }

        expectPeek(SEMICOLON);
        printNonTerminal("/varDec");
    }

    //Parsing Class VarDec

        
    // classVarDec → ( 'static' | 'field' ) type varName ( ',' varName)* ';'

    void parseClassVarDec() {
        printNonTerminal("classVarDec");
        expectPeek(FIELD, STATIC);

        // 'int' | 'char' | 'boolean' | className
        expectPeek(INT, CHAR, BOOLEAN, IDENTIFIER);
        String type = currentToken.value();

        expectPeek(IDENTIFIER);
        String name = currentToken.value();

        while (peekTokenIs(COMMA)) {
            expectPeek(COMMA);
            expectPeek(IDENTIFIER);
            name = currentToken.value();
        }

        expectPeek(SEMICOLON);
        printNonTerminal("/classVarDec");
    }

    //Parsing SubroutineDec

    void parseSubroutineDec() {
        printNonTerminal("subroutineDec");


        expectPeek(CONSTRUCTOR, FUNCTION, METHOD);
        var subroutineType = currentToken.type;

        // 'int' | 'char' | 'boolean' | className
        expectPeek(VOID, INT, CHAR, BOOLEAN, IDENTIFIER);
        expectPeek(IDENTIFIER);

        var functionName = "." + currentToken.value();

        expectPeek(LPAREN);
        parseParameterList();
        expectPeek(RPAREN);
        parseSubroutineBody(functionName, subroutineType);

        printNonTerminal("/subroutineDec");
    }
    
    //Parsing ParameterList

    void parseParameterList() {
        printNonTerminal("parameterList");
    
        if (!peekTokenIs(RPAREN)) // verifica se tem pelo menos uma expressao
        {
            expectPeek(INT, CHAR, BOOLEAN, IDENTIFIER);
    
            expectPeek(IDENTIFIER);
    
            while (peekTokenIs(COMMA)) {
                expectPeek(COMMA);
                expectPeek(INT, CHAR, BOOLEAN, IDENTIFIER);
    
                expectPeek(IDENTIFIER);
            }
        }
    
        printNonTerminal("/parameterList");
    }

    //Parsing SubroutineBody

    void parseSubroutineBody(String functionName, TokenType subroutineType) {
        printNonTerminal("subroutineBody");
        expectPeek(LBRACE);
        while (peekTokenIs(VAR)) {
            parseVarDec();
        }
    
        parseStatements();
        expectPeek(RBRACE);
        printNonTerminal("/subroutineBody");
    }

    //Parsing SubroutineCall

    // subroutineCall -> subroutineName '(' expressionList ')' | (className|varName)
    // '.' subroutineName '(' expressionList ')

    void parseSubroutineCall() {
        var nArgs = 0; // n será usado, mas é necessário pras chamadas de parsing de expressões
        var ident = currentToken.value();
        var symbol = symbolTable.resolve(ident); // classe ou objeto

        if (peekTokenIs(LPAREN)) { // método da propria classe
            expectPeek(LPAREN);
            nArgs = parseExpressionList() + 1;
            expectPeek(RPAREN);
        } else {
            // pode ser um metodo de um outro objeto ou uma função
            expectPeek(DOT);
            expectPeek(IDENTIFIER); // nome da função
            expectPeek(LPAREN);
            nArgs += parseExpressionList();
            expectPeek(RPAREN);
        }

    }

    //Parsing VarDec
    //Parsing VarDec
    //Parsing VarDec
    //Parsing VarDec
    //Parsing VarDec
    //Parsing VarDec

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
