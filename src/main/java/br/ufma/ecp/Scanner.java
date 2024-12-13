package br.ufma.ecp;

import static br.ufma.ecp.token.TokenType.*;

import java.nio.charset.StandardCharsets;

import br.ufma.ecp.token.IdentifierToken;
import br.ufma.ecp.token.IntegerToken;
import br.ufma.ecp.token.KeywordToken;
import br.ufma.ecp.token.StringToken;
import br.ufma.ecp.token.SymbolToken;

import br.ufma.ecp.token.Token;
import br.ufma.ecp.token.TokenType;

public class Scanner {

    private byte[] input;
    private int current;
    private int start;
    private int line = 1;

    public Scanner(byte[] input) {
        this.input = input;
        current = 0;
        start = 0;
    }

    private void skipBlockComments() {
        boolean endComment = false;
        advance();
        while (!endComment) {
            advance();
            char ch = peek();

            if (ch == '\n')
                line++;

            if (ch == 0) { // eof, lexical error
                System.exit(1);
            }

            if (ch == '*') {
                for (ch = peek(); ch == '*'; advance(), ch = peek())
                    ;
                if (ch == '/') {
                    endComment = true;
                    advance();
                }
            }

        }
    }

    private void skipLineComments() {
        for (char ch = peek(); ch != '\n' && ch != 0; advance(), ch = peek())
            if (ch == '\n')
                line++;
    }

    private char peekNext() {
        int next = current + 1;
        if (next < input.length) {
            return (char) input[next];
        } else {
            return 0;
        }
    }

    private void skipWhitespace() {
        char ch = peek();
        while (ch == ' ' || ch == '\r' || ch == '\t' || ch == '\n') {

            if (ch == '\n')
                line++;

            advance();
            ch = peek();
        }
    }

    public Token nextToken() {

        skipWhitespace();

        start = current;
        char ch = peek();

        if (Character.isDigit(ch)) {
            return number();
        }

        if (isAlpha(ch)) {
            return identifier();
        }

        switch (ch) {
            case '/':
                if (peekNext() == '/') {
                    skipLineComments();
                    return nextToken();
                } else if (peekNext() == '*') {
                    skipBlockComments();
                    return nextToken();
                } else {
                    advance();
                    return new Token(TokenType.SLASH, line);
                }
            case 0:
                return new Token(TokenType.EOF, line);
            default:

                if (Character.isDigit(ch)) {
                    return number();
                }

                if (Character.isLetter(ch)) {
                    return identifier();
                }

                if (TokenType.isSymbol(ch)) {
                    return symbol();
                }

                throw new Error(line+":Unexpected character: " + peek());
        }
    }

    private Token identifier() {
        while (isAlphaNumeric(peek()))
            advance();

        String id = new String(input, start, current - start, StandardCharsets.UTF_8);
        TokenType type = TokenType.keyword(id);
        if (type == null) {
            type = TokenType.keyword(id);
            return new IdentifierToken(id, line);
        } else {
            return new KeywordToken(type, line);
        }
    }

    private Token number() {
        while (Character.isDigit(peek())) {
            advance();
        }

        String num = new String(input, start, current - start, StandardCharsets.UTF_8);
        return new IntegerToken(num, line);
    }

    private Token string() {
        advance();
        start = current;
        while (peek() != '"' && peek() != 0) {
            advance();
        }
        String s = new String(input, start, current - start, StandardCharsets.UTF_8);
        Token token = new StringToken(s, line);
        advance();
        return token;
    }

    private SymbolToken symbol() {
        var ch = peek();
        advance();
        return new SymbolToken(TokenType.fromValue(String.valueOf(ch)), line);
    }

    private void advance() {
        char ch = peek();
        if (ch != 0) {
            current++;
        }
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || Character.isDigit((c));
    }

    private char peek() {
        if (current < input.length)
            return (char) input[current];
        return 0;
    }

}
