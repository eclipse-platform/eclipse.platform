package org.eclipse.update.internal.core;

import java.net.Authenticator;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
public class HttpClient {

	/**
	 * Method close.
	 */
	public void close() {
	}

	/**
	 * Method setAuthenticator.
	 * @param authenticator
	 */
	public void setAuthenticator(Authenticator authenticator) {
		Authenticator.setDefault(authenticator);
	}

}
