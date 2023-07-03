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
 *     Alexander Kurtakov - Bug 460858
 *******************************************************************************/
package org.eclipse.help.internal.workingset;

import org.eclipse.help.*;
import org.w3c.dom.*;

/**
 * Makes help resources adaptable and persistable
 */
public class AdaptableToc extends AdaptableHelpResource {

	protected AdaptableTopic[] children;

	/**
	 * This constructor will be called when wrapping help resources.
	 */
	public AdaptableToc(IToc element) {
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
		if (children == null) {
			ITopic[] topics = ((IToc) element).getTopics();
			children = new AdaptableTopic[topics.length];
			for (int i = 0; i < topics.length; i++) {
				children[i] = new AdaptableTopic(topics[i]);
				children[i].setParent(this);
			}
		}
		return children;
	}

	@Override
	public ITopic getTopic(String href) {
		if(null != href && href.equals(((IToc) element).getTopic(null).getHref())){
			return ((IToc) element).getTopic(null);
		}
		return ((IToc) element).getTopic(href);
	}

	public ITopic[] getTopics() {
		return ((IToc) element).getTopics();
	}

	@Override
	public void saveState(Element element) {
		element.setAttribute("toc", getHref()); //$NON-NLS-1$
	}
}
