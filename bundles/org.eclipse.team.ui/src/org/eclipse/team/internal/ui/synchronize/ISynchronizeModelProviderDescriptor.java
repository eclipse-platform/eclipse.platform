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
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.jface.resource.ImageDescriptor;

public interface ISynchronizeModelProviderDescriptor {
	/**
	 * Returns the name of this model provider. This can be shown to the user.
	 * @return the name of this model provider.
	 */
	public String getName();

	/**
	 * Returns the unique identifier for this model provider.
	 * @return the unique identifier for this model provider.
	 */
	public String getId();

	/**
	 * Returns the image that represents this model provider. This image
	 * will be shown to the user.
	 * @return the image that represents this model provider.
	 */
	public ImageDescriptor getImageDescriptor();
}
