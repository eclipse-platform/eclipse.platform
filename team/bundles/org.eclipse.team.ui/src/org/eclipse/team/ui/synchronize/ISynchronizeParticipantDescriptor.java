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
package org.eclipse.team.ui.synchronize;

import org.eclipse.jface.resource.ImageDescriptor;

/**
 * A participant descriptor contains the content of the
 * <code>synchronizeParticipants</code> extension section for
 * for a registered participant type in the declaring plug-in's
 * manifest (<code>plugin.xml</code>) file.
 * <p>
 * Clients are not intended to implement this interface.
 * </p>
 * @see ISynchronizeManager#getParticipantDescriptor(String)
 * @since 3.0
 */
public interface ISynchronizeParticipantDescriptor {
	/**
	 * Returns the name of this participant. This can be shown to the user.
	 *
	 * @return the name of this participant. This can be shown to the user.
	 */
	public String getName();

	/**
	 * Returns a string describing this participant type.
	 *
	 * @return a string describing this participant type.
	 */
	public String getDescription();

	/**
	 * Returns the unique id that identifies this participant type.
	 *
	 * @return the unique id that identifies this participant type.
	 */
	public String getId();

	/**
	 * Returns the image descriptor for this participant type.
	 *
	 * @return the image descriptor for this participant type.
	 */
	public ImageDescriptor getImageDescriptor();

	/**
	 * Returns if this participant can be persisted between sessions.
	 *
	 * @return <code>true</code> if this participant can be persisted between sessions
	 * and false otherwise.
	 */
	public boolean isPersistent();
}
