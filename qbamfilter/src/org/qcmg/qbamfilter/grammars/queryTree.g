tree grammar queryTree;

options{
    output=AST;
    ASTLabelType = CommonTree;
    tokenVocab = queryParser;
}

@header{ 
    package org.qcmg.qbamfilter.grammars;
    import htsjdk.samtools.SAMRecord;
    import htsjdk.samtools.filter.SamRecordFilter;
    import htsjdk.samtools.SAMValidationError;
    
}

@members{
    public static enum Operator{ OR, AND,NULL;  }
    private void out(String in){System.out.println( in);}
    
    public int no_exp = 0;
    public int no_con = 0;
}

script 
returns [SamRecordFilter  exp]
scope {Expression expression;}
@init{$script::expression = new Expression();}
:condition{
  //     System.out.println("return only condition as final output");
       $exp = $condition.con;  //return condition filter
      
      }
| atom[$script::expression]{
  //      System.out.println("return script expression as final output");
        $exp = $script::expression;  //return node added expression
        };

atom[Expression headNode]
scope {Expression expression;}
@init{$atom::expression = new Expression();}
:^(program[$atom::expression] expression[$atom::expression]   ( expression[$atom::expression] )+) {
 //       System.out.println("atom add program, expressions into  headnode");
       $headNode.addCondition($atom::expression);  //add expression node to tree   
      };

expression [Expression headNode]
  :  condition{ 
    //  System.out.println("add condition " + no_con);
      $headNode.addCondition($condition.con);  
}
| atom[$headNode]{ 
   // System.out.println("add atom " + no_exp++);
};


program[Expression headNode]
: OR {
     $headNode.addOperator(Operator.OR);  
     // System.out.println("add OR");
 }
| AND {
     $headNode.addOperator(Operator.AND);   
     //System.out.println("add AND"); 
      }
;


condition
returns [ SamRecordFilter con]
:^(CONDITION id=TAG comp=COMPARE  v=value ){
    try{   
        no_con ++;     
        Condition condition = new Condition( $id.text, $comp.text, $v.result ); 
       $con = condition.getFilter( );       
     //  System.out.println($id.text  + ","  + $comp.text + "," + $v.result); 
    }catch(Exception e){
        System.err.println(e.getMessage() );
        throw new RecognitionException();
    }
};

value returns [String result]
	: t=TAG{ $result = $t.text;}
	| v=VALUE{$result = $v.text;};
