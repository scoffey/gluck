package ar.edu.itba.dcc.tp.parser;

import ar.edu.itba.dcc.tp.lexer.Lexer;
import ar.edu.itba.dcc.tp.lexer.Token;
import ar.edu.itba.dcc.tp.lexer.TokenTypes;
import ar.edu.itba.dcc.tp.run.Compiler;
import ar.edu.itba.dcc.tp.semantic.SemanticAnalyzer;
import ar.edu.itba.dcc.tp.util.ProcessLogger;

/**
 * Clase que adapta los métodos generados por Byaccj en los parsers a la
 * interfaz Parser de la cátedra. Los parsers generados deben extender de esta
 * clase, que implementa <code>Parser</code>.
 */
public abstract class ParserAdapter implements Parser {
	protected Lexer lexer;
	protected ProcessLogger logger;
	protected SemanticAnalyzer analyzer;
	protected String workingDirectory = "";

	public void parse(Lexer lex) {
		this.lexer = lex;
		this.yyparseWrapper();
	}

	public SemanticAnalyzer getSemanticAnalyzer() {
		return analyzer;
	}

	public void setSemanticAnalyzer(SemanticAnalyzer analyzer) {
		this.analyzer = analyzer;
	}

	public void setFeedback(ProcessLogger logger) {
		this.logger = logger;
	}

	protected int yylex() {
		Token<TokenTypes> token = lexer.nextToken();
		Object value;
		if (token == null) {
			return 0; /* EOF */
		}

		value = token.getValue();
		if (value instanceof String)
			setYylval(new String((String) value));
		else
			setYylval(token.getValue() == null ? null : token.getValue().toString());

		return token.getType().getValue();
	}

	protected void yyerror(String error) {
	}

	public String getFileName() {
		return lexer.getFileName();
	}

	public int line() {
		return lexer.line() + 1;
	}

	public int column() {
		return lexer.column();
	}

	public void setWorkingDirectory(String workingDirectory) {
		this.workingDirectory = workingDirectory;
	}

	public String getFullName() {
		String[] arr = lexer.getFileName().split("/");
		return Compiler.getInstance().getCurrentLocation()
				+ (Compiler.getInstance().getCurrentLocation().equals("") ? "" : "/") + arr[arr.length - 1];
	}

	protected abstract void setYylval(String parserVal);

	protected abstract int yyparseWrapper();
}
