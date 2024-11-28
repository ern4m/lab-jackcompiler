package br.ufma.ecp.token;

import java.util.List;

public class Token {

    public final TokenType type;
    public final String lexeme;
    final int line;

    public Token(TokenType type, String lexeme, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
    }

    public String toString() {
        var type = this.type.toString();
        String valor = lexeme;

        if (TokenType.isSymbol(lexeme.charAt(0))) {
            switch (lexeme.charAt(0)) {
                case '>' -> {
                    valor = "&gt;";
                }
                case '<' -> {
                    valor = "&lt;";
                }
                case '\"' -> {
                    valor = "&quot;";
                }
                case '&' -> {
                    valor = "&amp;";
                }
            }
            type = "symbol";
        }

        if (type.equals("NUMBER"))
            type = "integerConstant";

        if (type.equals("STRING"))
            type = "stringConst";

        if (type.equals("IDENT"))
            type = "identifier";

        if (TokenType.isKeyword(this.type))
            type = "keyword";

        return "<" + type + "> " + valor + " </" + type + ">";
    }

}
