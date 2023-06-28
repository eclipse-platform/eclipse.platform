/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.views.variables.details;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.debug.ui.IDetailPane;
import org.eclipse.debug.ui.IDetailPaneFactory;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * The default detail pane factory is contributed to the <code>org.eclipse.debug.ui.detailPaneFactories</code>
 * extension without an enablement expression so it is always loaded.  For any given selection (even
 * null or empty selections), the factory can produce a <code>SourceDetailsPane</code>
 *
 * @since 3.3
 */
public class DefaultDetailPaneFactory implements IDetailPaneFactory {

	public static final String DEFAULT_DETAIL_PANE_ID = DefaultDetailPane.ID;

	@Override
	public IDetailPane createDetailPane(String id) {
		if (MessageDetailPane.ID.equals(id)) {
			return new MessageDetailPane();
		} else {
			return new DefaultDetailPane();
		}
	}

	@Override
	public Set<String> getDetailPaneTypes(IStructuredSelection selection) {
		Set<String> possibleIDs = new HashSet<>(1);
		possibleIDs.add(DefaultDetailPane.ID);
		return possibleIDs;
	}

	@Override
	public String getDefaultDetailPane(IStructuredSelection selection) {
		// Return null so that any contributed detail pane can override the default
		return null;
	}

	@Override
	public String getDetailPaneName(String id) {
		if (id.equals(DefaultDetailPane.ID)){
			return DefaultDetailPane.NAME;
		}
		if (id.equals(MessageDetailPane.ID)) {
			return MessageDetailPane.NAME;
		}
		return null;
	}

	@Override
	public String getDetailPaneDescription(String id) {
		if (id.equals(DefaultDetailPane.ID)){
			return DefaultDetailPane.DESCRIPTION;
		}
		if (id.equals(MessageDetailPane.ID)) {
			return MessageDetailPane.DESCRIPTION;
		}
		return null;
	}

}
