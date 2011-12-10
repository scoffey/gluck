%{
import java.util.*;
import ar.edu.itba.dcc.tp.util.*;
import ar.edu.itba.dcc.tp.run.Compiler;
import ar.edu.itba.dcc.tp.semantic.symbols.*;
import ar.edu.itba.dcc.tp.semantic.nodes.*;
import static ar.edu.itba.dcc.tp.semantic.Instruction.*;
import static ar.edu.itba.dcc.tp.semantic.CodeGenerator.*;
import ar.edu.itba.dcc.tp.semantic.symbols.VariableSymbol.VariableType;

@SuppressWarnings("unchecked")
%}

%token LT 257
%token GT 258
%token LTE 259
%token GTE 260
%token EQ 261
%token NEQ 262
%token PLUS 300
%token MINUS 301
%token TIMES 302
%token DIVIDE 303
%token MODULE 304
%token ASSIGN 305

%token COMMA 400
%token COLON 401
%token SEMICOLON 402
%token DOT 403
%token TO 404
	
%token OPEN_BRACE 500
%token CLOSE_BRACE 501
%token OPEN_PAREN 502
%token CLOSE_PAREN 503
%token OPEN_SQUARE_BRACKET 504
%token CLOSE_SQUARE_BRACKET 505
%token QUOTATION_MARK 506

%token K_INT 1100
%token K_REAL 1101
%token K_STRING 1102
%token K_NULL 1103
%token K_BOOLEAN 1104

%token ID 2000
%token LITERAL 2001
%token INTEGER 2002
%token REAL 2003

%token AND 3000
%token OR 3001
%token NOT 3002
%token TRUE 3003
%token FALSE 3004

%token USE 4001
%token IF 4002
%token ELSEIF 4003
%token THEN 4004
%token FOR 4005
%token IFNEVER 4007
%token IFQUIT 4008
%token QUIT 4009
%token CONSTRAINT 4010
%token FUNCTION 4011
%token TYPE 4012
%token LOCATION 4013
%token UNTIL 4014
%token ELSE 4015
%token IN 4016
%token LOOP 4017
%token WHILE 4018

%left OR
%left AND
%left EQ NEQ
%left ASSOC 
%left LT GT LTE GTE
%left MINUS PLUS
%right TIMES DIVIDE MODULE NOT
%left NEG

%%

pgm:	location_instr use_list pgm_content
		| location_instr pgm_content
		| { Compiler.getInstance().setCurrentLocation(""); analyzer.loadStandardLibrary(); } use_list pgm_content
        | { Compiler.getInstance().setCurrentLocation(""); analyzer.loadStandardLibrary(); } pgm_content
        ;

location_instr:	LOCATION ID submodule SEMICOLON	 { $$ = (String)$2 + (String)$3;
//													setWorkingDirectory((String)(((String)$2) + "/" + (String)$3));
													Compiler.getInstance().setCurrentLocation((String)$$);
													analyzer.loadStandardLibrary(); }
													
				//| empty          { $$ = ""; }
				;


use_list:	use_list use_instr SEMICOLON
			| use_instr SEMICOLON
			;
				
use_instr:	USE ID submodule     { analyzer.getSymbolTable().importFromExternalTable( Compiler.getInstance().compileFile(Compiler.getInstance().generateFilename((String)$2 + (String)$3) + ".gluck", line(), column() )); }
			| USE error  		{ logError(ConsoleProcessLogger.USE_INVALID_MODULE); }
			;
			
submodule:	submodule DOT ID 	{ $$ = (String)$1 + "/" + (String)$3; }
			| empty				{ $$ = ""; }
			| error				{ $$ = ""; logError(ConsoleProcessLogger.USE_INVALID_MODULE); }
			;

pgm_content:	pgm_instr pgm_content
                | empty
                ;

pgm_instr:	func_decl_instr
			| type_decl_instr
			| constr_decl_instr
			| var_decl_instr SEMICOLON
			| SEMICOLON
			| block_only_instrs   { logError(ConsoleProcessLogger.INVALID_INSTRUCTION_IN_GLOBAL_CONTEXT); }
			| error               { logError(ConsoleProcessLogger.INVALID_INSTRUCTION); }
			;

func_decl_instr:	ID COLON FUNCTION { analyzer.functionStartPreamble((String)$1, true); } input_params output_params 
					{ analyzer.functionStart((String)$1, (List<VariableSymbol>)$5, (List<VariableSymbol>)$6, true); } 
					block { analyzer.functionEnd((Sentence)$8); }
					| ID error { logError(ConsoleProcessLogger.FUNCTION_DECLARATION_INVALID);} block 
					;

block:	OPEN_BRACE block_content CLOSE_BRACE	{ $$ = $2; }
		;

block_content:	block_content m block_instrs	{ $$ = analyzer.blockSentence((Sentence)$1, (Integer)$2, (Sentence)$3); }
	        | empty								{ $$ = new Sentence();	}
	        ;
		
block_instrs:	var_decl_instr SEMICOLON	{ $$ = $1; }
				| block_only_instrs			{ $$ = $1; }
				| SEMICOLON					{ $$ = new Sentence(); }
				| error						{ logError(ConsoleProcessLogger.INVALID_INSTRUCTION); $$ = new Sentence(); }
				;

block_only_instrs:	assign_instr SEMICOLON	{ $$ = $1; }
			| if_instr						{ $$ = $1; }
			| loop_instr					{ $$ = $1; }
			| for_instr 					{ $$ = $1; }
			| func_call_instr SEMICOLON		{ $$ = new Sentence(); analyzer.functionCallExpression((FunctionCall)$1, false); }
			| quit_instr					{ $$ = $1; }
			;

input_params:	formal_params			{ $$ = analyzer.changeVariableType((List<VariableSymbol>)$1, VariableSymbol.VariableType.INPUT);  }
				| empty 				{ $$ = new ArrayList<VariableSymbol>(); }
				;

output_params:	TO formal_params 		{ $$ = analyzer.changeVariableType((List<VariableSymbol>)$2, VariableSymbol.VariableType.OUTPUT); }
				| empty					{ $$ = new ArrayList<VariableSymbol>(); }
				;

formal_params:	formal_params COMMA formal_param	{ ((ArrayList<VariableSymbol>)($$ = $1)).add((VariableSymbol)$3); }
		| formal_param  							{ $$ = new ArrayList<VariableSymbol>();  ((ArrayList<VariableSymbol>)$$).add((VariableSymbol)$1) ;  }
		| formal_params COMMA COMMA formal_param	{ logError(ConsoleProcessLogger.TYPES_LIST_TYPE_EXPECTED);	}
		;

formal_param:	ID COLON type	{ $$ = analyzer.formalParam((String)$1, (Type)$3, (List<FunctionSymbol>)((Type)$3).getConstraints()); }
				;
				
type:	type_type array_type constraint_types     { $$ = analyzer.checkType((TypeSymbol)$1, (ArrayList<Expression>)$2, (List<Constraint>)$3); } //TODO: modificar para contemplar arrays																		
	;	

type_type:	K_INT			{ $$ = IntTypeSymbol.getInstance(); }
			| K_REAL		{ $$ = DoubleTypeSymbol.getInstance(); }
			| K_STRING		{ $$ = new StringTypeSymbol(); }
			| K_BOOLEAN		{ $$ = BoolTypeSymbol.getInstance(); }
			| ID			{ $$ = analyzer.checkUserDefinedType((String)$1); }
			;
		
array_type:	array_type OPEN_SQUARE_BRACKET exp CLOSE_SQUARE_BRACKET   { ((ArrayList<Expression>)($$ = $1)).add((Expression)$3); }
	        | array_type OPEN_SQUARE_BRACKET CLOSE_SQUARE_BRACKET     { ((ArrayList<Expression>)($$ = $1)).add(new Expression(IntTypeSymbol.getInstance(), "0")); }
	        | empty  			                                      { $$ = new ArrayList<Expression>(); }	
	        ;
		
constraint_types:	constraint_types constraint_type	{ ((ArrayList<Constraint>)($$ = $1)).add((Constraint)$2); }	
			| empty										{ $$ = new ArrayList<Constraint>(); }
			;

constraint_type:	ID							{ $$ = analyzer.newConstraint((String)$1, new ArrayList<Expression>()); }
			| ID OPEN_PAREN exps CLOSE_PAREN	{ $$ = analyzer.newConstraint((String) $1, (List<Expression>)$3); }
			;
					
func_call_instr:	ID OPEN_PAREN actual_params CLOSE_PAREN    { $$ = new FunctionCall((String)$1, (List<Expression>)$3); }
			;
				
actual_params:		exps	{ $$ = $1; }
			;

type_decl_instr:	ID COLON TYPE OPEN_BRACE types CLOSE_BRACE { analyzer.declareType((String)$1, (List<VariableSymbol>)$5); }
					;
				
types: 		type_fields		{ $$ = $1; }
			;
			
type_fields: type_fields COMMA type_field          { ((ArrayList<VariableSymbol>)($$ = $1)).add((VariableSymbol)$3); }
			| type_field                            { $$ = new ArrayList<VariableSymbol>();  ((ArrayList<VariableSymbol>)$$).add((VariableSymbol)$1) ;  }
			;

type_field:	ID COLON type     { $$ = new VariableSymbol((String)$1, "NADA", ((Type)$3).getType(), (List<FunctionSymbol>)((Type)$3).getConstraints(), false, VariableType.COMPONENT, ((Type)$3).getArrayAddr()); }
			;
			
							
constr_decl_instr:	ID COLON CONSTRAINT { analyzer.functionStartPreamble((String)$1, false); } input_params OPEN_BRACE 
					{ $$ = analyzer.functionStart((String)$1, (List<VariableSymbol>)$5, new ArrayList<VariableSymbol>(), false); }
					exp { $$ = analyzer.constraintEnd((String)$1, (Expression)$8); } CLOSE_BRACE
					;
				
var_decl_instr:	ID COLON type var_init 		{ $$ = analyzer.declareVariable((String)$1, (Type)$3, (Expression)$4); } 
				;
				
var_init:	ASSIGN exp		{ $$ = $2;   }
			| empty			{ $$ = null; }
			;

lvalue: lvalue OPEN_BRACE ids  CLOSE_BRACE						{ $$ = analyzer.lvalueAccess((LValue)$1, (List<String>) $3); }
		| OPEN_BRACE exps CLOSE_BRACE							{ $$ = analyzer.lvalueExpList((ArrayList<Expression>) $2);}
		| lvalue OPEN_SQUARE_BRACKET exp CLOSE_SQUARE_BRACKET 	{ $$ = analyzer.lvalueArray((LValue)$1, (Expression)$3); }
		| ID													{ $$ = analyzer.lvalueVariable((String)$1); }
		| lvalue OPEN_BRACE INTEGER COMMA INTEGER CLOSE_BRACE	{ $$ = analyzer.lvalueArrayInterval((LValue)$1, Integer.parseInt((String)$3), Integer.parseInt((String)$5)); }
		;

ids: ids COMMA ID			{ ((ArrayList<String>) ($$ = $1)).add((String)$3); }
	| ID					{ $$ = new ArrayList<String>(); ((ArrayList<String>) $$).add((String)$1); }
	;
	
assign_instr:	lvalue ASSIGN exp					{ $$ = analyzer.assignValue((LValue)$1, (Expression)$3, true);   }
				;

exps:	exps COMMA exp			{ ((ArrayList<Expression>)($$ = $1)).add((Expression)$3); }
		| error					{ logError(ConsoleProcessLogger.EXPS_INVALID_LIST); $$ = new ArrayList<Expression>(); }
		| exp					{ $$ = new ArrayList<Expression>(); ((ArrayList<Expression>)$$).add((Expression) $1); }
		| empty					{ $$ = new ArrayList<Expression>(); }
		;

if_instr:	IF exp THEN						{ $$ = analyzer.ifSentenceStart((Expression)$2); }
			block n m elseif_list m else	{ $$ = analyzer.ifSentenceEnd((Sentence)$4, (Sentence)$5, (Integer)$6, (Integer)$7, (Sentence)$8, (Integer)$9, (Sentence)$10); }
			;

elseif_list:	elseif_list elseif 		{ $$ = analyzer.elseIfSentenceBlock((Sentence)$1, (Sentence)$2); }	
				| empty					{ $$ = null; }
				;

elseif:	ELSEIF exp THEN		{ $$ = analyzer.elseIfSentenceStart((Expression)$2); }
		block 	 			{ $$ = analyzer.elseIfSentenceEnd((Sentence)$4, (Sentence)$5);}
		;
			
else:	ELSE 		{ analyzer.enterContext(); } block { analyzer.leaveContext(); $$ = $3; }
		| empty		{ $$ = null; }
		;
	
loop_instr:  	loop_type m 		{ $$ = analyzer.loopSentenceStart(); } 
				exp LOOP 			{ $$ = analyzer.loopSentenceBlockStart((LoopType)$1, (String)$3, (Expression)$4); }
				loop_block 			{ $$ = analyzer.loopSentenceBlockEnd((Integer)$2, (ForSentence)$6, (Sentence)$7); }
				ifnever m ifquit 	{ $$ = analyzer.loopSentenceEnd((Sentence)$7, (Sentence)$8, (Sentence)$9, (Integer)$10, (Sentence)$11);}
            ;

loop_block: OPEN_BRACE loop_block_content CLOSE_BRACE	{ $$ = $2; }
			;

loop_block_content:	loop_instr_list		{ $$ = $1; }
					| empty				{ $$ = new Sentence(); }
					;

loop_instr_list:	loop_instr_list m loop_instrs		{ $$ = analyzer.loopSentenceBlockInstruction((Sentence)$1, (Integer)$2, (Sentence)$3); } 
					| loop_instrs						{ $$ = $1; }
					;

loop_instrs: 	var_decl_instr SEMICOLON	{ $$ = $1; }
				| block_only_instrs			{ $$ = $1; }
				| SEMICOLON;				{ $$ = new Sentence(); }
				;

quit_instr:	QUIT quit_if SEMICOLON			{ $$ = analyzer.quitSentence((Expression) $2); }
			;

quit_if:	IF exp					{ $$ =  $2; };
		| empty					{ $$ = null; }
		;

loop_type:      WHILE		{ $$ = LoopType.WHILE; }
                | UNTIL		{ $$ = LoopType.UNTIL; }
                ;

ifnever: 	IFNEVER {analyzer.enterContext(); } block { analyzer.leaveContext(); $$ = analyzer.ifNeverSentence((Sentence)$3); }
                | empty			{ $$ = analyzer.ifNeverSentence(null); }
                ;

ifquit:         IFQUIT {analyzer.enterContext(); } block { analyzer.leaveContext(); $$ = $3; }
                | empty			{ $$ = null; }
                ;
                
for_instr:	FOR ID IN m exp LOOP m 	{ $$ = analyzer.forSentenceBegin((String)$2, (Expression)$5);}  
				loop_block m 		{ $$ = analyzer.forSentenceBlock((Integer)$7, (ForSentence)$8, (Sentence)$9); } 
				ifnever m ifquit	{ $$ = analyzer.forSentenceEnd((Sentence)$9, (Sentence)$11, (Sentence)$12, (Integer)$13, (Sentence)$14); }
			;
	
exp:	INTEGER							{ $$ = analyzer.declareConstant(IntTypeSymbol.getInstance(), Integer.parseInt((String)$1)); }
		| REAL							{ $$ = analyzer.declareConstant(DoubleTypeSymbol.getInstance(), "&#" + "_const" + ((String)$1).replace('.', '_')); }
		| boolean						{ $$ = analyzer.declareConstant(BoolTypeSymbol.getInstance(), (Integer)$1); }
        | lvalue						{ $$ = $1; }
        | func_call_instr				{ $$ = analyzer.functionCallExpression((FunctionCall)$1, true); }
        | exp PLUS exp					{ $$ = analyzer.mathExpression(MathOperator.PLUS, (Expression)$1, (Expression)$3);   }
        | exp MINUS exp					{ $$ = analyzer.mathExpression(MathOperator.MINUS, (Expression)$1, (Expression)$3);  }
        | exp TIMES exp					{ $$ = analyzer.mathExpression(MathOperator.TIMES, (Expression)$1, (Expression)$3);  }
        | exp DIVIDE exp				{ $$ = analyzer.mathExpression(MathOperator.DIVIDE, (Expression)$1, (Expression)$3); }
        | MINUS exp %prec NEG			{ $$ = analyzer.mathExpression(MathOperator.UNARY_MINUS, (Expression)$2, (Expression)$2);  }
        | exp MODULE exp				{ $$ = analyzer.mathExpression(MathOperator.MODULE, (Expression)$1,(Expression)$3); }
        | OPEN_PAREN exp CLOSE_PAREN	{ $$ = $2; }
        | NOT exp						{ $$ = analyzer.booleanExpressionNot((Expression)$2); }
        | exp AND exp					{ $$ = analyzer.booleanExpressionAnd((Expression)$1, (Expression)$3); }		
        | exp OR exp					{ $$ = analyzer.booleanExpressionOr((Expression)$1, (Expression)$3); }	
        | exp rel exp %prec ASSOC		{ $$ = analyzer.relationalExpression((InstructionType)$2, (Expression)$1, (Expression)$3); }
        | LITERAL						{ $$ = analyzer.declareLiteral((String)$1); }
        | K_NULL						{ $$ = analyzer.nullConstant(); }
        | exp PLUS error				{ $$ = new Expression(((Expression)$1).getType(), "ERROR"); logError(ConsoleProcessLogger.EXPRESSION_INVALID_PLUS);	}
        | exp MINUS error				{ $$ = new Expression(((Expression)$1).getType(), "ERROR"); logError(ConsoleProcessLogger.EXPRESSION_INVALID_MINUS);	}
        | exp TIMES error				{ $$ = new Expression(((Expression)$1).getType(), "ERROR"); logError(ConsoleProcessLogger.EXPRESSION_INVALID_TIMES);	}
        | exp DIVIDE error				{ $$ = new Expression(((Expression)$1).getType(), "ERROR"); logError(ConsoleProcessLogger.EXPRESSION_INVALID_DIVIDE);	}
        | exp MODULE error				{ $$ = new Expression(((Expression)$1).getType(), "ERROR"); logError(ConsoleProcessLogger.EXPRESSION_INVALID_MODULE);	}
        | exp AND error					{ $$ = new Expression(((Expression)$1).getType(), "ERROR"); logError(ConsoleProcessLogger.EXPRESSION_INVALID_AND);	}
        | exp OR error					{ $$ = new Expression(((Expression)$1).getType(), "ERROR"); logError(ConsoleProcessLogger.EXPRESSION_INVALID_OR);		}
        | exp rel error %prec ASSOC		{ $$ = new Expression(((Expression)$1).getType(), "ERROR"); logError(ConsoleProcessLogger.EXPRESSION_INVALID_REL);	}
        ;

rel:	EQ         { $$ = InstructionType.IF_EQ;  }
        | NEQ	   { $$ = InstructionType.IF_NEQ; }	
        | GT       { $$ = InstructionType.IF_GT;  }	
        | GTE      { $$ = InstructionType.IF_GTE; }	
        | LT       { $$ = InstructionType.IF_LT;  }	
        | LTE      { $$ = InstructionType.IF_LTE; }	
        ;

boolean:	TRUE	   { $$ = 1; }
			| FALSE	   { $$ = 0; }
			;

m:		{ $$ = analyzer.getNextInstruction(); }
	        ;
		
n:		{ $$ = analyzer.generateGoto(); }
		;
		
empty: 	;
		
%%

protected void setYylval(String parserVal) {
	this.yylval = parserVal;
}

protected int yyparseWrapper() {
	return yyparse();
}

private void logError(String message) {
	logger.showError(getFileName(), line(), column(), message);
}
