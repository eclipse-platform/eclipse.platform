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

import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.debug.ui.launchview.internal.view.LaunchViewImpl;
import org.eclipse.e4.ui.model.application.ui.menu.MMenu;

/**
 * Provides different {@link ILaunchObject} instances to the view for display.
 * Also allows some interaction with the view by means of an update listener and
 * menu contributions.
 * <p>
 * Contribute extensions to the view by implementing this interface and
 * registering it as a {@literal @Component} (declarative service).
 *
 * @since 1.0.2
 */
public interface ILaunchObjectProvider {

	/**
	 * @return all {@link ILaunchObject}s that this provider contributes to the
	 *         view
	 */
	public Set<? extends ILaunchObject> getLaunchObjects();

	/**
	 * @param r register a {@link Runnable} that should be notified whenever the
	 *            provider's state changed. The view will react with refreshing
	 *            it's in-memory models.
	 */
	public void addUpdateListener(Runnable r);

	/**
	 * @param r a previously registered update listener {@link Runnable}.
	 */
	public void removeUpdateListener(Runnable r);

	/**
	 * @return the priority of the provider. The default (debug.core) provider
	 *         has priority 0. A higher priority means that
	 *         {@link ILaunchObject}s with the same type and id will take
	 *         precedence. This allows to provide {@link ILaunchObject}s that
	 *         will generate {@link ILaunchObject}s in lower priority providers.
	 */
	public int getPriority();

	/**
	 * @param selected supplier for selected elements in the view.
	 * @param menu the view's menu where items can be contributed.
	 */
	public void contributeViewMenu(Supplier<Set<ILaunchObject>> selected, MMenu menu);

	/**
	 * Contribute per-item context menu items here. Use
	 * {@link LaunchViewImpl#getSelectedElements()} to retrieve selected
	 * elements during CanExecute and Execute methods of menu items.
	 *
	 * @param selected supplier for selected elements in the view.
	 * @param menu the context menu applied to items in the tree.
	 */
	public void contributeContextMenu(Supplier<Set<ILaunchObject>> selected, MMenu menu);

}
