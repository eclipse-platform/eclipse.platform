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

import org.eclipse.core.runtime.*;
import org.eclipse.help.*;
import org.w3c.dom.*;

/**
 * Makes help resources adaptable and persistable
 */
public abstract class AdaptableHelpResource
		implements
			IAdaptable,
			IHelpResource {
	protected IHelpResource element;
	protected IAdaptable parent;

	/**
	 * This constructor will be called when wrapping help resources.
	 */
	public AdaptableHelpResource(IHelpResource element) {
		this.element = element;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IHelpResource.class)
			return (T) element;
		return null;
	}

	public abstract void saveState(Element element);

	public abstract AdaptableHelpResource[] getChildren();

	public IAdaptable getParent() {
		return parent;
	}

	public void setParent(IAdaptable parent) {
		this.parent = parent;
	}

	/**
	 * Tests the receiver and the object for equality
	 *
	 * @param object
	 *            object to compare the receiver to
	 * @return true=the object equals the receiver, the name is the same. false
	 *         otherwise
	 */
	@Override
	public boolean equals(Object object) {
		if (this == object)
			return true;
		else if (object instanceof AdaptableHelpResource)
			return (element == ((AdaptableHelpResource) object).element);
		else if (object instanceof IHelpResource)
			return element == object;
		else
			return false;
	}

	/**
	 * Returns the hash code.
	 *
	 * @return the hash code.
	 */
	@Override
	public int hashCode() {
		if (element == null)
			return -1;
		return element.hashCode();
	}

	/**
	 * Returns a descendant topic with a specified href
	 */
	public abstract ITopic getTopic(String href);

	@Override
	public String getHref() {
		return element.getHref();
	}

	@Override
	public String getLabel() {
		return element.getLabel();
	}

}
