package ar.edu.itba.dcc.tp.util;


/**
 * Interface to be implemented by classes that provide feedback about
 * a running process 
 */
public interface ProvidesFeedback {
	/**
	 * Sets the process logger that will receive the feedback
	 * @param logger
	 */
	public void setFeedback(ProcessLogger logger);
	
}
