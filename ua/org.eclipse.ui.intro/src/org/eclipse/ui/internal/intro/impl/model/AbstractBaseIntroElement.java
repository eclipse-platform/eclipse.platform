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

import org.eclipse.core.runtime.IConfigurationElement;
import org.osgi.framework.Bundle;
import org.w3c.dom.Element;


/**
 * An Intro Config component that has an id attribute and a style-id attribute.
 * It also has the notion of filtering. Only model elements that have meaning in
 * the UI, and that are targettable can be filtered. HEAD, for example, only
 * applied to HTML presentation, and so, it does not need filtering. When model
 * is first loaded, all elements are not filtered. When the UI is created, and
 * we know which presentation we are using, the filter state is computed
 * dynamically. Also, in the SWT presentation, when style manager picks
 * elements, it may alter the filter state of a given element to prevent it from
 * appearing twice in the ui. eg: title appearing as a title of the form, and as
 * a title child text.
 */
public abstract class AbstractBaseIntroElement extends AbstractIntroIdElement {

	protected static final String ATT_STYLE_ID = "style-id"; //$NON-NLS-1$
	protected static final String ATT_FILTERED_FROM = "filteredFrom"; //$NON-NLS-1$

	protected String style_id;
	protected String filteredFrom;
	private boolean isFiltered;

	AbstractBaseIntroElement(IConfigurationElement element) {
		super(element);
		style_id = element.getAttribute(ATT_STYLE_ID);
		filteredFrom = element.getAttribute(ATT_FILTERED_FROM);
	}

	AbstractBaseIntroElement(Element element, Bundle bundle) {
		super(element, bundle);
		style_id = getAttribute(element, ATT_STYLE_ID);
		filteredFrom = getAttribute(element, ATT_FILTERED_FROM);
	}

	/**
	 * Filter this element out based on the presentation kind.
	 *
	 */
	private boolean checkFilterState() {
		if (this.isOfType(AbstractIntroElement.MODEL_ROOT))
			// root element is not filtered.
			return false;
		IntroModelRoot root = (IntroModelRoot) getParentPage().getParent();
		return root.getPresentation().getImplementationKind().equals(
			filteredFrom) ? true : false;
	}


	/**
	 * @return Returns the class id.
	 */
	public String getStyleId() {
		return style_id;
	}

	@Override
	protected void loadFromParent() {
		style_id = getAttribute(getElement(), ATT_STYLE_ID);
		filteredFrom = getAttribute(getElement(), ATT_FILTERED_FROM);
	}

	/**
	 * @return Returns the filter_kind.
	 */
	public String getFilteredFrom() {
		return filteredFrom;
	}

	/**
	 * Return the filter state of this intro element. We need to do this when
	 * this element has been added to the model, and it has a parent. Also, this
	 * method will not be valid if the UI has not been loaded yet because it it
	 * the creation of the UI that determines the presentation details.
	 *
	 * @return Returns the isFiltered.
	 */
	public boolean isFiltered() {
		return checkFilterState() || isFiltered;
	}

	public void setFilterState(boolean state) {
		isFiltered = state;
	}



}
