// $ANTLR 3.4 /Users/q.xu/Documents/MyWork/EclipseProject/SourceForge/adamajava/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryParser.g 2014-05-14 11:57:55

    package org.qcmg.qbamfilter.grammars;
    import java.util.HashMap;


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

import org.antlr.runtime.tree.*;


@SuppressWarnings({"all", "warnings", "unchecked"})
public class queryParser extends Parser {
    public static final String[] tokenNames = new String[] {
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "AND", "COMMA", "COMMENT", "COMPARE", "INT", "LEFT_PAREN", "LETTER", "OR", "RIGHT_PAREN", "TAG", "VALUE", "WS", "CONDITION"
    };

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
    public static final int CONDITION=16;

    // delegates
    public Parser[] getDelegates() {
        return new Parser[] {};
    }

    // delegators


    public queryParser(TokenStream input) {
        this(input, new RecognizerSharedState());
    }
    public queryParser(TokenStream input, RecognizerSharedState state) {
        super(input, state);
    }

protected TreeAdaptor adaptor = new CommonTreeAdaptor();

public void setTreeAdaptor(TreeAdaptor adaptor) {
    this.adaptor = adaptor;
}
public TreeAdaptor getTreeAdaptor() {
    return adaptor;
}
    public String[] getTokenNames() { return queryParser.tokenNames; }
    public String getGrammarFileName() { return "/Users/q.xu/Documents/MyWork/EclipseProject/SourceForge/adamajava/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryParser.g"; }


      @Override
      public void reportError(RecognitionException e) {
        throw new RuntimeException( "Exception during query parsing. You may forget to put double quotation on your query string." );
        }


    public static class script_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "script"
    // /Users/q.xu/Documents/MyWork/EclipseProject/SourceForge/adamajava/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryParser.g:23:1: script : ( condition | atom );
    public final queryParser.script_return script() throws RecognitionException {
        queryParser.script_return retval = new queryParser.script_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        queryParser.condition_return condition1 =null;

        queryParser.atom_return atom2 =null;



        try {
            // /Users/q.xu/Documents/MyWork/EclipseProject/SourceForge/adamajava/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryParser.g:23:7: ( condition | atom )
            int alt1=2;
            int LA1_0 = input.LA(1);

            if ( (LA1_0==TAG) ) {
                alt1=1;
            }
            else if ( (LA1_0==AND||LA1_0==OR) ) {
                alt1=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 1, 0, input);

                throw nvae;

            }
            switch (alt1) {
                case 1 :
                    // /Users/q.xu/Documents/MyWork/EclipseProject/SourceForge/adamajava/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryParser.g:23:9: condition
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_condition_in_script52);
                    condition1=condition();

                    state._fsp--;

                    adaptor.addChild(root_0, condition1.getTree());

                    }
                    break;
                case 2 :
                    // /Users/q.xu/Documents/MyWork/EclipseProject/SourceForge/adamajava/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryParser.g:23:22: atom
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_atom_in_script57);
                    atom2=atom();

                    state._fsp--;

                    adaptor.addChild(root_0, atom2.getTree());

                    }
                    break;

            }
            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "script"


    public static class atom_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "atom"
    // /Users/q.xu/Documents/MyWork/EclipseProject/SourceForge/adamajava/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryParser.g:26:1: atom : program ^ LEFT_PAREN ! expression ( COMMA ! expression )+ RIGHT_PAREN !;
    public final queryParser.atom_return atom() throws RecognitionException {
        queryParser.atom_return retval = new queryParser.atom_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token LEFT_PAREN4=null;
        Token COMMA6=null;
        Token RIGHT_PAREN8=null;
        queryParser.program_return program3 =null;

        queryParser.expression_return expression5 =null;

        queryParser.expression_return expression7 =null;


        Object LEFT_PAREN4_tree=null;
        Object COMMA6_tree=null;
        Object RIGHT_PAREN8_tree=null;

        try {
            // /Users/q.xu/Documents/MyWork/EclipseProject/SourceForge/adamajava/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryParser.g:26:5: ( program ^ LEFT_PAREN ! expression ( COMMA ! expression )+ RIGHT_PAREN !)
            // /Users/q.xu/Documents/MyWork/EclipseProject/SourceForge/adamajava/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryParser.g:27:5: program ^ LEFT_PAREN ! expression ( COMMA ! expression )+ RIGHT_PAREN !
            {
            root_0 = (Object)adaptor.nil();


            pushFollow(FOLLOW_program_in_atom70);
            program3=program();

            state._fsp--;

            root_0 = (Object)adaptor.becomeRoot(program3.getTree(), root_0);

            LEFT_PAREN4=(Token)match(input,LEFT_PAREN,FOLLOW_LEFT_PAREN_in_atom74); 

            pushFollow(FOLLOW_expression_in_atom77);
            expression5=expression();

            state._fsp--;

            adaptor.addChild(root_0, expression5.getTree());

            // /Users/q.xu/Documents/MyWork/EclipseProject/SourceForge/adamajava/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryParser.g:27:38: ( COMMA ! expression )+
            int cnt2=0;
            loop2:
            do {
                int alt2=2;
                int LA2_0 = input.LA(1);

                if ( (LA2_0==COMMA) ) {
                    alt2=1;
                }


                switch (alt2) {
            	case 1 :
            	    // /Users/q.xu/Documents/MyWork/EclipseProject/SourceForge/adamajava/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryParser.g:27:39: COMMA ! expression
            	    {
            	    COMMA6=(Token)match(input,COMMA,FOLLOW_COMMA_in_atom80); 

            	    pushFollow(FOLLOW_expression_in_atom83);
            	    expression7=expression();

            	    state._fsp--;

            	    adaptor.addChild(root_0, expression7.getTree());

            	    }
            	    break;

            	default :
            	    if ( cnt2 >= 1 ) break loop2;
                        EarlyExitException eee =
                            new EarlyExitException(2, input);
                        throw eee;
                }
                cnt2++;
            } while (true);


            RIGHT_PAREN8=(Token)match(input,RIGHT_PAREN,FOLLOW_RIGHT_PAREN_in_atom88); 

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "atom"


    public static class expression_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "expression"
    // /Users/q.xu/Documents/MyWork/EclipseProject/SourceForge/adamajava/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryParser.g:29:1: expression : ( condition | atom );
    public final queryParser.expression_return expression() throws RecognitionException {
        queryParser.expression_return retval = new queryParser.expression_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        queryParser.condition_return condition9 =null;

        queryParser.atom_return atom10 =null;



        try {
            // /Users/q.xu/Documents/MyWork/EclipseProject/SourceForge/adamajava/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryParser.g:30:5: ( condition | atom )
            int alt3=2;
            int LA3_0 = input.LA(1);

            if ( (LA3_0==TAG) ) {
                alt3=1;
            }
            else if ( (LA3_0==AND||LA3_0==OR) ) {
                alt3=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 3, 0, input);

                throw nvae;

            }
            switch (alt3) {
                case 1 :
                    // /Users/q.xu/Documents/MyWork/EclipseProject/SourceForge/adamajava/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryParser.g:30:8: condition
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_condition_in_expression102);
                    condition9=condition();

                    state._fsp--;

                    adaptor.addChild(root_0, condition9.getTree());

                    }
                    break;
                case 2 :
                    // /Users/q.xu/Documents/MyWork/EclipseProject/SourceForge/adamajava/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryParser.g:30:22: atom
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_atom_in_expression108);
                    atom10=atom();

                    state._fsp--;

                    adaptor.addChild(root_0, atom10.getTree());

                    }
                    break;

            }
            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "expression"


    public static class program_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "program"
    // /Users/q.xu/Documents/MyWork/EclipseProject/SourceForge/adamajava/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryParser.g:32:1: program : ( OR | AND );
    public final queryParser.program_return program() throws RecognitionException {
        queryParser.program_return retval = new queryParser.program_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token set11=null;

        Object set11_tree=null;

        try {
            // /Users/q.xu/Documents/MyWork/EclipseProject/SourceForge/adamajava/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryParser.g:32:8: ( OR | AND )
            // /Users/q.xu/Documents/MyWork/EclipseProject/SourceForge/adamajava/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryParser.g:
            {
            root_0 = (Object)adaptor.nil();


            set11=(Token)input.LT(1);

            if ( input.LA(1)==AND||input.LA(1)==OR ) {
                input.consume();
                adaptor.addChild(root_0, 
                (Object)adaptor.create(set11)
                );
                state.errorRecovery=false;
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                throw mse;
            }


            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "program"


    public static class condition_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "condition"
    // /Users/q.xu/Documents/MyWork/EclipseProject/SourceForge/adamajava/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryParser.g:34:1: condition : TAG COMPARE value -> ^( CONDITION TAG COMPARE value ) ;
    public final queryParser.condition_return condition() throws RecognitionException {
        queryParser.condition_return retval = new queryParser.condition_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token TAG12=null;
        Token COMPARE13=null;
        queryParser.value_return value14 =null;


        Object TAG12_tree=null;
        Object COMPARE13_tree=null;
        RewriteRuleTokenStream stream_COMPARE=new RewriteRuleTokenStream(adaptor,"token COMPARE");
        RewriteRuleTokenStream stream_TAG=new RewriteRuleTokenStream(adaptor,"token TAG");
        RewriteRuleSubtreeStream stream_value=new RewriteRuleSubtreeStream(adaptor,"rule value");
        try {
            // /Users/q.xu/Documents/MyWork/EclipseProject/SourceForge/adamajava/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryParser.g:35:5: ( TAG COMPARE value -> ^( CONDITION TAG COMPARE value ) )
            // /Users/q.xu/Documents/MyWork/EclipseProject/SourceForge/adamajava/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryParser.g:35:7: TAG COMPARE value
            {
            TAG12=(Token)match(input,TAG,FOLLOW_TAG_in_condition132);  
            stream_TAG.add(TAG12);


            COMPARE13=(Token)match(input,COMPARE,FOLLOW_COMPARE_in_condition134);  
            stream_COMPARE.add(COMPARE13);


            pushFollow(FOLLOW_value_in_condition136);
            value14=value();

            state._fsp--;

            stream_value.add(value14.getTree());

            // AST REWRITE
            // elements: TAG, COMPARE, value
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 36:5: -> ^( CONDITION TAG COMPARE value )
            {
                // /Users/q.xu/Documents/MyWork/EclipseProject/SourceForge/adamajava/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryParser.g:36:8: ^( CONDITION TAG COMPARE value )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(CONDITION, "CONDITION")
                , root_1);

                adaptor.addChild(root_1, 
                stream_TAG.nextNode()
                );

                adaptor.addChild(root_1, 
                stream_COMPARE.nextNode()
                );

                adaptor.addChild(root_1, stream_value.nextTree());

                adaptor.addChild(root_0, root_1);
                }

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "condition"


    public static class value_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "value"
    // /Users/q.xu/Documents/MyWork/EclipseProject/SourceForge/adamajava/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryParser.g:38:2: value : ( TAG | VALUE );
    public final queryParser.value_return value() throws RecognitionException {
        queryParser.value_return retval = new queryParser.value_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token set15=null;

        Object set15_tree=null;

        try {
            // /Users/q.xu/Documents/MyWork/EclipseProject/SourceForge/adamajava/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryParser.g:38:7: ( TAG | VALUE )
            // /Users/q.xu/Documents/MyWork/EclipseProject/SourceForge/adamajava/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryParser.g:
            {
            root_0 = (Object)adaptor.nil();


            set15=(Token)input.LT(1);

            if ( (input.LA(1) >= TAG && input.LA(1) <= VALUE) ) {
                input.consume();
                adaptor.addChild(root_0, 
                (Object)adaptor.create(set15)
                );
                state.errorRecovery=false;
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                throw mse;
            }


            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "value"

    // Delegated rules


 

    public static final BitSet FOLLOW_condition_in_script52 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_atom_in_script57 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_program_in_atom70 = new BitSet(new long[]{0x0000000000000200L});
    public static final BitSet FOLLOW_LEFT_PAREN_in_atom74 = new BitSet(new long[]{0x0000000000002810L});
    public static final BitSet FOLLOW_expression_in_atom77 = new BitSet(new long[]{0x0000000000000020L});
    public static final BitSet FOLLOW_COMMA_in_atom80 = new BitSet(new long[]{0x0000000000002810L});
    public static final BitSet FOLLOW_expression_in_atom83 = new BitSet(new long[]{0x0000000000001020L});
    public static final BitSet FOLLOW_RIGHT_PAREN_in_atom88 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_condition_in_expression102 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_atom_in_expression108 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_TAG_in_condition132 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_COMPARE_in_condition134 = new BitSet(new long[]{0x0000000000006000L});
    public static final BitSet FOLLOW_value_in_condition136 = new BitSet(new long[]{0x0000000000000002L});

}