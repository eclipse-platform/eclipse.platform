/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.help.ui.internal.views;

import org.eclipse.help.IHelpResource;
import org.eclipse.help.search.ISearchEngineResult;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

public class FederatedSearchSorter extends ViewerComparator {
	public FederatedSearchSorter() {
		super(ReusableHelpPart.SHARED_COLLATOR);
	}

	@Override
	public int category(Object element) {
		if (element instanceof ISearchEngineResult) {
			ISearchEngineResult r = (ISearchEngineResult)element;
			IHelpResource c = r.getCategory();
			if (c!=null) {
				String label = c.getLabel();
				if (label.length()==0)
					return 10;
				return 5;
			}
		}
		return super.category(element);
	}


	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		int cat1 = category(e1);
		int cat2 = category(e2);

		if (cat1 != cat2)
			return cat1 - cat2;
		try {
			ISearchEngineResult r1 = (ISearchEngineResult) e1;
			ISearchEngineResult r2 = (ISearchEngineResult) e2;
			IHelpResource c1 = r1.getCategory();
			IHelpResource c2 = r2.getCategory();
			if (c1!=null && c2!=null) {
				int cat = super.compare(viewer, c1.getLabel(), c2.getLabel());
				if (cat!=0) return cat;
			}
			float rank1 = ((ISearchEngineResult) e1).getScore();
			float rank2 = ((ISearchEngineResult) e2).getScore();
			if (rank1 - rank2 > 0) {
				return -1;
			} else if (rank1 == rank2) {
				return 0;
			} else {
				return 1;
			}
		} catch (Exception e) {
		}
		return super.compare(viewer, e1, e2);
	}
}
