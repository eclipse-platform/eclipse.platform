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
 *     Atsuhiko Yamanaka, JCraft,Inc. - adding promptForKeyboradInteractive method
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.core;

import java.util.List;
import java.util.Map;

/**
 * IUserAuthenticators are used to ensure that the user
 * is validated for access to a given repository.  The
 * user is prompted for a username and password as
 * appropriate for the given repository type.
 */
public interface IUserAuthenticator {
	/**
	 * Button id for an "Ok" button (value 0).
	 */
	public int OK_ID = 0;

	/**
	 * Button id for a "Cancel" button (value 1).
	 */
	public int CANCEL_ID = 1;

	/**
	 * Button id for a "Yes" button (value 2).
	 */
	public int YES_ID = 2;

	/**
	 * Button id for a "No" button (value 3).
	 */
	public int NO_ID = 3;
	
	/**
	 * 	Constant for a prompt with no type (value 0).
	 */
	public final static int NONE = 0;

	/**
	 * Constant for an error prompt (value 1).
	 */
	public final static int ERROR = 1;

	/**
	 * 	Constant for an information prompt (value 2).
	 */
	public final static int INFORMATION = 2;

	/**
	 * 	Constant for a question prompt (value 3).
	 */
	public final static int QUESTION = 3;

	/**
	 * 	Constant for a warning dialog (value 4).
	 */
	public final static int WARNING = 4;	
	
	/**
	 * Authenticates the user for access to a given repository.
	 * The obtained values for user name and password will be placed
	 * into the supplied user info object. Implementors are allowed to
	 * save user names and passwords. The user should be prompted for
	 * user name and password if there is no saved one, or if <code>retry</code>
	 * is <code>true</code>.
	 *
	 * @param location The repository location to authenticate the user for or <code>null</code>
	 * if this authentication is not for a CVS repository location.
	 * @param info The object to place user validation information into.
	 * @param retry <code>true</code> if a previous attempt to log in failed.
	 * @param message An optional message to display if, e.g., previous authentication failed.
	 * @return true if the validation was successful, and false otherwise.
	 */
	public void promptForUserInfo(ICVSRepositoryLocation location, IUserInfo userInfo, String message) throws CVSException;
	
	/**
	 * Prompts the user for a number values using text fields. The labels are provided in
	 * the <code>prompt</code> array.  Implementors will return the entered values, or null if
	 * the user cancel the prompt.
	 *
	 * @param location The repository location to authenticate the user for or <code>null</code>
	 * if this authentication is not for a CVS repository location.
	 * @param destination The destination in the format like username@hostname:port
	 * @param name A name about this dialog.
	 * @param instruction A message for the instruction.
	 * @param prompt Labels for text fields.
	 * @param echo the array to show which fields are secret.
	 * @return the entered values, or null if the user canceled.
	 *
	 * @since 3.0
	 */
	public String[] promptForKeyboradInteractive(ICVSRepositoryLocation location, String destination, String name, String instruction, String[] prompt, boolean[] echo) throws CVSException;

	/**
	 * Prompts the authenticator for additional information regarding this authentication 
	 * request. A default implementation of this method should return the <code>defaultResponse</code>,
	 * whereas alternate implementations could prompt the user with a dialog.
	 * 
	 * @param location the repository location for this authentication or <code>null</code>
	 * if this authentication is not for a CVS repository location.
	 * @param promptType one of the following values:
	 * <ul>
	 *	<li> <code>NONE</code> for a unspecified prompt type </li>
	 *	<li> <code>ERROR</code> for an error prompt </li>
	 *	<li> <code>INFORMATION</code> for an information prompt </li>
	 * 	<li> <code>QUESTION </code> for a question prompt </li>
	 *	<li> <code>WARNING</code> for a warning prompt </li>
	 * </ul>
	 * @param title the prompt title that could be displayed to the user
	 * @param message the prompt
	 * @param promptResponses the possible responses to the prompt
	 * @param defaultResponse the default response to the prompt
	 * @return the response to the prompt
	 * 
	 * @since 3.0
	 */
	public int prompt(ICVSRepositoryLocation location, int promptType, String title, String message, int[] promptResponses, int defaultResponseIndex);

	/**
	 * The host key for the given location has changed.
	 * @param location
	 * @return true if new host key should be accepted
	 */
	public boolean promptForHostKeyChange(ICVSRepositoryLocation location);
	
	/**
	 * If the project set contains only partial repository information (some of
	 * location are unknown) we will display a dialog to associate each project
	 * set repository with a known repository location or create a new one or at
	 * last use the default locations.
	 * 
	 * @param alternativeMap
	 *            a map of CVS repository locations form the project set (as
	 *            keys) and a list of suggested alternative (known) CVS
	 *            repository locations (as values)
	 * @return a map of CVS repository locations from the project set (as keys)
	 *         and confirmed CVS repository locations to be used during checkout
	 *         (as values) or <code>null</code> if the operation is to be
	 *         canceled
	 */
	public abstract Map<ICVSRepositoryLocation, List<String>> promptToConfigureRepositoryLocations(Map<ICVSRepositoryLocation, List<String>> alternativeMap);
}
