package org.eclipse.update.internal.ui.security;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */


import java.net.*;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.update.internal.core.net.http.client.Authenticator;
import org.eclipse.core.runtime.*;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.update.internal.core.UpdateManagerPlugin;

/**
 * A transient database that remembers information, such as usernames and
 * passwords.  The information is stored in memory only and is discarted
 * when the Plaform shuts down.
 */
public class UpdateManagerAuthenticator extends Authenticator {

	// fields needed for caching the password
	public static final String INFO_PASSWORD = "password"; //$NON-NLS-1$ 
	public static final String INFO_USERNAME = "username"; //$NON-NLS-1$ 
	public static final String AUTH_SCHEME = ""; //$NON-NLS-1$ 

	private InetAddress requestingSite;
	private int requestingPort;
	private String requestingProtocol;
	private String requestingPrompt;
	private String requestingScheme;
	private Map savedPasswordAuthentication;

	/**
	 * The Map containing the userid and password
	 * HashMap allows <code>null</code> as the value
	 */
	private Map result = new HashMap();

	/**
	 *  
	 */
	public void addAuthenticationInfo(URL serverUrl, String realm, String scheme, Map info) {
		try {
			Platform.addAuthorizationInfo(serverUrl, realm, scheme, info);
		} catch (CoreException e) {
			UpdateManagerPlugin.warn("",e);
		}
	}

	/**
	 *
	 */
	public Map getAuthenticationInfo(URL serverUrl, String realm, String scheme) {
		return Platform.getAuthorizationInfo(serverUrl, realm, scheme);
	}

	/**
	 * 
	 */
	public Map requestAuthenticationInfo(URL resourceUrl,String realm,String scheme) {
		if (!equalsPreviousRequest(resourceUrl,realm,scheme)) {
			// save state
			InetAddress ip = null;
			try {
				ip = InetAddress.getByName(resourceUrl.getHost());
			} catch (UnknownHostException e){
				UpdateManagerPlugin.warn("",e);
			}
			
			this.requestingPort = resourceUrl.getPort();
			this.requestingPrompt = realm;
			this.requestingProtocol = resourceUrl.getProtocol();
			this.requestingScheme = scheme;
			this.requestingSite = ip;
			
			// try to get the password info from the in-memory database first
			Map map = Platform.getAuthorizationInfo(resourceUrl, requestingPrompt, requestingScheme);
			if (map == null) {
				map = retrievePasswordAuthentication(resourceUrl, requestingPrompt, requestingScheme);
			}
			savedPasswordAuthentication =  map;			
		} 

		// we must return a valid Map while we internally manage Cancel dialog (map==null -> dialog cancelled)
		return savedPasswordAuthentication;
	}

	/**
	 * @see org.eclipse.update.internal.core.net.http.client.IAuthenticator#addProtectionSpace(URL, String)
	 */
	public void addProtectionSpace(URL resourceUrl, String realm) {
		try {
			Platform.addProtectionSpace(resourceUrl, realm);
		}
		catch (CoreException e) {
			UpdateManagerPlugin.warn("",e);
		}
	}

	/**
	 * @see org.eclipse.update.internal.core.net.http.client.IAuthenticator#getProtectionSpace(URL)
	 */
	public String getProtectionSpace(URL resourceUrl) {
		return Platform.getProtectionSpace(resourceUrl);
	}


	/*
	 * forces a refresh
	 */
	public void reset(){
			requestingPort = 0;
			requestingPrompt = null;
			requestingProtocol = null;
			requestingScheme = null;
			requestingSite = null;
	}

	/*
	 * returns a map that contains the userid and the password
	 * or <code>null</code> if the dialog has been cancelled
	 */
	private Map retrievePasswordAuthentication(final URL resourceUrl, final String realm, final String scheme) {
		result = new HashMap();
		if ("Basic".equalsIgnoreCase(scheme)) {
			Display disp = Display.getCurrent();
			if (disp != null) {
				result = promptForPassword(resourceUrl, realm);
			} else {
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						result = promptForPassword(resourceUrl, realm);
					}
				});
			};
		}
		return result;
	}

	/*
	 * returns true if this request is the same as the saved one
	 * used to prevent double dialog if user cancelled or entered wrong userid/password
	 */
	private boolean equalsPreviousRequest(URL url,String realm,String scheme) {

		if (requestingPort != url.getPort())
			return false;

		if (requestingPrompt != null && !requestingPrompt.equals(realm))
			return false;
		if (requestingPrompt == null && realm != null)
			return false;

		if (requestingProtocol != null && !requestingProtocol.equals(url.getProtocol()))
			return false;
		if (requestingProtocol == null && url.getProtocol() != null)
			return false;

		if (requestingScheme != null && !requestingScheme.equals(scheme))
			return false;
		if (requestingScheme == null && scheme != null)
			return false;

		InetAddress ip = null;
		try {
			ip = InetAddress.getByName(url.getHost());
		} catch (UnknownHostException e){
			UpdateManagerPlugin.warn("",e);
		}

		if (requestingSite != null && !requestingSite.equals(ip))
			return false;
		if (requestingSite == null && ip != null)
			return false;

		return true;
	}

	/*
	 * returns a map that contains the userid and the password
	 * or <code>null</code> if the dialog has been cancelled
	 */
	private Map promptForPassword(URL resourceUrl, String realm) {

		Map result = null;
		Shell shell = new Shell();
		UserValidationDialog ui = new UserValidationDialog(shell, resourceUrl, realm, ""); //$NON-NLS-1$
		ui.setUsernameMutable(true);
		ui.setBlockOnOpen(true);
		ui.open();

		boolean isCancelled = ui.getReturnCode() == ui.CANCEL;
		if (!isCancelled){
			result= new HashMap();
			result.put(INFO_USERNAME, ui.getUserid());
			result.put(INFO_PASSWORD, ui.getPassword());
		}
		shell.dispose();
		
		return result;
	}
}