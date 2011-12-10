package ar.edu.itba.dcc.tp.lexer;

import java.io.IOException;
import java.io.Reader;

import ar.edu.itba.dcc.tp.util.ProcessLogger;

/**
 * Clase que adapta los métodos generados por JFlex en los lexers a la interfaz
 * Lexer de la cátedra. El lexer generado debe extender de esta clase, que
 * implementa Lexer.
 */
public abstract class LexerAdapter implements Lexer {

	protected ProcessLogger logger;
	protected String fileName;
	protected String finalText;
	
	// @Override
	public Token<TokenTypes> nextToken() throws IllegalStateException {
		TokenTypes tokenType;
		
		finalText = null;
		
		try {
			if ((tokenType = this.yylex()) == null) {
				return null;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return new Token<TokenTypes>(tokenType, finalText == null ? null : new String(finalText));
	}

	// @Override
	public void process(Reader input, String fileName) {
		this.yyreset(input);
		this.fileName = fileName;
	}

	// @Override
	public void setFeedback(ProcessLogger logger) {
		this.logger = logger;
	}
	
	// @Override
	public String getFileName() {
		return fileName;
	}
	
	public ProcessLogger getFeedback() {
		return logger;
	}

	public abstract TokenTypes yylex() throws java.io.IOException;

	public abstract String yytext();

	public abstract void yyreset(java.io.Reader reader);
	



}
