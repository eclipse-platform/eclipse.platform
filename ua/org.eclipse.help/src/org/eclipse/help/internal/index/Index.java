/*******************************************************************************
 * Copyright (c) 2005, 2016 Intel Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Intel Corporation - initial API and implementation
 *     IBM Corporation - 122967 [Help] Remote help system
 *     IBM Corporation - add support for see / see also
 *     IBM Corporation - add support for filtering of the index view
 *******************************************************************************/
package org.eclipse.help.internal.index;

import java.util.Iterator;

import org.eclipse.help.IIndex;
import org.eclipse.help.IIndexEntry;
import org.eclipse.help.internal.UAElement;
import org.w3c.dom.Element;

public class Index extends UAElement implements IIndex {

	public static final String NAME = "index"; //$NON-NLS-1$

	public Index() {
		super(NAME);
	}

	public Index(IIndex src) {
		super(NAME, src);
		appendChildren(src.getChildren());
	}

	public Index(Element src) {
		super(src);
	}

	@Override
	public IIndexEntry[] getEntries() {
		return getChildren(IIndexEntry.class);
	}

	/**
	 * @param see A see element
	 * @return the entry with matching keyword or null
	 */
	public IndexEntry getSeeTarget(IndexSee see) {
		if (children == null) getChildren();
		String keyword = see.getKeyword();
		for (Iterator<UAElement> iter = children.iterator(); iter.hasNext();) {
			UAElement next = iter.next();
			if (next instanceof IndexEntry && keyword.equals(((IndexEntry)next).getKeyword())) {
				return (IndexEntry)next;
			}
		}
		return null;
	}

}
