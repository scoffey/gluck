package ar.edu.itba.dcc.tp.parser;

import ar.edu.itba.dcc.tp.lexer.Lexer;
import ar.edu.itba.dcc.tp.util.ProvidesFeedback;

/**
 * Interface implemented by parsers
 */
public interface Parser extends ProvidesFeedback {
	
	/**
	 * Parses input provided by a given lexical analyzer
	 * @param lex the lexical analyzer
	 */
	public void parse(Lexer lex);

}
