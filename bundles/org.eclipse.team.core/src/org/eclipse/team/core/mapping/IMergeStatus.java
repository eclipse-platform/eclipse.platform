/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.team.core.mapping;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.core.mapping.provider.MergeStatus;

/**
 * A special status that is returned when the return code of the
 * <code>merge</code> method is <code>CONFLICTS</code>. It is possible that
 * there were problems that caused the auto-merge to fail. In that case, the
 * implementor of <code>IResourceMappingMerger</code> can return a multi-status
 * in which one of the children is a <code>MergeStatus</code> and the others
 * describe other problems that were encountered.
 *
 * @see org.eclipse.team.core.mapping.IResourceMappingMerger
 * @see MergeStatus
 *
 * @since 3.2
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IMergeStatus extends IStatus {

	/**
	 * Indicates that a change conflict prevented some or all of the resource
	 * mappings to be merged (value <code>1</code>). When this code is
	 * returned, the status must be of type
	 * <code>MergeStatus</code> and must contain the list of all
	 * resource mappings for which a manual merge is required.
	 */
	public static final int CONFLICTS = 1;

	/**
	 * Status code describing an internal error (value <code>2</code>).
	 * The status return is not required to be of type <code>MergeStatus</code>
	 * for internal errors.
	 */
	public static final int INTERNAL_ERROR = 2;

	/**
	 * Returns the set of resource mappings for which an auto-merge was
	 * not performed. If the code of the status is <code>CONFLICTS</code>
	 * the status may contain a set of mappings or files depending
	 * on what method returned the status.
	 * @return the set of resource mappings for which an auto-merge was
	 * not performed.
	 */
	public abstract ResourceMapping[] getConflictingMappings();

	/**
	 * Returns the set of file for which an auto-merge was
	 * not performed. If the code of the status is <code>CONFLICTS</code>
	 * the status may contain a set of mappings or files depending
	 * on what method returned the status.
	 * @return the set of files for which an auto-merge was
	 * not performed.
	 */
	public abstract IFile[] getConflictingFiles();

}
