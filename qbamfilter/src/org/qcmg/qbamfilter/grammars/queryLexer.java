/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
// $ANTLR 3.4 /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g 2013-10-18 15:56:38

	package org.qcmg.qbamfilter.grammars;


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked"})
public class queryLexer extends Lexer {
    public static final int EOF=-1;
    public static final int AND=4;
    public static final int COMMA=5;
    public static final int COMMENT=6;
    public static final int COMPARE=7;
    public static final int INT=8;
    public static final int LEFT_PAREN=9;
    public static final int LETTER=10;
    public static final int OR=11;
    public static final int RIGHT_PAREN=12;
    public static final int TAG=13;
    public static final int VALUE=14;
    public static final int WS=15;

      @Override
      public void reportError(RecognitionException e) {
        throw new RuntimeException( e.getMessage()); 
      }


    // delegates
    // delegators
    public Lexer[] getDelegates() {
        return new Lexer[] {};
    }

    public queryLexer() {} 
    public queryLexer(CharStream input) {
        this(input, new RecognizerSharedState());
    }
    public queryLexer(CharStream input, RecognizerSharedState state) {
        super(input,state);
    }
    public String getGrammarFileName() { return "/Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g"; }

    // $ANTLR start "LEFT_PAREN"
    public final void mLEFT_PAREN() throws RecognitionException {
        try {
            int _type = LEFT_PAREN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:17:12: ( '(' )
            // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:17:14: '('
            {
            match('('); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "LEFT_PAREN"

    // $ANTLR start "RIGHT_PAREN"
    public final void mRIGHT_PAREN() throws RecognitionException {
        try {
            int _type = RIGHT_PAREN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:18:13: ( ')' )
            // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:18:16: ')'
            {
            match(')'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "RIGHT_PAREN"

    // $ANTLR start "COMPARE"
    public final void mCOMPARE() throws RecognitionException {
        try {
            int _type = COMPARE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:20:9: ( '>' | '<' | '==' | '>=' | '<=' | '!=' )
            int alt1=6;
            switch ( input.LA(1) ) {
            case '>':
                {
                int LA1_1 = input.LA(2);

                if ( (LA1_1=='=') ) {
                    alt1=4;
                }
                else {
                    alt1=1;
                }
                }
                break;
            case '<':
                {
                int LA1_2 = input.LA(2);

                if ( (LA1_2=='=') ) {
                    alt1=5;
                }
                else {
                    alt1=2;
                }
                }
                break;
            case '=':
                {
                alt1=3;
                }
                break;
            case '!':
                {
                alt1=6;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 1, 0, input);

                throw nvae;

            }

            switch (alt1) {
                case 1 :
                    // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:20:11: '>'
                    {
                    match('>'); 

                    }
                    break;
                case 2 :
                    // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:20:17: '<'
                    {
                    match('<'); 

                    }
                    break;
                case 3 :
                    // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:20:23: '=='
                    {
                    match("=="); 



                    }
                    break;
                case 4 :
                    // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:20:30: '>='
                    {
                    match(">="); 



                    }
                    break;
                case 5 :
                    // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:20:37: '<='
                    {
                    match("<="); 



                    }
                    break;
                case 6 :
                    // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:20:44: '!='
                    {
                    match("!="); 



                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "COMPARE"

    // $ANTLR start "OR"
    public final void mOR() throws RecognitionException {
        try {
            int _type = OR;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:21:4: ( ( 'o' | 'O' ) ( 'r' | 'R' ) )
            // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:21:6: ( 'o' | 'O' ) ( 'r' | 'R' )
            {
            if ( input.LA(1)=='O'||input.LA(1)=='o' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            if ( input.LA(1)=='R'||input.LA(1)=='r' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "OR"

    // $ANTLR start "AND"
    public final void mAND() throws RecognitionException {
        try {
            int _type = AND;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:22:5: ( ( 'a' | 'A' ) ( 'n' | 'N' ) ( 'd' | 'D' ) )
            // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:22:7: ( 'a' | 'A' ) ( 'n' | 'N' ) ( 'd' | 'D' )
            {
            if ( input.LA(1)=='A'||input.LA(1)=='a' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            if ( input.LA(1)=='N'||input.LA(1)=='n' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            if ( input.LA(1)=='D'||input.LA(1)=='d' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "AND"

    // $ANTLR start "TAG"
    public final void mTAG() throws RecognitionException {
        try {
            int _type = TAG;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:23:5: ( ( LETTER ) ( INT | LETTER | '_' | '.' )* )
            // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:23:8: ( LETTER ) ( INT | LETTER | '_' | '.' )*
            {
            if ( (input.LA(1) >= 'A' && input.LA(1) <= 'Z')||(input.LA(1) >= 'a' && input.LA(1) <= 'z') ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:23:18: ( INT | LETTER | '_' | '.' )*
            loop2:
            do {
                int alt2=2;
                int LA2_0 = input.LA(1);

                if ( (LA2_0=='.'||(LA2_0 >= '0' && LA2_0 <= '9')||(LA2_0 >= 'A' && LA2_0 <= 'Z')||LA2_0=='_'||(LA2_0 >= 'a' && LA2_0 <= 'z')) ) {
                    alt2=1;
                }


                switch (alt2) {
            	case 1 :
            	    // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:
            	    {
            	    if ( input.LA(1)=='.'||(input.LA(1) >= '0' && input.LA(1) <= '9')||(input.LA(1) >= 'A' && input.LA(1) <= 'Z')||input.LA(1)=='_'||(input.LA(1) >= 'a' && input.LA(1) <= 'z') ) {
            	        input.consume();
            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    break loop2;
                }
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "TAG"

    // $ANTLR start "VALUE"
    public final void mVALUE() throws RecognitionException {
        try {
            int _type = VALUE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:24:7: ( ( INT | LETTER | '_' | '.' | '*' )* )
            // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:24:9: ( INT | LETTER | '_' | '.' | '*' )*
            {
            // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:24:9: ( INT | LETTER | '_' | '.' | '*' )*
            loop3:
            do {
                int alt3=2;
                int LA3_0 = input.LA(1);

                if ( (LA3_0=='*'||LA3_0=='.'||(LA3_0 >= '0' && LA3_0 <= '9')||(LA3_0 >= 'A' && LA3_0 <= 'Z')||LA3_0=='_'||(LA3_0 >= 'a' && LA3_0 <= 'z')) ) {
                    alt3=1;
                }


                switch (alt3) {
            	case 1 :
            	    // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:
            	    {
            	    if ( input.LA(1)=='*'||input.LA(1)=='.'||(input.LA(1) >= '0' && input.LA(1) <= '9')||(input.LA(1) >= 'A' && input.LA(1) <= 'Z')||input.LA(1)=='_'||(input.LA(1) >= 'a' && input.LA(1) <= 'z') ) {
            	        input.consume();
            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    break loop3;
                }
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "VALUE"

    // $ANTLR start "COMMA"
    public final void mCOMMA() throws RecognitionException {
        try {
            int _type = COMMA;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:25:7: ( ',' )
            // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:25:10: ','
            {
            match(','); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "COMMA"

    // $ANTLR start "INT"
    public final void mINT() throws RecognitionException {
        try {
            // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:28:17: ( '0' .. '9' )
            // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:
            {
            if ( (input.LA(1) >= '0' && input.LA(1) <= '9') ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            }


        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "INT"

    // $ANTLR start "LETTER"
    public final void mLETTER() throws RecognitionException {
        try {
            // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:29:17: ( 'a' .. 'z' | 'A' .. 'Z' )
            // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:
            {
            if ( (input.LA(1) >= 'A' && input.LA(1) <= 'Z')||(input.LA(1) >= 'a' && input.LA(1) <= 'z') ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            }


        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "LETTER"

    // $ANTLR start "COMMENT"
    public final void mCOMMENT() throws RecognitionException {
        try {
            int _type = COMMENT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:33:5: ( '//' (~ ( '\\n' | '\\r' ) )* ( '\\r' )? '\\n' | '#' (~ ( '\\n' | '\\r' ) )* ( '\\r' )? '\\n' | '/*' ( options {greedy=false; } : . )* '*/' )
            int alt9=3;
            int LA9_0 = input.LA(1);

            if ( (LA9_0=='/') ) {
                int LA9_1 = input.LA(2);

                if ( (LA9_1=='/') ) {
                    alt9=1;
                }
                else if ( (LA9_1=='*') ) {
                    alt9=3;
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("", 9, 1, input);

                    throw nvae;

                }
            }
            else if ( (LA9_0=='#') ) {
                alt9=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 9, 0, input);

                throw nvae;

            }
            switch (alt9) {
                case 1 :
                    // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:33:9: '//' (~ ( '\\n' | '\\r' ) )* ( '\\r' )? '\\n'
                    {
                    match("//"); 



                    // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:33:14: (~ ( '\\n' | '\\r' ) )*
                    loop4:
                    do {
                        int alt4=2;
                        int LA4_0 = input.LA(1);

                        if ( ((LA4_0 >= '\u0000' && LA4_0 <= '\t')||(LA4_0 >= '\u000B' && LA4_0 <= '\f')||(LA4_0 >= '\u000E' && LA4_0 <= '\uFFFF')) ) {
                            alt4=1;
                        }


                        switch (alt4) {
                    	case 1 :
                    	    // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:
                    	    {
                    	    if ( (input.LA(1) >= '\u0000' && input.LA(1) <= '\t')||(input.LA(1) >= '\u000B' && input.LA(1) <= '\f')||(input.LA(1) >= '\u000E' && input.LA(1) <= '\uFFFF') ) {
                    	        input.consume();
                    	    }
                    	    else {
                    	        MismatchedSetException mse = new MismatchedSetException(null,input);
                    	        recover(mse);
                    	        throw mse;
                    	    }


                    	    }
                    	    break;

                    	default :
                    	    break loop4;
                        }
                    } while (true);


                    // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:33:28: ( '\\r' )?
                    int alt5=2;
                    int LA5_0 = input.LA(1);

                    if ( (LA5_0=='\r') ) {
                        alt5=1;
                    }
                    switch (alt5) {
                        case 1 :
                            // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:33:28: '\\r'
                            {
                            match('\r'); 

                            }
                            break;

                    }


                    match('\n'); 

                    skip();

                    }
                    break;
                case 2 :
                    // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:34:8: '#' (~ ( '\\n' | '\\r' ) )* ( '\\r' )? '\\n'
                    {
                    match('#'); 

                    // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:34:12: (~ ( '\\n' | '\\r' ) )*
                    loop6:
                    do {
                        int alt6=2;
                        int LA6_0 = input.LA(1);

                        if ( ((LA6_0 >= '\u0000' && LA6_0 <= '\t')||(LA6_0 >= '\u000B' && LA6_0 <= '\f')||(LA6_0 >= '\u000E' && LA6_0 <= '\uFFFF')) ) {
                            alt6=1;
                        }


                        switch (alt6) {
                    	case 1 :
                    	    // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:
                    	    {
                    	    if ( (input.LA(1) >= '\u0000' && input.LA(1) <= '\t')||(input.LA(1) >= '\u000B' && input.LA(1) <= '\f')||(input.LA(1) >= '\u000E' && input.LA(1) <= '\uFFFF') ) {
                    	        input.consume();
                    	    }
                    	    else {
                    	        MismatchedSetException mse = new MismatchedSetException(null,input);
                    	        recover(mse);
                    	        throw mse;
                    	    }


                    	    }
                    	    break;

                    	default :
                    	    break loop6;
                        }
                    } while (true);


                    // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:34:26: ( '\\r' )?
                    int alt7=2;
                    int LA7_0 = input.LA(1);

                    if ( (LA7_0=='\r') ) {
                        alt7=1;
                    }
                    switch (alt7) {
                        case 1 :
                            // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:34:26: '\\r'
                            {
                            match('\r'); 

                            }
                            break;

                    }


                    match('\n'); 

                    skip();

                    }
                    break;
                case 3 :
                    // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:35:8: '/*' ( options {greedy=false; } : . )* '*/'
                    {
                    match("/*"); 



                    // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:35:13: ( options {greedy=false; } : . )*
                    loop8:
                    do {
                        int alt8=2;
                        int LA8_0 = input.LA(1);

                        if ( (LA8_0=='*') ) {
                            int LA8_1 = input.LA(2);

                            if ( (LA8_1=='/') ) {
                                alt8=2;
                            }
                            else if ( ((LA8_1 >= '\u0000' && LA8_1 <= '.')||(LA8_1 >= '0' && LA8_1 <= '\uFFFF')) ) {
                                alt8=1;
                            }


                        }
                        else if ( ((LA8_0 >= '\u0000' && LA8_0 <= ')')||(LA8_0 >= '+' && LA8_0 <= '\uFFFF')) ) {
                            alt8=1;
                        }


                        switch (alt8) {
                    	case 1 :
                    	    // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:35:41: .
                    	    {
                    	    matchAny(); 

                    	    }
                    	    break;

                    	default :
                    	    break loop8;
                        }
                    } while (true);


                    match("*/"); 



                    _channel=HIDDEN;

                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "COMMENT"

    // $ANTLR start "WS"
    public final void mWS() throws RecognitionException {
        try {
            int _type = WS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:37:5: ( ( ' ' | '\\t' | '\\r' | '\\n' ) )
            // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:37:9: ( ' ' | '\\t' | '\\r' | '\\n' )
            {
            if ( (input.LA(1) >= '\t' && input.LA(1) <= '\n')||input.LA(1)=='\r'||input.LA(1)==' ' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            _channel=HIDDEN;

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "WS"

    public void mTokens() throws RecognitionException {
        // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:1:8: ( LEFT_PAREN | RIGHT_PAREN | COMPARE | OR | AND | TAG | VALUE | COMMA | COMMENT | WS )
        int alt10=10;
        alt10 = dfa10.predict(input);
        switch (alt10) {
            case 1 :
                // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:1:10: LEFT_PAREN
                {
                mLEFT_PAREN(); 


                }
                break;
            case 2 :
                // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:1:21: RIGHT_PAREN
                {
                mRIGHT_PAREN(); 


                }
                break;
            case 3 :
                // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:1:33: COMPARE
                {
                mCOMPARE(); 


                }
                break;
            case 4 :
                // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:1:41: OR
                {
                mOR(); 


                }
                break;
            case 5 :
                // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:1:44: AND
                {
                mAND(); 


                }
                break;
            case 6 :
                // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:1:48: TAG
                {
                mTAG(); 


                }
                break;
            case 7 :
                // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:1:52: VALUE
                {
                mVALUE(); 


                }
                break;
            case 8 :
                // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:1:58: COMMA
                {
                mCOMMA(); 


                }
                break;
            case 9 :
                // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:1:64: COMMENT
                {
                mCOMMENT(); 


                }
                break;
            case 10 :
                // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryLexer.g:1:72: WS
                {
                mWS(); 


                }
                break;

        }

    }


    protected DFA10 dfa10 = new DFA10(this);
    static final String DFA10_eotS =
        "\1\7\3\uffff\3\15\4\uffff\1\17\1\15\1\uffff\1\15\1\uffff\1\21\1"+
        "\uffff";
    static final String DFA10_eofS =
        "\22\uffff";
    static final String DFA10_minS =
        "\1\11\3\uffff\3\52\4\uffff\2\52\1\uffff\1\52\1\uffff\1\52\1\uffff";
    static final String DFA10_maxS =
        "\1\172\3\uffff\3\172\4\uffff\2\172\1\uffff\1\172\1\uffff\1\172\1"+
        "\uffff";
    static final String DFA10_acceptS =
        "\1\uffff\1\1\1\2\1\3\3\uffff\1\7\1\10\1\11\1\12\2\uffff\1\6\1\uffff"+
        "\1\4\1\uffff\1\5";
    static final String DFA10_specialS =
        "\22\uffff}>";
    static final String[] DFA10_transitionS = {
            "\2\12\2\uffff\1\12\22\uffff\1\12\1\3\1\uffff\1\11\4\uffff\1"+
            "\1\1\2\2\uffff\1\10\2\uffff\1\11\14\uffff\3\3\2\uffff\1\5\15"+
            "\6\1\4\13\6\6\uffff\1\5\15\6\1\4\13\6",
            "",
            "",
            "",
            "\1\7\3\uffff\1\14\1\uffff\12\14\7\uffff\21\14\1\13\10\14\4"+
            "\uffff\1\14\1\uffff\21\14\1\13\10\14",
            "\1\7\3\uffff\1\14\1\uffff\12\14\7\uffff\15\14\1\16\14\14\4"+
            "\uffff\1\14\1\uffff\15\14\1\16\14\14",
            "\1\7\3\uffff\1\14\1\uffff\12\14\7\uffff\32\14\4\uffff\1\14"+
            "\1\uffff\32\14",
            "",
            "",
            "",
            "",
            "\1\7\3\uffff\1\14\1\uffff\12\14\7\uffff\32\14\4\uffff\1\14"+
            "\1\uffff\32\14",
            "\1\7\3\uffff\1\14\1\uffff\12\14\7\uffff\32\14\4\uffff\1\14"+
            "\1\uffff\32\14",
            "",
            "\1\7\3\uffff\1\14\1\uffff\12\14\7\uffff\3\14\1\20\26\14\4\uffff"+
            "\1\14\1\uffff\3\14\1\20\26\14",
            "",
            "\1\7\3\uffff\1\14\1\uffff\12\14\7\uffff\32\14\4\uffff\1\14"+
            "\1\uffff\32\14",
            ""
    };

    static final short[] DFA10_eot = DFA.unpackEncodedString(DFA10_eotS);
    static final short[] DFA10_eof = DFA.unpackEncodedString(DFA10_eofS);
    static final char[] DFA10_min = DFA.unpackEncodedStringToUnsignedChars(DFA10_minS);
    static final char[] DFA10_max = DFA.unpackEncodedStringToUnsignedChars(DFA10_maxS);
    static final short[] DFA10_accept = DFA.unpackEncodedString(DFA10_acceptS);
    static final short[] DFA10_special = DFA.unpackEncodedString(DFA10_specialS);
    static final short[][] DFA10_transition;

    static {
        int numStates = DFA10_transitionS.length;
        DFA10_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA10_transition[i] = DFA.unpackEncodedString(DFA10_transitionS[i]);
        }
    }

    class DFA10 extends DFA {

        public DFA10(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 10;
            this.eot = DFA10_eot;
            this.eof = DFA10_eof;
            this.min = DFA10_min;
            this.max = DFA10_max;
            this.accept = DFA10_accept;
            this.special = DFA10_special;
            this.transition = DFA10_transition;
        }
        public String getDescription() {
            return "1:1: Tokens : ( LEFT_PAREN | RIGHT_PAREN | COMPARE | OR | AND | TAG | VALUE | COMMA | COMMENT | WS );";
        }
    }
 

}