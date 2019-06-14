/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Atsuhiko Yamanaka, JCraft,Inc. - implementation of promptForKeyboradInteractive
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui;

import java.util.Map;

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.*;
import org.eclipse.team.internal.ccvs.core.*;

/**
 * An authenticator that prompts the user for authentication info,
 * and stores the results in the Platform's authentication key-ring.
 */
public class WorkbenchUserAuthenticator implements IUserAuthenticator {
	public static boolean USE_ALTERNATE_PROMPTER = false;
	
	/**
	 * WorkbenchUserAuthenticator constructor.
	 */
	public WorkbenchUserAuthenticator() {
		super();
		// Initialize USE_ALTERNATE_PROMPTER
		IIgnoreInfo[] ignores = Team.getAllIgnores();
		boolean found = false;
		for (IIgnoreInfo ignore : ignores) {
			if (ignore.getPattern().equals("*.notes")) { //$NON-NLS-1$
				found = true;
			}
		}
		if (!found) return;
		IStringMapping[] mappings = Team.getFileContentManager().getExtensionMappings();
		for (IStringMapping mapping : mappings) {
			if (mapping.getString().equals("notes")) { //$NON-NLS-1$
				USE_ALTERNATE_PROMPTER = true;
				return;
			}
		}
		USE_ALTERNATE_PROMPTER = false;
	}
	@Override
	public void promptForUserInfo(final ICVSRepositoryLocation location, final IUserInfo userinfo, final String message) throws CVSException {
		if (!userinfo.isUsernameMutable() && USE_ALTERNATE_PROMPTER) {
			alternatePromptForUserInfo(userinfo);
			return;
		}
		// ask the user for a password
		final String[] result = new String[2];
		Display display = Display.getCurrent();
		final boolean allowCaching[] = {false};
		if (display != null) {
			allowCaching[0] = promptForPassword(location, userinfo.getUsername(), message, userinfo.isUsernameMutable(), result);
		} else {
			// sync exec in default thread
			final CVSException[] exception = new CVSException[] { null };
			Display.getDefault().syncExec(() -> {
				try {
					allowCaching[0] = promptForPassword(location, userinfo.getUsername(), message,
							userinfo.isUsernameMutable(), result);
				} catch (CVSException e) {
					exception[0] = e;
				}
			});
			if (exception[0] != null) {
				throw exception[0];
			}
		}
			
		if (result[0] == null) {
			throw new OperationCanceledException(CVSUIMessages.WorkbenchUserAuthenticator_cancelled); 
		}
		
		if (userinfo.isUsernameMutable()) {
			userinfo.setUsername(result[0]);
		
		}
		userinfo.setPassword(result[1]);
		
		if(location != null) {
			if (userinfo.isUsernameMutable()) {
				location.setUsername(result[0]);
			}
			location.setPassword(result[1]);
			location.setAllowCaching(allowCaching[0]);
		}
	}
	
	/**
	 * Asks the user to enter a password. Places the
	 * results in the supplied string[].  result[0] must
	 * contain the username, result[1] must contain the password.
	 * If the user canceled, both values must be zero.
	 * 
	 * @param location  the location to obtain the password for
	 * @param username  the username
	 * @param message  a message to display to the user
	 * @param userMutable  whether the user can be changed in the dialog
	 * @param result  a String array of length two in which to put the result
	 * @throws CVSException 
	 */
	private boolean promptForPassword(final ICVSRepositoryLocation location, final String username, final String message, final boolean userMutable, final String[] result) throws CVSException {
		String domain = location == null ? null : location.getLocation(true);
		boolean cachingCheckbox=true;
		
		if(location != null && location.getMethod().getName().equals("pserverssh2")){  //$NON-NLS-1$
			/**
			 * If this method 'promptForPassword' is invoked for ssh2 connection,
			 * we want to disable the checkbox for password caching, because it will
			 * overwrite the password for pserver with the password for ssh2.
			 * 
			 * In pserverssh2 connection type, its location name will be like
			 *    pserverssh2:pserver_username@ssh2_username@ssh2_host@pserver_host
			 *    
			 * The problem is that there is not a method to know if we are invoked from ssh2 or pserver.
			 * The following code will guess that if 'username' is equals to 'ssh2_userver' we are from ssh2,
			 * but if 'pserver2_username' is equals to 'ssh2_username', this guess will not be correct.
			 */
			String host = location.getHost();              // ssh2_username@ssh2_host@pserver_host
			int index = host.indexOf("@"); //$NON-NLS-1$
			if (index != -1) {
				cachingCheckbox = false;
				if (index != 0) {
					if (!username.equals(host.substring(0, index))) {
						cachingCheckbox = true;
					}
				}
			}
		}
		
		UserValidationDialog dialog = new UserValidationDialog(null, domain, (username==null)?"":username, message, cachingCheckbox);//$NON-NLS-1$
		dialog.setUsernameMutable(userMutable);
		dialog.open();	
		result[0] = dialog.getUsername();
		result[1] = dialog.getPassword();
		return dialog.getAllowCaching();
	}

	/**
	 * Asks the user to enter values. 
	 * 
	 * @param location  the location to obtain the password for
	 * @param destication the location
	 * @param name the name
	 * @param instruction the instruction
	 * @param prompt the titles for text fields
	 * @param echo '*' should be used or not
	 * @param result the entered values, or null if user canceled.
	 */
	@Override
	public String[] promptForKeyboradInteractive(final ICVSRepositoryLocation location,
							 final String destination,
							 final String name,
							 final String instruction,
							 final String[] prompt,
							 final boolean[] echo) throws CVSException {
		final String[][] result = new String[1][];
		final boolean[] allowCaching=new boolean[1];
		Display display = Display.getCurrent();
		if (display != null) {
		result[0]=_promptForUserInteractive(location, destination, name, instruction, prompt, echo, allowCaching);
		} 
		else {
			// sync exec in default thread
			Display.getDefault().syncExec(() -> result[0] = _promptForUserInteractive(location, destination, name,
					instruction, prompt, echo, allowCaching));
		}
		if (result[0] != null && location != null && 
				KeyboardInteractiveDialog.isPasswordAuth(prompt)) {
			location.setPassword(result[0][0]);
			location.setAllowCaching(allowCaching[0]);
		}
		return result[0];
	}

	private String[] _promptForUserInteractive(final ICVSRepositoryLocation location, 
							 final String destination,
							 final String name,
							 final String instruction,
							 final String[] prompt,
							 final boolean[] echo,
							 final boolean[] allowCaching) {
	
		String domain = location == null ? null : location.getLocation(true);
		String userName = location == null ? null : location.getUsername();
		boolean cachingCheckbox=true;
		
		if (location != null
				&& location.getMethod().getName().equals("pserverssh2")) { //$NON-NLS-1$
			/**
			 * We want to disable the checkbox for password caching, because it will
			 * overwrite the password for pserver with the password for ssh2.
			 * 
			 * In pserverssh2 connection type, its location name will be like
			 *    pserverssh2:pserver_username@ssh2_username@ssh2_host@pserver_host
			 * 
			 * 'userName' is 'pserver_username', so we also have to change it to 'ssh2_username'.
			 */
			String host = location.getHost();         // ssh2_username@ssh2_host@pserver_host
			int index = host.indexOf("@"); //$NON-NLS-1$
			if (index != -1) {
				if (index != 0) {
					userName = host.substring(0, index);
				}
				cachingCheckbox = false;
			}
		}
		
		KeyboardInteractiveDialog dialog = new KeyboardInteractiveDialog(null, 
										 domain,
										 destination,
										 name,
										 userName, 
										 instruction,
										 prompt,
										 echo,
										 cachingCheckbox);
		dialog.open();
		String[] _result=dialog.getResult();
		if(_result!=null)
			allowCaching[0]=dialog.getAllowCaching();
		return _result;
	}
	
	/**
	 * Special alternate prompting. Returns the password. Username must be fixed.
	 */
	private String alternatePromptForPassword(final String username) {
		AlternateUserValidationDialog dialog = new AlternateUserValidationDialog(null, (username == null) ? "" : username); //$NON-NLS-1$
		dialog.setUsername(username);
		int result = dialog.open();
		if (result == Window.CANCEL) return null;
		return dialog.getPassword();
	}
	
	/**
	 * Special alternate prompting.
	 */
	public void alternatePromptForUserInfo(final IUserInfo userinfo) {
		// ask the user for a password
		final String[] result = new String[1];
		Display display = Display.getCurrent();
		if (display != null) {
			result[0] = alternatePromptForPassword(userinfo.getUsername());
		} else {
			// sync exec in default thread
			Display.getDefault().syncExec(() -> result[0] = alternatePromptForPassword(userinfo.getUsername()));
		}
			
		if (result[0] == null) {
			throw new OperationCanceledException(CVSUIMessages.WorkbenchUserAuthenticator_The_operation_was_canceled_by_the_user_1); 
		}
		
		userinfo.setPassword(result[0]);
	}

	@Override
	public int prompt(final ICVSRepositoryLocation location, final int promptType, final String title, final String message, final int[] promptResponses, final int defaultResponse) {
		final Display display = CVSUIPlugin.getStandardDisplay();
		final int[] retval = new int[1];
		final String[] buttons = new String[promptResponses.length];
		for (int i = 0; i < promptResponses.length; i++) {
			int prompt = promptResponses[i];
			switch(prompt) { 
				case IUserAuthenticator.OK_ID: buttons[i] = IDialogConstants.OK_LABEL; break;
				case IUserAuthenticator.CANCEL_ID: buttons[i] = IDialogConstants.CANCEL_LABEL; break;
				case IUserAuthenticator.NO_ID: buttons[i] = IDialogConstants.NO_LABEL; break;
				case IUserAuthenticator.YES_ID: buttons[i] = IDialogConstants.YES_LABEL; break;
			}
		}
		
		display.syncExec(() -> {
			final MessageDialog dialog = new MessageDialog(new Shell(display), title, null /* title image */,
					NLS.bind(CVSUIMessages.WorkbenchUserAuthenticator_0, message, location.getLocation(true)),
					promptType, buttons, 1);
			retval[0] = dialog.open();
		});
		return retval[0];
	}
	
	@Override
	public boolean promptForHostKeyChange(final ICVSRepositoryLocation location) {
		final boolean[] openConfirm = new boolean[] { false };
		final Display display = CVSUIPlugin.getStandardDisplay();
		display.syncExec(
				() -> openConfirm[0] = MessageDialog.openConfirm(null, CVSUIMessages.WorkbenchUserAuthenticator_1,
						NLS.bind(CVSUIMessages.WorkbenchUserAuthenticator_2, new String[] { location.getHost() })));
		if (!openConfirm[0]) {
			throw new OperationCanceledException();
		}
		return openConfirm[0];
	}
	
	@Override
	public Map promptToConfigureRepositoryLocations(final Map alternativeMap) {
		final Map[] result = new Map[1];
		Display display = Display.getCurrent();
		if (display != null) {
			result[0] = openAlternativeRepositoryDialog(alternativeMap);
		} else {
			// sync exec in default thread
			Display.getDefault().syncExec(() -> result[0] = openAlternativeRepositoryDialog(alternativeMap));
		}
		return result[0];
	}
	
	/**
	 * Opens the Alternative Repository Selection dialog.
	 * 
	 * @param alternativeMap
	 * @return a map with selected repositories to use or <code>null</code>
	 *         when canceled.
	 */
	private Map openAlternativeRepositoryDialog(Map alternativeMap) {
		ConfigureRepositoryLocationsDialog dialog = new ConfigureRepositoryLocationsDialog(
				null, alternativeMap);
		int result = dialog.open();
		return result == IDialogConstants.CANCEL_ID ? null : dialog
				.getSelected();
	}
}
