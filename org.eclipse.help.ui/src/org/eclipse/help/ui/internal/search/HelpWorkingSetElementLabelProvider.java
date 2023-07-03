/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
package org.eclipse.help.ui.internal.search;

import org.eclipse.help.internal.workingset.AdaptableToc;
import org.eclipse.help.internal.workingset.AdaptableTopic;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

public class HelpWorkingSetElementLabelProvider extends LabelProvider {

	/**
	 * Constructor for HelpWorkingSetElementLabelProvider.
	 */
	public HelpWorkingSetElementLabelProvider() {
		super();
	}

	@Override
	public Image getImage(Object element) {
		return null;
	}

	@Override
	public String getText(Object element) {
		if (element instanceof AdaptableToc)
			return ((AdaptableToc) element).getLabel();
		else if (element instanceof AdaptableTopic)
			return ((AdaptableTopic) element).getLabel();
		else
			return null;
	}

}
