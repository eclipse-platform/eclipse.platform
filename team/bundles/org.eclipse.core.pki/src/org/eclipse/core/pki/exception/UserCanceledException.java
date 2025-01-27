package org.eclipse.core.pki.exception;

public class UserCanceledException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public UserCanceledException() {
		super();
	}

	/**
	 * @param message
	 * @param cause
	 */
	public UserCanceledException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 */
	public UserCanceledException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public UserCanceledException(Throwable cause) {
		super(cause);
	}

	
	
}
