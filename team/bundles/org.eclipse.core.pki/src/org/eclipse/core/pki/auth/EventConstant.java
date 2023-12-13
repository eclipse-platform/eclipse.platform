package org.eclipse.core.pki.auth;

public enum EventConstant {
	DONE(0),
	CANCEL(2),
	SETUP(10);
	private int value;
	EventConstant( int request ) {
		value=request;
	}
	public int getValue() {
		return value;
	}
}
