package org.eclipse.update.internal.ui.security;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.net.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.update.core.Utilities;
import org.eclipse.update.internal.core.UpdateManagerPlugin;

/**
 * A transient database that remembers information, such as usernames and
 * passwords.  The information is stored in memory only and is discarted
 * when the Plaform shuts down.
 */
public class AuthorizationDatabase extends Authenticator {

	private InetAddress requestingSite;
	private int requestingPort;
	private String requestingProtocol;
	private String requestingPrompt;
	private String requestingScheme;
	private PasswordAuthentication savedPasswordAuthentication;

	// fields needed for caching the password
	public static final String INFO_PASSWORD = "org.eclipse.update.internal.ui.security.password"; //$NON-NLS-1$ 
	public static final String INFO_USERNAME = "org.eclipse.update.internal.ui.security.username"; //$NON-NLS-1$ 
	public static final String AUTH_SCHEME = ""; //$NON-NLS-1$ 

	/**
	 * The Map containing the userid and password
	 */
	private Map result = new Hashtable();

	/**
	 * 
	 */
	public void addAuthenticationInfo(URL serverUrl, String realm, String scheme, Map info) {
		try {
			Platform.addAuthorizationInfo(serverUrl, realm, scheme, info);
		} catch (CoreException e) {
			UpdateManagerPlugin.getPlugin().getLog().log(e.getStatus());
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
	public Map requestAuthenticationInfo(final URL resourceUrl, final String realm, final String scheme) {

		result = new Hashtable();
		if (scheme != null && scheme.equalsIgnoreCase("Basic")) {

			Display disp = Display.getCurrent();
			if (disp != null) {
				promptForPassword(resourceUrl, realm, result);
			} else {
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						promptForPassword(resourceUrl, realm, result);
					}
				});
			};
		}
		return result;
	}

	public void reset(){
			requestingPort = 0;
			requestingPrompt = null;
			requestingProtocol = null;
			requestingScheme = null;
			requestingSite = null;
	}

	/**
	 *
	 */
	private void promptForPassword(URL resourceUrl, String realm, Map info) {

		Shell shell = new Shell();
		UserValidationDialog ui = new UserValidationDialog(shell, resourceUrl, realm, ""); //$NON-NLS-1$
		ui.setUsernameMutable(true);
		ui.setBlockOnOpen(true);
		ui.open();

		if (ui.getReturnCode() != ui.CANCEL) {
			info.put(INFO_USERNAME, ui.getUserid());
			info.put(INFO_PASSWORD, ui.getPassword());
		}
		shell.dispose();

	}
	/*
	 * @see Authenticator#getPasswordAuthentication()
	 */
	protected PasswordAuthentication getPasswordAuthentication() {

		try {
			URL url = new URL(getRequestingProtocol(), getRequestingSite().getHostName(), getRequestingPort(), ""); //$NON-NLS-1$
			return retrievePasswordAuthentication(url);
		} catch (MalformedURLException e) {
			IStatus status = Utilities.newCoreException("",e).getStatus();
			UpdateManagerPlugin.getPlugin().getLog().log(status);
		}
		return new PasswordAuthentication("", new char[] { ' ' }); //$NON-NLS-1$
	}

	/*
	 * 
	 */
	private PasswordAuthentication retrievePasswordAuthentication(URL url) {

		Map map = null;
		String username = null;
		String password = null;

		if (equalsPreviousRequest()) {
			// same request, the userid/password was wrong
			// or user cancelled. force a refresh
			if (savedPasswordAuthentication != null)
				// only prompt if the user didn't cancel
				map = requestAuthenticationInfo(url, requestingPrompt, requestingScheme);
		} else {
			// save state
			requestingPort = getRequestingPort();
			requestingPrompt = getRequestingPrompt();
			requestingProtocol = getRequestingProtocol();
			requestingScheme = getRequestingScheme();
			requestingSite = getRequestingSite();
			map = Platform.getAuthorizationInfo(url, requestingPrompt, requestingScheme);
			if (map == null) {
				map = requestAuthenticationInfo(url, requestingPrompt, requestingScheme);
			}
		}

		if (map != null) {
			username = (String) map.get(INFO_USERNAME);
			password = (String) map.get(INFO_PASSWORD);
		}

		if (username!=null && password!=null){
			savedPasswordAuthentication =  new PasswordAuthentication(username, password.toCharArray());
		} else {
			savedPasswordAuthentication = null;
		}

		return savedPasswordAuthentication;

	}

	private boolean equalsPreviousRequest() {

		if (requestingPort != getRequestingPort())
			return false;

		if (requestingPrompt != null && !requestingPrompt.equals(getRequestingPrompt()))
			return false;
		if (requestingPrompt == null && getRequestingPrompt() != null)
			return false;

		if (requestingProtocol != null && !requestingProtocol.equals(getRequestingProtocol()))
			return false;
		if (requestingProtocol == null && getRequestingProtocol() != null)
			return false;

		if (requestingScheme != null && !requestingScheme.equals(getRequestingScheme()))
			return false;
		if (requestingScheme == null && getRequestingScheme() != null)
			return false;

		if (requestingSite != null && !requestingSite.equals(getRequestingSite()))
			return false;
		if (requestingSite == null && getRequestingSite() != null)
			return false;

		return true;
	}

}