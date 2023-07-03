/*******************************************************************************
 * Copyright (c) 2010, 2019 IBM Corporation and others.
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

import org.eclipse.help.ui.internal.search.HelpCriteriaContentProvider.CriterionName;
import org.eclipse.help.ui.internal.search.HelpCriteriaContentProvider.CriterionValue;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

public class HelpCriteriaLabelProvider extends LabelProvider {

	/**
	 * Constructor for HelpWorkingSetElementLabelProvider.
	 */
	public HelpCriteriaLabelProvider() {
		super();
	}

	@Override
	public Image getImage(Object element) {
		return null;
	}

	@Override
	public String getText(Object element) {
		if (element instanceof HelpCriteriaContentProvider.CriterionName) {
			CriterionName criterionName = (HelpCriteriaContentProvider.CriterionName)element;
			return criterionName.getName();
		} else if (element instanceof HelpCriteriaContentProvider.CriterionValue) {
			CriterionValue criterionValue = (HelpCriteriaContentProvider.CriterionValue)element;
			return criterionValue.getName();
		}
		return null;
	}

}
