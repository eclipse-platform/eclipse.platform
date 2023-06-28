/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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


import org.eclipse.ui.IEditorInput;

/**
 * A source presentation is used to resolve an editor in which to display a
 * debug model element, breakpoint, or source element. By default, a debug model
 * presentation (which implements this interface) is used to resolve editors
 * when performing source lookup. However, a source locator may override default
 * editor resolution by implementing this interface.
 * <p>
 * Source lookup consists of the following steps:
 * </p>
 * <ol>
 * <li>Locating a source element - the source locator associated with a launch
 * is queried for the source element associated with a stack frame.</li>
 * <li>Resolving an editor in which to display a source element - by default,
 * the debug model presentation associated with the debug model being debugged
 * is queried for an editor input and editor id in which to display a source
 * element. However, clients may override editor resolution by specifying a
 * source locator that is an instance of <code>ISourcePresentation</code>. When
 * a source presentation is specified as a source locator, the source
 * presentation is used to resolve an editor, rather than the default debug
 * model presentation.</li>
 * </ol>
 *
 * <p>
 * Clients may implement this interface as part of an
 * {@link org.eclipse.debug.ui.IDebugModelPresentation} or as an optional
 * extension to an {@link org.eclipse.debug.core.model.ISourceLocator}.
 * </p>
 *
 * @since 2.0
 */
public interface ISourcePresentation {

	/**
	 * Returns an editor input that should be used to display the given object
	 * in an editor or <code>null</code> if unable to provide an editor input
	 * for the given object.
	 *
	 * @param element a debug model element, breakpoint, or a source element
	 *  that was returned by a source locator's <code>getSourceElement(IStackFrame)</code>
	 *  method
	 * @return an editor input, or <code>null</code> if none
	 */
	IEditorInput getEditorInput(Object element);

	/**
	 * Returns the id of the editor to use to display the
	 * given editor input and object, or <code>null</code> if
	 * unable to provide an editor id.
	 *
	 * @param input an editor input that was previously retrieved from this
	 *    source presentation's <code>getEditorInput</code> method
	 * @param element the object that was used in the call to
	 *  <code>getEditorInput</code>, that corresponds to the given editor
	 *  input
	 * @return an editor id, or <code>null</code> if none
	 */
	String getEditorId(IEditorInput input, Object element);
}
