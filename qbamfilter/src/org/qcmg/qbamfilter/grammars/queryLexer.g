lexer grammar queryLexer;


//lexer grammar
@header{
	package org.qcmg.qbamfilter.grammars;
}

@members {
  @Override
  public void reportError(RecognitionException e) {
    throw new RuntimeException( e.getMessage()); 
  }
}


LEFT_PAREN :	'(' ;
RIGHT_PAREN :	 ')' ;

COMPARE :	'>' | '<' | '==' | '>=' | '<=' | '!=';
OR : ('o' | 'O') ( 'r' | 'R');
AND : ('a' | 'A')('n' | 'N')('d' | 'D');
TAG	:  ( LETTER )( INT | LETTER | '_'| '.' )* ;
VALUE	: ( INT | LETTER | '_'| '.' | '*')* ;
COMMA	: 	',';


fragment INT    :	'0'..'9';
fragment LETTER	:	'a'..'z' | 'A'..'Z';

// $>
COMMENT
    :   '//' ~('\n'|'\r')* '\r'? '\n' {skip();}
    |  '#' ~('\n'|'\r')* '\r'? '\n' {skip();}
    |  '/*' ( options {greedy=false;} : . )* '*/' {$channel=HIDDEN;}
    ;
WS  :   ( ' ' | '\t'| '\r'| '\n') {$channel=HIDDEN;};