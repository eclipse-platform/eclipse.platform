/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
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
package org.eclipse.ui.internal.console;

import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;

/**
 * When any region of a hyperlink is replaced, the hyperlink needs to be deleted.
 *
 * @since 3.3
 */
public class HyperlinkUpdater extends DefaultPositionUpdater {

	public HyperlinkUpdater() {
		super(ConsoleHyperlinkPosition.HYPER_LINK_CATEGORY);
	}
	/**
	 * When any region of a hyperlink is replaced, the hyperlink needs to be deleted.
	 *
	 * @return <code>true</code> if position has NOT been deleted
	 */
	@Override
	protected boolean notDeleted() {

		int positionEnd = fPosition.offset + fPosition.length - 1;
		int editEnd = fOffset + fLength - 1;
		if ((fOffset <= fPosition.offset && (editEnd > fPosition.offset)) ||
				(fOffset < positionEnd && (editEnd > positionEnd)) ||
				(fOffset >= fPosition.offset && fOffset <= positionEnd) ||
				(editEnd >= fPosition.offset && editEnd <= positionEnd)) {

			fPosition.delete();

			try {
				fDocument.removePosition(ConsoleHyperlinkPosition.HYPER_LINK_CATEGORY, fPosition);
			} catch (BadPositionCategoryException x) {
			}

			return false;
		}

		return true;
	}
}
