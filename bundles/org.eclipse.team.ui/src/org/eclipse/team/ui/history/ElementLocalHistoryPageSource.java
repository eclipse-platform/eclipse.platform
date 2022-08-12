/*******************************************************************************
 * Copyright (c) 2006, 2012 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui.history;

import org.eclipse.compare.ITypedElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ui.history.EditionHistoryPage;
import org.eclipse.ui.part.Page;

/**
 * A history page source that can create history pages for a sub-element of a file.
 * @since 3.3
 */
public abstract class ElementLocalHistoryPageSource extends HistoryPageSource {

	/**
	 * Return the previous edition from the local history of the given element located in the given
	 * file. A <code>null</code> is returned if a previous edition could not be found.
	 * @param file the file containing the element
	 * @param element the element
	 * @return the previous edition of the element from the local history or <code>null</code>
	 * @throws TeamException if an error occurs
	 */
	public static ITypedElement getPreviousEdition(IFile file, Object element) throws TeamException {
		return EditionHistoryPage.getPreviousState(file, element);
	}

	/**
	 * Create an instance of the page source.
	 */
	public ElementLocalHistoryPageSource() {
		super();
	}

	@Override
	public final boolean canShowHistoryFor(Object object) {
		return getFile(object) != null;
	}

	@Override
	public final Page createPage(Object object) {
		return new EditionHistoryPage(getFile(object), object);
	}

	/**
	 * Return the file that contains the given element of <code>null</code>
	 * if this page source can not show history for the given element.
	 * @param element the element
	 * @return the file that contains the given element of <code>null</code>
	 */
	protected abstract IFile getFile(Object element);

	/**
	 * Return the file that contains the given element of <code>null</code> if this page source can
	 * not show history for the given element.
	 *
	 * @param element the element
	 * @return the file that contains the given element of <code>null</code>
	 * @noreference This method is not intended to be referenced by clients.
	 */
	final public IFile internalGetFile(Object element) {
		return getFile(element);
	}

}
