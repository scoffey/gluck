package ar.edu.itba.dcc.tp.lexer;

/**
 * List of tokens retrieved by the parser.
 * <p>
 * Each token has a 
 * </p>
 */
public enum TokenTypes {
	LT(257, "<"),
	GT(258, ">"),
	LTE(259, "<="),
	GTE(260, ">="),
	EQ(261, "=="),
	NEQ(262, "!="),
	PLUS(300, "+"),
	MINUS(301, "-"),
	TIMES(302, "*"),
	DIVIDE(303, "/"),
	MODULE(304, "%"),
	ASSIGN(305, "="),
	
	COMMA(400, ","),
	COLON(401, ":"),
	SEMICOLON(402, ";"),
	DOT(403, "."),
	TO(404, "->"),
	
	OPEN_BRACE(500, "{"),
	CLOSE_BRACE(501,"}"),
	OPEN_PAREN(502, "("),
	CLOSE_PAREN(503,")"),
	OPEN_SQUARE_BRACKET(504, "["),
	CLOSE_SQUARE_BRACKET(505, "]"),
	QUOTATION_MARK(506, "\""),
	
	K_INT(1100, "int"),
	K_REAL(1101, "real"),
	K_STRING(1102, "string"),
	K_NULL(1103, "null"),
	K_BOOLEAN(1104, "bool"),
	
	ID(2000),
	LITERAL(2001),
	INTEGER(2002),
	REAL(2003),
	
	AND(3000, "and"),
	OR(3001, "or"),
	NOT(3002, "!"),
	TRUE(3003, "true"),
	FALSE(3004, "false"),
	
	USE(4001, "use"),
	IF(4002, "if"),
	ELSEIF(4003, "elseif"),
	THEN(4004, "then"),
	FOR(4005, "for"),
	IFNEVER(4007, "ifnever"),
	IFQUIT(4008, "ifquit"),
	QUIT(4009, "quit"),
	CONSTRAINT(4010, "constraint"),
	FUNCTION(4011, "function"),
	TYPE(4012, "type"),
	LOCATION(4013, "location"),
	UNTIL(4014, "until"),
	ELSE(4015, "else"),
	IN(4016, "in"),
	LOOP(4017, "loop"),
	WHILE(4018, "while");
	
	private int value;
	private String caption;
	
    private TokenTypes(int value) {
        this.value = value;
    }
    private TokenTypes(int value, String caption) {
        this.value = value;
        this.caption = caption;
    }
    
    public int getValue() {
    	return this.value;
    }
	@Override
	public String toString() {
		return this.caption == null ? super.toString() : this.caption;
	}
    
}
