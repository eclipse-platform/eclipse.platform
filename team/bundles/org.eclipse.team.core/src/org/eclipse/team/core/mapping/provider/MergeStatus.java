/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
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
package org.eclipse.team.core.mapping.provider;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.core.mapping.IMergeStatus;

/**
 * A special status that is returned when the return code
 * of the <code>merge</code> method is <code>CONFLICTS</code>.
 * It is possible that there were problems that caused the
 * auto-merge to fail. In that case, the implementor of
 * <code>IResourceMappingMerger</code> can return a multi-status
 * in which one of the children is a <code>MergeStatus</code> and
 * the others describe other problems that were encountered.
 *
 * @see org.eclipse.team.core.mapping.IResourceMappingMerger
 *
 * @since 3.2
 */
public class MergeStatus extends Status implements IMergeStatus {

	private ResourceMapping[] conflictingMappings;
	private IFile[] conflictingFiles;

	/**
	 * Create a merge status for reporting that some of the resource mappings
	 * for which a merge was attempted were not auto-mergable.
	 * @param pluginId the plugin id
	 * @param message the message for the status
	 * @param conflictingMappings the mappings which were not auto-mergable
	 */
	public MergeStatus(String pluginId, String message, ResourceMapping[] conflictingMappings) {
		super(IStatus.ERROR, pluginId, CONFLICTS, message, null);
		this.conflictingMappings = conflictingMappings;
	}

	/**
	 * Create a merge status for reporting that some of the files
	 * for which a merge was attempted were not auto-mergable.
	 * @param pluginId the plugin id
	 * @param message the message for the status
	 * @param files the files which were not auto-mergable
	 */
	public MergeStatus(String pluginId, String message, IFile[] files) {
		super(IStatus.ERROR, pluginId, CONFLICTS, message, null);
		this.conflictingFiles = files;
	}

	@Override
	public ResourceMapping[] getConflictingMappings() {
		return conflictingMappings;
	}

	@Override
	public IFile[] getConflictingFiles() {
		return conflictingFiles;
	}
}
