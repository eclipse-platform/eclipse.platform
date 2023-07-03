/*******************************************************************************
 * Copyright (c) 2004, 2016 IBM Corporation and others.
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

package org.eclipse.ui.internal.intro.impl.model;

import org.eclipse.ui.internal.intro.impl.model.util.BundleUtil;
import org.osgi.framework.Bundle;
import org.w3c.dom.Element;

/**
 * An intro image element.
 */
public class IntroImage extends AbstractBaseIntroElement {

	protected static final String TAG_IMAGE = "img"; //$NON-NLS-1$

	private static final String ATT_SRC = "src"; //$NON-NLS-1$
	private static final String ATT_ALT = "alt"; //$NON-NLS-1$
	private static final String ATT_TITLE = "title"; //$NON-NLS-1$

	private Element element;
	private String src;
	private String base;

	IntroImage(Element element, Bundle bundle, String base) {
		super(element, bundle);
		this.element = element;
		this.base = base;
	}

	/**
	 * @return Returns the alt.
	 */
	public String getAlt() {
		return getAttribute(element, ATT_ALT);
	}

	/**
	 * @return Returns the title.
	 */
	public String getTitle() {
		return getAttribute(element, ATT_TITLE);
	}

	@Override
	public int getType() {
		return AbstractIntroElement.IMAGE;
	}

	/**
	 * @return Returns the src value, already resolved as a local url.
	 */
	public String getSrc() {
		if (src==null)// Resolve src.
			src = BundleUtil.getResolvedResourceLocation(base, getSrcAsIs(), getBundle());
		return src;
	}

	/**
	 * @return Returns the src value, as is, without resolving it as a local
	 *         url. The src value is relative to the parent plugin.
	 */
	public String getSrcAsIs() {
		return getAttribute(element, ATT_SRC);
	}
}
