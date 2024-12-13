
## Analisador Sintático
###
#### **Objetivos Específicos**

1. Implementar o analisador sintático seguindo as regras fornecidas no guia.
2. Integrar o analisador sintático ao projeto já existente no repositório do grupo.
3. Garantir que a implementação passe em todos os testes do arquivo `ParserTest.java`.


#### **Mapeando as implementações necessárias**

- Para todas as implementações remover (por hora) as interações com `vmWriter`
- Para todas as implementações remover (por hora) as interações com `SymbolTable`
- Para todas as categorias de implementações (parsing, utilidades e alterações na classe),
realizar um commit individual

---
#### Funções de parsing:
incluir em `Parser.java`:

---

  - **parse()**
```java
void parse() {
    parseClass();
}
```

  - **parseClass()**
```java
void parseClass() {
    printNonTerminal("class");
    expectPeek(CLASS);
    expectPeek(IDENTIFIER);
    className = currentToken.value();
    expectPeek(LBRACE);

    while (peekTokenIs(STATIC) || peekTokenIs(FIELD)) {
        parseClassVarDec();
    }

    while (peekTokenIs(FUNCTION) || peekTokenIs(CONSTRUCTOR) || peekTokenIs(METHOD)) {
        parseSubroutineDec();
    }

    expectPeek(RBRACE);

    printNonTerminal("/class");
}
```

---

  - **parseLet()**

```java
void parseLet() {
    var isArray = false;

    printNonTerminal("letStatement");
    expectPeek(LET);
    expectPeek(IDENTIFIER);

    var symbol = symbolTable.resolve(currentToken.value());

    if (peekTokenIs(LBRACKET)) { // array
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
```

  - **parseIf()**

```java
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
```

  - **parseWhile()**
```java
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
```

  - **parseDo()**
```java
void parseDo() {
    printNonTerminal("doStatement");
    expectPeek(DO);
    expectPeek(IDENTIFIER);
    parseSubroutineCall();
    expectPeek(SEMICOLON);
    printNonTerminal("/doStatement");
}
```

  - **parseReturn()**
```java
void parseReturn() {
    printNonTerminal("returnStatement");
    expectPeek(RETURN);
    if (!peekTokenIs(SEMICOLON)) {
        parseExpression();
    }
    expectPeek(SEMICOLON);
    printNonTerminal("/returnStatement");
}
```

---

  - **parseVarDec()**
```java

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


```

  - **parseClassVarDec()**
```java

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
```

  - **parseSubroutineDec()**
```java
void parseSubroutineDec() {
        printNonTerminal("subroutineDec");

        ifLabelNum = 0;
        whileLabelNum = 0;

        expectPeek(CONSTRUCTOR, FUNCTION, METHOD);
        var subroutineType = currentToken.type;

        // 'int' | 'char' | 'boolean' | className
        expectPeek(VOID, INT, CHAR, BOOLEAN, IDENTIFIER);
        expectPeek(IDENTIFIER);

        var functionName = className + "." + currentToken.value();

        expectPeek(LPAREN);
        parseParameterList();
        expectPeek(RPAREN);
        parseSubroutineBody(functionName, subroutineType);

        printNonTerminal("/subroutineDec");
    }
```

  - **parseParameterList()**
```java
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
```

  - **parseSubroutineBody()**
```java
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
```

  - **parseSubroutineCall()**
```java

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
```

---

  - **parseStatements()**
```java
void parseStatements() {
    printNonTerminal("statements");
    while (peekToken.type == WHILE ||
            peekToken.type == IF ||
            peekToken.type == LET ||
            peekToken.type == DO ||
            peekToken.type == RETURN) {
        parseStatement();
    }

    printNonTerminal("/statements");
}
```

  - **parseStatement()**
```java
void parseStatement() {
    switch (peekToken.type) {
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
            throw error(peekToken, "Expected a statement");
    }
}
```

  - **parseExpressionList()**
```java
int parseExpressionList() {
    printNonTerminal("expressionList");
    var nArgs = 0; // -> usado pela vmWriter
    if (!peekTokenIs(RPAREN)) // verifica se tem pelo menos uma expressao
    {
        parseExpression();
        nArgs = 1;
    }
    // procurando as demais
    while (peekTokenIs(COMMA)) {
        expectPeek(COMMA);
        parseExpression();
        nArgs++;
    }

    printNonTerminal("/expressionList");
    return nArgs;
}
```

  - **parseExpression()**
```java

// expression -> term (op term)*

void parseExpression() {
    printNonTerminal("expression");
    parseTerm();
    while (isOperator(peekToken.type)) {
        var op = peekToken.type;
        expectPeek(peekToken.type);
        parseTerm();
    }
    printNonTerminal("/expression");
}
```

  - **parseTerm()** (1st)
```java

// term -> number | identifier | stringConstant | keywordConstant

void parseTerm() {
    printNonTerminal("term");
    switch (peekToken.type) {
        case INTEGER:
            expectPeek(INTEGER);
            break;
        case STRING:
            expectPeek(STRING);
            break;
        case FALSE:
        case NULL:
        case TRUE:
            expectPeek(FALSE, NULL, TRUE);
            break;
        case THIS:
            expectPeek(THIS);
            break;
        case IDENTIFIER:
            expectPeek(IDENTIFIER);

            if (peekTokenIs(LPAREN) || peekTokenIs(DOT)) {
                parseSubroutineCall();
            } else { // variavel comum ou array
                if (peekTokenIs(LBRACKET)) { // array
                    expectPeek(LBRACKET);
                    parseExpression();
                    expectPeek(RBRACKET);
                }
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
            var op = currentToken.type;
            parseTerm();
            break;
        default:
            throw error(peekToken, "term expected");
    }
    printNonTerminal("/term");
}
```


  - **todo()**
```java
//todo!
```

#### Funções de utilidade:

  - **XMLOutput()**
```java
public String XMLOutput() {
    return xmlOutput.toString();
}
```

  - **printNonTerminal()**
```java
private void printNonTerminal(String nterminal) {
    xmlOutput.append(String.format("<%s>\r\n", nterminal));
}
```

  - **peekTokenIs()**
```java
boolean peekTokenIs(TokenType type) {
    return peekToken.type == type;
}
```

  - **currentTokenIs()**
```java
boolean currentTokenIs(TokenType type) {
    return currentToken.type == type;
}
```

  - **expectPeek(TokenType... types)**
```java
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
```

  - **expectPeek(TokenType type)**
```java
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
```

  - **report()**
```java
private static void report(int line, String where, String message) {
    System.err.println(
            "[line " + line + "] Error" + where + ": " + message);
}
```

  - **error()**
```java
private ParseError error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.value() + "'", message);
        }
        return new ParseError();
    }
```
#### Novos atributos da classe Parser:

```java
private static class ParseError extends RuntimeException {
}

private StringBuilder xmlOutput = new StringBuilder();
```

### Todo:
- [x] Parser updates
- [x] Token updates
- [x] Utility Functions
    - [x] report()
    - [x] error()
    - [x] expectPeek()
    - [x] nextToken()
    - [x] peekTokenIs()
    - [x] currentTokenIs()
    - [x] printNonTerminal()
    - [x] XMLOutput()

- [x] Expression Parsing
    - [x] parseExpression()
    - [x] parseTerm()
    - [x] parseSubroutineCall()
    - [x] parseExpressionList()

- [ ] Statement Parsing
    - [x] parseStatement()
    - [x] parseStatements()
    - [x] parseLet()
    - [ ] parseWhile()
    - [ ] parseIf()
    - [ ] parseReturn()
    - [ ] parseDo()

- [ ] Parsing Core Structures
    - [ ] parse()
    - [ ] parseClass()
    - [ ] parseClassVarDec()
    - [ ] parseSubroutineDec()
    - [ ] parseParameterList()
    - [ ] parseVarDec()
