/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.debug.ui.sourcelookup;

import org.eclipse.ui.IEditorInput;

/**
 * The result of performing source lookup on a debug artifact.
 * The result contains the resolved source element and description
 * of an editor (editor id, and editor input) in which to display
 * the result.
 * @see org.eclipse.debug.ui.DebugUITools#lookupSource(Object, org.eclipse.debug.core.model.ISourceLocator)
 * @see org.eclipse.debug.ui.DebugUITools#displaySource(ISourceLookupResult, org.eclipse.ui.IWorkbenchPage)
 * @since 3.1
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface ISourceLookupResult {

	/**
	 * Returns the artifact for which source lookup was performed,
	 * such as a stack frame.
	 *
	 * @return the artifact for which source lookup was performed
	 */
	Object getArtifact();

	/**
	 * Returns the source element resolved during source lookup,
	 * or <code>null</code> if a source element was not resolved.
	 *
	 * @return resolved source element or <code>null</code> if unknown
	 */
	Object getSourceElement();

	/**
	 * Returns the identifier of an editor used to display this result,
	 * or <code>null</code> if unknown.
	 *
	 * @return the identifier of an editor used to display this result,
	 * or <code>null</code> if unknown
	 */
	String getEditorId();

	/**
	 * Returns the editor input used to display result,
	 * or <code>null</code> if unknown.
	 *
	 * @return the editor input used to display result,
	 * or <code>null</code> if unknown
	 */
	IEditorInput getEditorInput();
}
