
parser grammar queryParser;

options { 
	output = AST;
	tokenVocab = queryLexer;
}

tokens {CONDITION;}

@header {
    package org.qcmg.qbamfilter.grammars;
    import java.util.HashMap;
}

@members {
  @Override
  public void reportError(RecognitionException e) {
    throw new RuntimeException( "Exception during query parsing. You may forget to put double quotation on your query string." );
    }
}

script: condition  | atom; 


atom:
    program^  LEFT_PAREN! expression (COMMA! expression )+ RIGHT_PAREN!;

expression
    :  condition  |  atom;

program: OR | AND;

condition 
    : TAG COMPARE value
    -> ^(CONDITION TAG COMPARE value);
    
 value: TAG  |  VALUE;
    
