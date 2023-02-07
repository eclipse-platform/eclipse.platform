/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
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
 *     Alexander Kurtakov - Bug 460858
 *******************************************************************************/
package org.eclipse.help.internal.workingset;

import org.eclipse.help.IToc;
import org.eclipse.help.ITopic;
import org.w3c.dom.Element;

/**
 * Makes help resources adaptable and persistable
 */
public class AdaptableSelectedToc extends AdaptableHelpResource {

	/**
	 * This constructor will be called when wrapping help resources.
	 */
	public AdaptableSelectedToc(IToc element) {
		super(element);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IToc.class)
			return (T) element;
		return super.getAdapter(adapter);
	}

	@Override
	public AdaptableHelpResource[] getChildren() {
		return new AdaptableHelpResource[0];
	}


	/**
	 * When href is exactly the href of the selected toc, then return the selected topic
	 * Otherwise, return null
	 *
	 * @param href
	 *            The topic's href value.
	 */
	@Override
	public ITopic getTopic(String href) {
		if(null == href){
			return null;
		}
		if(href.equals(((IToc) element).getTopic(null).getHref())){
			return ((IToc) element).getTopic(null);
		}
		return null;
	}

	@Override
	public void saveState(Element element) {

	}
}
