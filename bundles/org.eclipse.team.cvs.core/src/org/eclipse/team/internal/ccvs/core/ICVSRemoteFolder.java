/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.core;

 
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;

 /**
 * This interface represents a remote folder in a repository. It provides
 * access to the members (remote files and folders) of a remote folder
 * 
 * Clients are not expected to implement this interface.
 */
public interface ICVSRemoteFolder extends ICVSRemoteResource, ICVSFolder {
	
	// This constant is the name of the folder at the root of a repository
	public static final String REPOSITORY_ROOT_FOLDER_NAME = ""; //$NON-NLS-1$
	
	/**
	 * Return the context of this handle. The returned tag can be a branch or
	 * version tag.
	 */
	public CVSTag getTag();
	
	/**
	 * Return the local options that are used to determine how memebers are retrieved.
	 * 
	 * Interesting options are:
	 *     Checkout.ALIAS
	 *     Command.DO_NOT_RECURSE
	 */
	public LocalOption[] getLocalOptions();
	
	/**
	 * Indicates whether the remote folder can be expanded. 
	 * 
	 * This is a temporary (hopefully) means of indicating certain types of folders 
	 * (i.e. module definitions) that are not expandable due to lack of mdoule expansion.
	 * They can still be checked out.
	 */
	public boolean isExpandable();
	
	/**
	 * Indicates whether the remote folder is an actual remote folder is a
	 * module defined in the CVSROOT/modules file (or some other module
	 * definition).
	 */
	public boolean isDefinedModule();
}
