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
package org.eclipse.compare.examples.xml;

import org.eclipse.jface.action.Action;

class SelectMatcherAction extends Action {

	private XMLStructureViewer fViewer;
	private String fDesc;

	public SelectMatcherAction(String desc, XMLStructureViewer viewer) {
		fViewer= viewer;
		fDesc= desc;
		setText(fDesc);
		setToolTipText(fDesc);
	}

	@Override
	public void run() {
		((XMLStructureCreator) fViewer.getStructureCreator()).setIdMap(fDesc);
		fViewer.contentChanged();
	}
}
