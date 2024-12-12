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

}
