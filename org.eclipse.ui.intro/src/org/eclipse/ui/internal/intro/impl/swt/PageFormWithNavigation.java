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
package org.eclipse.ui.internal.intro.impl.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledPageBook;
import org.eclipse.ui.internal.intro.impl.model.IntroLink;
import org.eclipse.ui.internal.intro.impl.model.IntroModelRoot;
import org.eclipse.ui.internal.intro.impl.util.ImageUtil;

/**
 * Extends the UI of a PageForm and adds a navigation toolbar UI for the root
 * page links.
 */
public class PageFormWithNavigation extends PageForm {

	private PageStyleManager rootPageStyleManager;

	// Id to this page. There is only a single instance of this page in the
	// main page book.
	public static String PAGE_FORM_WITH_NAVIGATION_ID = "pageFormWithNavigationId"; //$NON-NLS-1$


	/**
	 *
	 */
	public PageFormWithNavigation(FormToolkit toolkit,
			IntroModelRoot modelRoot, Form parentForm) {
		super(toolkit, modelRoot, parentForm);
	}

	/**
	 * Extend parent behavior and add navigation.
	 *
	 * @param pageBook
	 */
	@Override
	public void createPartControl(ScrolledPageBook mainPageBook,
			SharedStyleManager sharedStyleManager) {

		super.createPartControl(mainPageBook, sharedStyleManager);

		// Create a style manager from shared style manager. We only need it
		// for the UI navigation composite.
		rootPageStyleManager = new PageStyleManager(model.getHomePage(),
			sharedStyleManager.getProperties());

		// Now create Navigation bar.
		Composite navigationComposite = toolkit.createComposite(pageForm
			.getBody());
		navigationComposite.setLayoutData(new GridData(
			GridData.HORIZONTAL_ALIGN_CENTER));
		int numberOfLinks = model.getRootPage().getLinks().length;
		GridLayout layout = new GridLayout();
		layout.numColumns = numberOfLinks;
		navigationComposite.setLayout(layout);
		createSmallNavigator(navigationComposite, model.getRootPage()
			.getLinks());

		pageForm.setText(rootPageStyleManager.getPageSubTitle());
	}

	/**
	 * Override parent id.
	 */
	@Override
	protected String getId() {
		return PAGE_FORM_WITH_NAVIGATION_ID;
	}

	private void createSmallNavigator(Composite parent, IntroLink[] links) {
		for (int i = 0; i < links.length; i++) {
			Control c = createImageHyperlink(parent, links[i]);
			c.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
		}
		for (int i = 0; i < links.length; i++) {
			Label text = toolkit.createLabel(parent, links[i].getLabel());
			text.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
			text.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
		}
	}

	/**
	 * Creates an Image Hyperlink from an IntroLink. Model object is NOT cached.
	 *
	 * @param body
	 * @param link
	 */
	private Control createImageHyperlink(Composite body, IntroLink link) {
		ImageHyperlink imageLink = toolkit.createImageHyperlink(body, SWT.NULL);

		// set link image.
		Image image = rootPageStyleManager.getImage(link, "small-link-icon", //$NON-NLS-1$
			ImageUtil.DEFAULT_SMALL_ROOT_LINK);
		imageLink.setImage(image);

		// set link hover image.
		image = rootPageStyleManager.getImage(link, "small-hover-icon", null); //$NON-NLS-1$
		imageLink.setHoverImage(image);
		imageLink.setToolTipText(link.getLabel());
		// each link is centered in cell.
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
		imageLink.setLayoutData(gd);
		imageLink.setHref(link.getUrl());
		imageLink.addHyperlinkListener(hyperlinkAdapter);
		return imageLink;
	}


}
