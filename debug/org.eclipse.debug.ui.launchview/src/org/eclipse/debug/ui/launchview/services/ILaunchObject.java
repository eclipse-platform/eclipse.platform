/*******************************************************************************
 * Copyright (c) 2017, 2019 SSI Schaefer IT Solutions GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SSI Schaefer IT Solutions GmbH
 *******************************************************************************/
package org.eclipse.debug.ui.launchview.services;

import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchMode;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;

/**
 * A {@link ILaunchObject} describes a single launch-able "thing".
 * <p>
 * Instances of this interface are provided by extensions by a custom
 * {@link ILaunchObjectProvider} implementation in the extension.
 *
 * @since 1.0.2
 */
public interface ILaunchObject extends Comparable<ILaunchObject> {

	/**
	 * @return the unique ID of the {@link ILaunchObject}.
	 */
	public String getId();

	/**
	 * @return a label for display purposes.
	 */
	public StyledString getLabel();

	/**
	 * @return an image, usually derived from the type of the launch
	 */
	default public Image getImage() {
		if (getType() == null) {
			return null;
		}
		return DebugPluginImages.getImage(getType().getIdentifier());
	}

	/**
	 * @return the underlying {@link ILaunchConfigurationType} of the launch
	 */
	public ILaunchConfigurationType getType();

	/**
	 * Launch the {@link ILaunchObject} in the specified mode.
	 *
	 * @param mode in which mode to launch
	 */
	public void launch(ILaunchMode mode);

	/**
	 * @return whether this {@link ILaunchObject} supports termination by the
	 *         user.
	 */
	public boolean canTerminate();

	/**
	 * Terminates all running instances of this {@link ILaunchObject}.
	 */
	public void terminate();

	/**
	 * First terminates and then launches this {@link ILaunchObject} if it is
	 * running already.
	 */
	public void relaunch();

	/**
	 * Triggers custom editing UI logic for the {@link ILaunchObject}. Might open
	 * an editor, or a dialog, or do something completely different depending on
	 * the implementation.
	 */
	public void edit();

	/**
	 * @return whether this {@link ILaunchObject} should be displayed in the
	 *         favorite container of the view.
	 */
	public boolean isFavorite();

}
