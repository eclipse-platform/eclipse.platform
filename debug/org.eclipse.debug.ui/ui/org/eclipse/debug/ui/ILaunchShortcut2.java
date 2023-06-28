/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
package org.eclipse.debug.ui;

import org.eclipse.core.resources.IResource;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;


/**
 * <p>
 * An extension to a standard launch shortcut ({@link ILaunchShortcut}) allowing
 * launch shortcuts to specify how selections and editors should be launched.
 * </p>
 * <p>
 * To launch a selection (or active editor), the debug platform derives a resource associated
 * with the selection (or active editor), and then resolves the most recently launched configuration
 * associated with that resource. This interface allows a launch shortcut to override the
 * framework's resource and launch configuration resolution for selections (and active editors).
 * </p>
 * <p>
 * NOTE: the methods in this interface can be called in a <b>non-UI</b> thread.
 * </p>
 * <p>
 * Clients contributing a launch shortcut are intended to implement this interface.
 * </p>
 * @see org.eclipse.debug.internal.ui.contextlaunching.ContextRunner
 * @see org.eclipse.debug.internal.ui.contextlaunching.LaunchingResourceManager
 * @since 3.4
 */
public interface ILaunchShortcut2 extends ILaunchShortcut {

	/**
	 * Returns an array of  <code>ILaunchConfiguration</code>s that apply to the specified
	 * selection, an empty collection if one could be created but does not exist, or
	 * <code>null</code> if default resource mappings should be used to derive associated
	 * configurations.
	 *
	 * @param selection the current selection
	 * @return an array of existing <code>ILaunchConfiguration</code>s that could be
	 *  used to launch the given selection, an empty array if one could be created
	 *  but does not exist, or <code>null</code> if default resource mappings should
	 *  be used to derive associated configurations
	 */
	ILaunchConfiguration[] getLaunchConfigurations(ISelection selection);

	/**
	 * Returns an array of existing <code>ILaunchConfiguration</code>s that could be
	 * used to launch the given editor part, an empty array if one
	 * could be created but does not exist, or <code>null</code> if default resource
	 * mappings should be used to derive associated configurations
	 *
	 * @param editorpart the current selection
	 * @return an array of existing <code>ILaunchConfiguration</code>s that could be
	 *  used to launch the given editor part/editor input, an empty array if one
	 *  could be created but does not exist, or <code>null</code> if default resource
	 *  mappings should be used to derive associated configurations
	 */
	ILaunchConfiguration[] getLaunchConfigurations(IEditorPart editorpart);

	/**
	 * Returns an <code>IResource</code> that maps to the given selection for launch
	 * purposes, or <code>null</code> if none. The resource is used to resolve a configuration
	 * to launch if this shortcut does not provide specific launch configurations to launch
	 * for the selection (via {@link #getLaunchConfigurations(ISelection)}.
	 *
	 * @param selection the current selection
	 * @return an <code>IResource</code> that maps to the given selection for launch
	 *  purposes or <code>null</code> if none
	 */
	IResource getLaunchableResource(ISelection selection);

	/**
	 * Returns an <code>IResource</code> that maps to given editor part for launch
	 * purposes, or <code>null</code> if none. The resource is used to resolve a configuration
	 * to launch if this shortcut does not provide specific launch configurations to launch
	 * for the editor (via {@link #getLaunchConfigurations(IEditorPart)}.
	 *
	 * @param editorpart the current editor part
	 * @return an <code>IResource</code> that maps to given editor part for launch
	 *  purposes, or <code>null</code> if none
	 */
	IResource getLaunchableResource(IEditorPart editorpart);
}
