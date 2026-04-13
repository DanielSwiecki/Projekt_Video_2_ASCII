grammar AsciiFlow;
/*
@header {
package grammar;
}
*/
program
    : statement* EOF
    ;

statement
    : varDecl
    | assignment SEMI
    | sourceStmt
    | sampleStmt
    | fpsStmt
    | setStmt
    | filterStmt
    | exportStmt
    | ifStmt
    | forStmt
    | block
    | playStmt
    ;

block
    : LBRACE statement* RBRACE
    ;

playStmt
    : 'play' ';'
    ;

varDecl
    : LET ID (ASSIGN expr)? SEMI
    ;

varDeclInline
    : LET ID (ASSIGN expr)?
    ;

assignment
    : ID ASSIGN expr
    ;

sourceStmt
    : SOURCE expr SEMI
    ;

sampleStmt
    : SAMPLE EVERY expr FRAMES SEMI
    ;

fpsStmt
    : FPS expr SEMI
    ;

setStmt
    : SET ID ASSIGN expr SEMI
    ;

filterStmt
    : FILTER ID (LPAREN argList? RPAREN)? SEMI
    ;

exportStmt
    : EXPORT (ASCII | JSON) TO expr SEMI
    ;

ifStmt
    : IF LPAREN expr RPAREN block (ELSE block)?
    ;

forStmt
    : FOR LPAREN forInit? SEMI expr? SEMI assignment? RPAREN block
    ;

forInit
    : varDeclInline
    | assignment
    ;

argList
    : expr (COMMA expr)*
    ;

expr
    : logicalOr
    ;

logicalOr
    : logicalAnd (OR logicalAnd)*
    ;

logicalAnd
    : equality (AND equality)*
    ;

equality
    : comparison ((EQ | NEQ) comparison)*
    ;

comparison
    : addition ((LT | GT | LE | GE) addition)*
    ;

addition
    : multiplication ((PLUS | MINUS) multiplication)*
    ;

multiplication
    : unary ((STAR | SLASH | PERCENT) unary)*
    ;

unary
    : (NOT | MINUS) unary
    | primary
    ;

primary
    : INT
    | FLOAT
    | STRING
    | TRUE
    | FALSE
    | ID
    | LPAREN expr RPAREN
    ;

LET      : 'let';
SOURCE   : 'source';
SAMPLE   : 'sample';
EVERY    : 'every';
FRAMES   : 'frames';
FPS      : 'fps';
SET      : 'set';
FILTER   : 'filter';
EXPORT   : 'export';
ASCII    : 'ascii';
JSON     : 'json';
TO       : 'to';
IF       : 'if';
ELSE     : 'else';
FOR      : 'for';
TRUE     : 'true';
FALSE    : 'false';
AND      : 'and';
OR       : 'or';
NOT      : 'not';

EQ       : '==';
NEQ      : '!=';
LE       : '<=';
GE       : '>=';
LT       : '<';
GT       : '>';
ASSIGN   : '=';
PLUS     : '+';
MINUS    : '-';
STAR     : '*';
SLASH    : '/';
PERCENT  : '%';
LPAREN   : '(';
RPAREN   : ')';
LBRACE   : '{';
RBRACE   : '}';
COMMA    : ',';
SEMI     : ';';

FLOAT    : [0-9]+ '.' [0-9]+;
INT      : [0-9]+;
STRING   : '"' (~["\\\r\n] | '\\' .)* '"';
ID       : [a-zA-Z_][a-zA-Z0-9_]*;

LINE_COMMENT
    : '//' ~[\r\n]* -> skip
    ;

BLOCK_COMMENT
    : '/*' .*? '*/' -> skip
    ;

WS
    : [ \t\r\n]+ -> skip
    ;
