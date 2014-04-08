/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
// $ANTLR 3.4 /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryTree.g 2013-10-18 16:08:53
 
    package org.qcmg.qbamfilter.grammars;
    import net.sf.samtools.SAMRecord;
    import net.sf.picard.filter.SamRecordFilter;
    import net.sf.samtools.SAMValidationError;
    


import org.antlr.runtime.*;
import org.antlr.runtime.tree.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;


@SuppressWarnings({"all", "warnings", "unchecked"})
public class queryTree extends TreeParser {
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
    public TreeParser[] getDelegates() {
        return new TreeParser[] {};
    }

    // delegators


    public queryTree(TreeNodeStream input) {
        this(input, new RecognizerSharedState());
    }
    public queryTree(TreeNodeStream input, RecognizerSharedState state) {
        super(input, state);
    }

protected TreeAdaptor adaptor = new CommonTreeAdaptor();

public void setTreeAdaptor(TreeAdaptor adaptor) {
    this.adaptor = adaptor;
}
public TreeAdaptor getTreeAdaptor() {
    return adaptor;
}
    public String[] getTokenNames() { return queryTree.tokenNames; }
    public String getGrammarFileName() { return "/Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryTree.g"; }


        public static enum Operator{ OR, AND,NULL;  }
        private void out(String in){System.out.println( in);}
        
        public int no_exp = 0;
        public int no_con = 0;


    protected static class script_scope {
        Expression expression;
    }
    protected Stack script_stack = new Stack();


    public static class script_return extends TreeRuleReturnScope {
        public SamRecordFilter  exp;
        CommonTree tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "script"
    // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryTree.g:25:1: script returns [SamRecordFilter exp] : ( condition | atom[$script::expression] );
    public final queryTree.script_return script() throws RecognitionException {
        script_stack.push(new script_scope());
        queryTree.script_return retval = new queryTree.script_return();
        retval.start = input.LT(1);


        CommonTree root_0 = null;

        CommonTree _first_0 = null;
        CommonTree _last = null;

        queryTree.condition_return condition1 =null;

        queryTree.atom_return atom2 =null;



        ((script_scope)script_stack.peek()).expression = new Expression();
        try {
            // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryTree.g:29:2: ( condition | atom[$script::expression] )
            int alt1=2;
            int LA1_0 = input.LA(1);

            if ( (LA1_0==CONDITION) ) {
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
                    // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryTree.g:29:2: condition
                    {
                    root_0 = (CommonTree)adaptor.nil();


                    _last = (CommonTree)input.LT(1);
                    pushFollow(FOLLOW_condition_in_script70);
                    condition1=condition();

                    state._fsp--;

                    adaptor.addChild(root_0, condition1.getTree());



                      //     System.out.println("return only condition as final output");
                           retval.exp = (condition1!=null?condition1.con:null);  //return condition filter
                          
                          

                    }
                    break;
                case 2 :
                    // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryTree.g:34:3: atom[$script::expression]
                    {
                    root_0 = (CommonTree)adaptor.nil();


                    _last = (CommonTree)input.LT(1);
                    pushFollow(FOLLOW_atom_in_script75);
                    atom2=atom(((script_scope)script_stack.peek()).expression);

                    state._fsp--;

                    adaptor.addChild(root_0, atom2.getTree());



                      //      System.out.println("return script expression as final output");
                            retval.exp = ((script_scope)script_stack.peek()).expression;  //return node added expression
                            

                    }
                    break;

            }
            retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }

        finally {
        	// do for sure before leaving
            script_stack.pop();
        }
        return retval;
    }
    // $ANTLR end "script"


    protected static class atom_scope {
        Expression expression;
    }
    protected Stack atom_stack = new Stack();


    public static class atom_return extends TreeRuleReturnScope {
        CommonTree tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "atom"
    // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryTree.g:39:1: atom[Expression headNode] : ^( program[$atom::expression] expression[$atom::expression] ( expression[$atom::expression] )+ ) ;
    public final queryTree.atom_return atom(Expression headNode) throws RecognitionException {
        atom_stack.push(new atom_scope());
        queryTree.atom_return retval = new queryTree.atom_return();
        retval.start = input.LT(1);


        CommonTree root_0 = null;

        CommonTree _first_0 = null;
        CommonTree _last = null;

        queryTree.program_return program3 =null;

        queryTree.expression_return expression4 =null;

        queryTree.expression_return expression5 =null;



        ((atom_scope)atom_stack.peek()).expression = new Expression();
        try {
            // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryTree.g:42:2: ( ^( program[$atom::expression] expression[$atom::expression] ( expression[$atom::expression] )+ ) )
            // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryTree.g:42:2: ^( program[$atom::expression] expression[$atom::expression] ( expression[$atom::expression] )+ )
            {
            root_0 = (CommonTree)adaptor.nil();


            _last = (CommonTree)input.LT(1);
            {
            CommonTree _save_last_1 = _last;
            CommonTree _first_1 = null;
            CommonTree root_1 = (CommonTree)adaptor.nil();
            _last = (CommonTree)input.LT(1);
            pushFollow(FOLLOW_program_in_atom94);
            program3=program(((atom_scope)atom_stack.peek()).expression);

            state._fsp--;

            root_1 = (CommonTree)adaptor.becomeRoot(program3.getTree(), root_1);


            match(input, Token.DOWN, null); 
            _last = (CommonTree)input.LT(1);
            pushFollow(FOLLOW_expression_in_atom97);
            expression4=expression(((atom_scope)atom_stack.peek()).expression);

            state._fsp--;

            adaptor.addChild(root_1, expression4.getTree());


            // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryTree.g:42:63: ( expression[$atom::expression] )+
            int cnt2=0;
            loop2:
            do {
                int alt2=2;
                int LA2_0 = input.LA(1);

                if ( (LA2_0==AND||LA2_0==OR||LA2_0==CONDITION) ) {
                    alt2=1;
                }


                switch (alt2) {
            	case 1 :
            	    // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryTree.g:42:65: expression[$atom::expression]
            	    {
            	    _last = (CommonTree)input.LT(1);
            	    pushFollow(FOLLOW_expression_in_atom104);
            	    expression5=expression(((atom_scope)atom_stack.peek()).expression);

            	    state._fsp--;

            	    adaptor.addChild(root_1, expression5.getTree());


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


            match(input, Token.UP, null); 
            adaptor.addChild(root_0, root_1);
            _last = _save_last_1;
            }



             //       System.out.println("atom add program, expressions into  headnode");
                   headNode.addCondition(((atom_scope)atom_stack.peek()).expression);  //add expression node to tree   
                  

            }

            retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }

        finally {
        	// do for sure before leaving
            atom_stack.pop();
        }
        return retval;
    }
    // $ANTLR end "atom"


    public static class expression_return extends TreeRuleReturnScope {
        CommonTree tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "expression"
    // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryTree.g:47:1: expression[Expression headNode] : ( condition | atom[$headNode] );
    public final queryTree.expression_return expression(Expression headNode) throws RecognitionException {
        queryTree.expression_return retval = new queryTree.expression_return();
        retval.start = input.LT(1);


        CommonTree root_0 = null;

        CommonTree _first_0 = null;
        CommonTree _last = null;

        queryTree.condition_return condition6 =null;

        queryTree.atom_return atom7 =null;



        try {
            // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryTree.g:48:3: ( condition | atom[$headNode] )
            int alt3=2;
            int LA3_0 = input.LA(1);

            if ( (LA3_0==CONDITION) ) {
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
                    // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryTree.g:48:6: condition
                    {
                    root_0 = (CommonTree)adaptor.nil();


                    _last = (CommonTree)input.LT(1);
                    pushFollow(FOLLOW_condition_in_expression124);
                    condition6=condition();

                    state._fsp--;

                    adaptor.addChild(root_0, condition6.getTree());


                     
                        //  System.out.println("add condition " + no_con);
                          headNode.addCondition((condition6!=null?condition6.con:null));  


                    }
                    break;
                case 2 :
                    // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryTree.g:52:3: atom[$headNode]
                    {
                    root_0 = (CommonTree)adaptor.nil();


                    _last = (CommonTree)input.LT(1);
                    pushFollow(FOLLOW_atom_in_expression129);
                    atom7=atom(headNode);

                    state._fsp--;

                    adaptor.addChild(root_0, atom7.getTree());


                     
                       // System.out.println("add atom " + no_exp++);


                    }
                    break;

            }
            retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "expression"


    public static class program_return extends TreeRuleReturnScope {
        CommonTree tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "program"
    // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryTree.g:57:1: program[Expression headNode] : ( OR | AND );
    public final queryTree.program_return program(Expression headNode) throws RecognitionException {
        queryTree.program_return retval = new queryTree.program_return();
        retval.start = input.LT(1);


        CommonTree root_0 = null;

        CommonTree _first_0 = null;
        CommonTree _last = null;

        CommonTree OR8=null;
        CommonTree AND9=null;

        CommonTree OR8_tree=null;
        CommonTree AND9_tree=null;

        try {
            // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryTree.g:58:3: ( OR | AND )
            int alt4=2;
            int LA4_0 = input.LA(1);

            if ( (LA4_0==OR) ) {
                alt4=1;
            }
            else if ( (LA4_0==AND) ) {
                alt4=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 4, 0, input);

                throw nvae;

            }
            switch (alt4) {
                case 1 :
                    // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryTree.g:58:3: OR
                    {
                    root_0 = (CommonTree)adaptor.nil();


                    _last = (CommonTree)input.LT(1);
                    OR8=(CommonTree)match(input,OR,FOLLOW_OR_in_program141); 
                    OR8_tree = (CommonTree)adaptor.dupNode(OR8);


                    adaptor.addChild(root_0, OR8_tree);



                         headNode.addOperator(Operator.OR);  
                         // System.out.println("add OR");
                     

                    }
                    break;
                case 2 :
                    // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryTree.g:62:3: AND
                    {
                    root_0 = (CommonTree)adaptor.nil();


                    _last = (CommonTree)input.LT(1);
                    AND9=(CommonTree)match(input,AND,FOLLOW_AND_in_program147); 
                    AND9_tree = (CommonTree)adaptor.dupNode(AND9);


                    adaptor.addChild(root_0, AND9_tree);



                         headNode.addOperator(Operator.AND);   
                         //System.out.println("add AND"); 
                          

                    }
                    break;

            }
            retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "program"


    public static class condition_return extends TreeRuleReturnScope {
        public SamRecordFilter con;
        CommonTree tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "condition"
    // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryTree.g:69:1: condition returns [ SamRecordFilter con] : ^( CONDITION id= TAG comp= COMPARE v= value ) ;
    public final queryTree.condition_return condition() throws RecognitionException {
        queryTree.condition_return retval = new queryTree.condition_return();
        retval.start = input.LT(1);


        CommonTree root_0 = null;

        CommonTree _first_0 = null;
        CommonTree _last = null;

        CommonTree id=null;
        CommonTree comp=null;
        CommonTree CONDITION10=null;
        queryTree.value_return v =null;


        CommonTree id_tree=null;
        CommonTree comp_tree=null;
        CommonTree CONDITION10_tree=null;

        try {
            // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryTree.g:71:2: ( ^( CONDITION id= TAG comp= COMPARE v= value ) )
            // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryTree.g:71:2: ^( CONDITION id= TAG comp= COMPARE v= value )
            {
            root_0 = (CommonTree)adaptor.nil();


            _last = (CommonTree)input.LT(1);
            {
            CommonTree _save_last_1 = _last;
            CommonTree _first_1 = null;
            CommonTree root_1 = (CommonTree)adaptor.nil();
            _last = (CommonTree)input.LT(1);
            CONDITION10=(CommonTree)match(input,CONDITION,FOLLOW_CONDITION_in_condition163); 
            CONDITION10_tree = (CommonTree)adaptor.dupNode(CONDITION10);


            root_1 = (CommonTree)adaptor.becomeRoot(CONDITION10_tree, root_1);


            match(input, Token.DOWN, null); 
            _last = (CommonTree)input.LT(1);
            id=(CommonTree)match(input,TAG,FOLLOW_TAG_in_condition167); 
            id_tree = (CommonTree)adaptor.dupNode(id);


            adaptor.addChild(root_1, id_tree);


            _last = (CommonTree)input.LT(1);
            comp=(CommonTree)match(input,COMPARE,FOLLOW_COMPARE_in_condition171); 
            comp_tree = (CommonTree)adaptor.dupNode(comp);


            adaptor.addChild(root_1, comp_tree);


            _last = (CommonTree)input.LT(1);
            pushFollow(FOLLOW_value_in_condition176);
            v=value();

            state._fsp--;

            adaptor.addChild(root_1, v.getTree());


            match(input, Token.UP, null); 
            adaptor.addChild(root_0, root_1);
            _last = _save_last_1;
            }



                try{   
                    no_con ++;     
                    Condition condition = new Condition( (id!=null?id.getText():null), (comp!=null?comp.getText():null), (v!=null?v.result:null) ); 
                   retval.con = condition.getFilter( );       
                 //  System.out.println((id!=null?id.getText():null)  + ","  + (comp!=null?comp.getText():null) + "," + (v!=null?v.result:null)); 
                }catch(Exception e){
                    System.err.println(e.getMessage() );
                    throw new RecognitionException();
                }


            }

            retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "condition"


    public static class value_return extends TreeRuleReturnScope {
        public String result;
        CommonTree tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "value"
    // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryTree.g:83:1: value returns [String result] : (t= TAG |v= VALUE );
    public final queryTree.value_return value() throws RecognitionException {
        queryTree.value_return retval = new queryTree.value_return();
        retval.start = input.LT(1);


        CommonTree root_0 = null;

        CommonTree _first_0 = null;
        CommonTree _last = null;

        CommonTree t=null;
        CommonTree v=null;

        CommonTree t_tree=null;
        CommonTree v_tree=null;

        try {
            // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryTree.g:84:2: (t= TAG |v= VALUE )
            int alt5=2;
            int LA5_0 = input.LA(1);

            if ( (LA5_0==TAG) ) {
                alt5=1;
            }
            else if ( (LA5_0==VALUE) ) {
                alt5=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 5, 0, input);

                throw nvae;

            }
            switch (alt5) {
                case 1 :
                    // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryTree.g:84:4: t= TAG
                    {
                    root_0 = (CommonTree)adaptor.nil();


                    _last = (CommonTree)input.LT(1);
                    t=(CommonTree)match(input,TAG,FOLLOW_TAG_in_value194); 
                    t_tree = (CommonTree)adaptor.dupNode(t);


                    adaptor.addChild(root_0, t_tree);


                     retval.result = (t!=null?t.getText():null);

                    }
                    break;
                case 2 :
                    // /Users/qbamfilter/src/org/qcmg/qbamfilter/grammars/queryTree.g:85:4: v= VALUE
                    {
                    root_0 = (CommonTree)adaptor.nil();


                    _last = (CommonTree)input.LT(1);
                    v=(CommonTree)match(input,VALUE,FOLLOW_VALUE_in_value202); 
                    v_tree = (CommonTree)adaptor.dupNode(v);


                    adaptor.addChild(root_0, v_tree);


                    retval.result = (v!=null?v.getText():null);

                    }
                    break;

            }
            retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "value"

    // Delegated rules


 

    public static final BitSet FOLLOW_condition_in_script70 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_atom_in_script75 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_program_in_atom94 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_expression_in_atom97 = new BitSet(new long[]{0x0000000000010810L});
    public static final BitSet FOLLOW_expression_in_atom104 = new BitSet(new long[]{0x0000000000010818L});
    public static final BitSet FOLLOW_condition_in_expression124 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_atom_in_expression129 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_OR_in_program141 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_AND_in_program147 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_CONDITION_in_condition163 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_TAG_in_condition167 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_COMPARE_in_condition171 = new BitSet(new long[]{0x0000000000006000L});
    public static final BitSet FOLLOW_value_in_condition176 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_TAG_in_value194 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_VALUE_in_value202 = new BitSet(new long[]{0x0000000000000002L});

}