package ar.edu.itba.dcc.tp.lexer;

import ar.edu.itba.dcc.tp.util.*;
import static ar.edu.itba.dcc.tp.lexer.TokenTypes.*;

@SuppressWarnings("unused")
%%
%unicode
%public
%class GluckLexer
%byaccj
%extends LexerAdapter
%type TokenTypes

%{
	private int commentDepth;
	
	public GluckLexer(){
	}
	
	public int line(){
		return yyline;
	}
	
	public int column(){
		return yycolumn;
	}
	
	private void logError(String message) {
		logger.showError(fileName, yyline+1, yycolumn, message);
	}
%}

%line
%column
%char
%state BLOCKCOMMENT

%eofval{
if (yystate() == BLOCKCOMMENT) {
	logError(ConsoleProcessLogger.UNTERMINATED_COMMENT + "(" + (commentDepth+1) + " level" + (commentDepth == 0 ? "" : "s") + ")");
}
return null;
	
%eofval}


DIGIT=[0-9]
REAL={DIGIT}+\.{DIGIT}* | \.{DIGIT}+
INTEGER={DIGIT}+
ID=[a-zA-Z]+[a-zA-Z0-9_]*
LITERAL=\"[^\"]*\"
%%

<YYINITIAL> {

// Caracteres varios
"<"		{return LT; 	}
">"		{return GT; 	}
"<="	{return LTE;	}
">="	{return GTE;	}
"="		{return ASSIGN;	}
"=="	{return EQ;		}
"!="	{return NEQ;	}

"+"		{return PLUS;	}
"-"		{return MINUS;	}
"*"		{return TIMES;	}
"/"		{return DIVIDE;	}
"%"		{return MODULE;	}
	
","		{return COMMA; 		}
":"		{return COLON; 		}
";"		{return SEMICOLON;	}
"."		{return DOT; 		}
"->"	{return TO; 		}

"{"		{return OPEN_BRACE;				}
"}"		{return CLOSE_BRACE;			}
"("		{return OPEN_PAREN; 			}
")"		{return CLOSE_PAREN;			}
"["		{return OPEN_SQUARE_BRACKET; 	}
"]"		{return CLOSE_SQUARE_BRACKET;	}
	
// Palabras reservadas	
"type"			{return TYPE; 		}
"function"		{return FUNCTION; 	}
"constraint"	{return CONSTRAINT;	}

// Tipos de datos primitivos	
"int"		{return K_INT;		}
"real"		{return K_REAL; 	}
"string"	{return K_STRING;	}
"bool"		{return K_BOOLEAN;	}
"null"		{return K_NULL; 	}


"and"			{return AND;	}
"or"			{return OR;		}
"!"				{return NOT; 	}
"true"			{return TRUE;	}
"false"			{return FALSE;	}

"use"			{return USE;	}
"location"		{return LOCATION;}
"if"			{return IF;		}
"then"			{return THEN;	}
"else"			{return ELSE;	}
"elseif"		{return ELSEIF;	}
"ifnever"		{return IFNEVER;}
"ifquit"		{return IFQUIT;	}
"quit"			{return QUIT;	}
"for"			{return FOR;	}
"loop"			{return LOOP;	}
"while"			{return WHILE;	}
"until"			{return UNTIL;	}
"in"			{return IN;		}


{REAL}			{finalText = yytext(); return REAL; 	}
{INTEGER}		{finalText = yytext(); return INTEGER;	}
{ID}			{finalText = yytext(); return ID; 		}
{LITERAL}		{finalText = yytext(); return LITERAL;	}

// Comentarios
"/*"			{	yybegin(BLOCKCOMMENT);
					commentDepth = 0;		}

"*/"			{	logError(ConsoleProcessLogger.UNEXPECTEDLY_CLOSED_COMMENT);  }
				
"//".*		{ ; }


[\t \n\r]*		{ ; }


// Recuperación de errores léxicos.

// Entero inválido
[0-9]+[a-zA-Z]+		{	logError(ConsoleProcessLogger.INVALID_INTEGER + yytext());
        				int i;
        				for (i = 0; i < yytext().length(); i++) {
                			if (yytext().charAt(i) - '0' > 10 || yytext().charAt(i) - '0' < 0)
                        		break;
        				}
        				this.finalText = yytext().substring(0, i);
        				return INTEGER;
					}

// Real inválido
{REAL}[a-zA-Z]+		{  	logError(ConsoleProcessLogger.INVALID_REAL + yytext());
        				int i;
        				for (i = 0; i < yytext().length(); i++) {
                			if (yytext().charAt(i) - '0' > 10 || yytext().charAt(i) - '0' < 0 || yytext().charAt(i) != '.')
                        		break;
        				}
        				this.finalText = yytext().substring(0, i);
						return REAL;
						}

_[0-9]+[a-zA-Z]*	{
						logError(ConsoleProcessLogger.INVALID_INTEGER + yytext());
        				int i;
        				for (i = 1; i < yytext().length(); i++) {
                			if (yytext().charAt(i) - '0' > 10 || yytext().charAt(i) - '0' < 0)
                        		break;
        				}
        				this.finalText = yytext().substring(0, i);
        				return INTEGER;
					}
					
_[a-zA-Z]+{ID}		{	logError(ConsoleProcessLogger.INVALID_IDENTIFIER + yytext());
						this.finalText = yytext().substring(1, yytext().length()-1);
        				return ID;
					}

.					{ 	logError(ConsoleProcessLogger.UNEXPECTED_CHARACTER + yytext()); }

}

// Comentarios

<BLOCKCOMMENT>	{
"*/"			{	if (commentDepth > 0)
						commentDepth--;
					else
						yybegin(YYINITIAL);
				}
				
"/*"			{ commentDepth++; }

.|\n			{ ; }
}