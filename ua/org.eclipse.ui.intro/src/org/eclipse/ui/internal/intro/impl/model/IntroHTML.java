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
import org.eclipse.ui.internal.intro.impl.util.Log;
import org.osgi.framework.Bundle;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * An intro HTML element. Can have text and image as fall back. "type" attribute
 * in markup determines if it is inlined or not. if inlined, value of 'src' will
 * be treated as a snippet of HTML to emit 'in-place'. If 'embed', a valid
 * (full) HTML document will be embedded using HTML 'OBJECT' tag. Ecoding can be
 * specified for inline snippets.
 */
public class IntroHTML extends AbstractTextElement {

	protected static final String TAG_HTML = "html"; //$NON-NLS-1$

	private static final String ATT_SRC = "src"; //$NON-NLS-1$
	/**
	 * type must be "inline" or "embed".
	 */
	private static final String ATT_TYPE = "type"; //$NON-NLS-1$
	// Default is UTF-8.
	private static final String ATT_ENCODING = "encoding"; //$NON-NLS-1$

	private String src;
	private String html_type;
	private String encoding;
	private IntroImage introImage;

	IntroHTML(Element element, Bundle bundle, String base) {
		super(element, bundle);
		src = getAttribute(element, ATT_SRC);
		html_type = getAttribute(element, ATT_TYPE);
		encoding = getAttribute(element, ATT_ENCODING);
		if (encoding == null)
			encoding = "UTF-8"; //$NON-NLS-1$
		if (html_type != null && !html_type.equalsIgnoreCase("inline") //$NON-NLS-1$
				&& !html_type.equalsIgnoreCase("embed")) //$NON-NLS-1$
			// if type is not correct, null it.
			html_type = null;

		// description will be null if there is no description element.
		introImage = getIntroImage(element, base);

		// Resolve.
		src = BundleUtil.getResolvedResourceLocation(base, src, bundle);
	}

	/**
	 * Retruns the intro image element embedded in this element.
	 */
	private IntroImage getIntroImage(Element element, String base) {
		try {
			// There should only be one text element. Since elements where
			// obtained by name, no point validating name.
			NodeList imageElements = element
				.getElementsByTagName(IntroImage.TAG_IMAGE);
			if (imageElements.getLength() == 0)
				// no contributions. done.
				return null;
			IntroImage image = new IntroImage((Element) imageElements.item(0),
				getBundle(), base);
			image.setParent(this);
			return image;
		} catch (Exception e) {
			Log.error(e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Returns the html type. Will be either "inline" or "embed". If not, null
	 * will be returned as if the attibute was nto defined.
	 *
	 * @return Returns the html type value.
	 */
	public boolean isInlined() {
		return (html_type != null && html_type.equalsIgnoreCase("inline")) ? true //$NON-NLS-1$
				: false;
	}

	/**
	 * @return Returns the src.
	 */
	public String getSrc() {
		return src;
	}

	/**
	 * @return Returns the encoding of the inlined file. This is not needed for
	 *         embedded files. Default is UTF-8.
	 */
	public String getInlineEncoding() {
		return encoding;
	}

	/**
	 * Returns the intro image used as a replacement if this HTML element fails.
	 * May return null if there is no image child.
	 *
	 * @return Returns the introImage.
	 */
	public IntroImage getIntroImage() {
		return introImage;
	}

	@Override
	public int getType() {
		return AbstractIntroElement.HTML;
	}

	/**
	 * Deep copy since class has mutable objects.
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		IntroHTML clone = (IntroHTML) super.clone();
		if (introImage != null) {
			IntroImage cloneIntroImage = (IntroImage) introImage.clone();
			cloneIntroImage.setParent(clone);
			clone.introImage = cloneIntroImage;
		}
		return clone;
	}

}
