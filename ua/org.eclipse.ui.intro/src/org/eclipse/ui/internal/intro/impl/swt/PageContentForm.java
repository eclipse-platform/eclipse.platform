/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledPageBook;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.ui.internal.intro.impl.IIntroConstants;
import org.eclipse.ui.internal.intro.impl.model.AbstractIntroElement;
import org.eclipse.ui.internal.intro.impl.model.AbstractIntroPage;
import org.eclipse.ui.internal.intro.impl.model.IntroModelRoot;
import org.eclipse.ui.intro.config.IIntroContentProviderSite;

/**
 * A Composite that represents the content of an Intro Page. It is swapped in
 * the categories page book in the PageForm class.
 */
public class PageContentForm implements IIntroConstants {

	private FormToolkit toolkit;
	private IntroModelRoot model;
	private PageStyleManager styleManager;
	// composite to control reflow.
	private Composite contentComposite;

	// the page we are modeling here.
	private AbstractIntroPage page;

	// site is cached to hand down to the PageWidgetFactory for creating the UI
	// for content providers..
	private IIntroContentProviderSite site;


	public PageContentForm(FormToolkit toolkit, IntroModelRoot modelRoot) {
		this.toolkit = toolkit;
		this.model = modelRoot;
		page = model.getCurrentPage();
	}

	public PageContentForm(FormToolkit toolkit, IntroModelRoot modelRoot,
			AbstractIntroPage page) {
		this(toolkit, modelRoot);
		this.page = page;
	}


	/**
	 * Create the form for the root page. Number of columns there is equal to
	 * the number of links. Every image link does not cache a model object for
	 * data retrieval..
	 *
	 * @param pageBook
	 */
	public void createPartControl(ScrolledPageBook contentPageBook,
			PageStyleManager pageStyleManager) {
		styleManager = pageStyleManager;

		// categoriesComposite has Table Layout with one col. Holds page
		// description and composite with all other children.
		contentComposite = contentPageBook.createPage(page.getId());
		// Util.highlight(contentComposite, SWT.COLOR_GREEN);
		TableWrapLayout layout = new TableWrapLayout();
		layout.topMargin = 15;
		layout.leftMargin = 15;
		layout.rightMargin = 15;
		layout.bottomMargin = 15;
		layout.verticalSpacing = 15;
		contentComposite.setLayout(layout);

		if (styleManager.getPageDescription() != null) {
			Label label = toolkit.createLabel(contentComposite, styleManager
				.getPageDescription(), SWT.WRAP);
			label.setFont(PageStyleManager.getBannerFont());
			TableWrapData td = new TableWrapData();
			td.align = TableWrapData.FILL;
			label.setLayoutData(td);
		}

		// Store the sub-title data for this composite from this page's
		// subtitle. Make sure you do this before creating the page content to
		// filter out page sub-title from content area.
		contentComposite.setData(PAGE_SUBTITLE, styleManager.getPageSubTitle());

		createPageChildren(page, contentComposite);

		styleManager = null;
	}

	private void createPageChildren(AbstractIntroPage page, Composite parent) {
		// setup page composite/layout
		PageWidgetFactory factory = new PageWidgetFactory(toolkit, styleManager);
		factory.setContentProviderSite(site);
		Composite pageComposite = createPageTableComposite(factory, toolkit, styleManager, parent);
		// now add all children
		AbstractIntroElement[] children = page.getChildren();
		for (int i = 0; i < children.length; i++)
			factory.createIntroElement(pageComposite, children[i]);

	}

	/**
	 * Creates a composite with TableWrapLayout to hold all page children. The
	 * default number of columns is 1.
	 *
	 * @param parent
	 * @return
	 */
	static Composite createPageTableComposite(PageWidgetFactory factory, FormToolkit toolkit,
			PageStyleManager styleManager, Composite parent) {
		Composite client = toolkit.createComposite(parent);
		TableWrapLayout layout = new TableWrapLayout();
		layout.topMargin = 0;
		layout.bottomMargin = 0;
		layout.leftMargin = 0;
		layout.rightMargin = 0;
		int numColumns = styleManager.getPageNumberOfColumns();
		layout.numColumns = numColumns == 0 ? 1 : numColumns;
		layout.horizontalSpacing = styleManager.getPageHorizantalSpacing();
		layout.verticalSpacing = styleManager.getPageVerticalSpacing();
		client.setLayout(layout);

		// parent has TableWrapLayout, and so update layout of this child.
		TableWrapData td = new TableWrapData(TableWrapData.FILL,
			TableWrapData.FILL);
		// td.align = TableWrapData.FILL;
		td.grabHorizontal = true;
		client.setLayoutData(td);
		return client;
	}


	public void setContentProviderSite(IIntroContentProviderSite site) {
		this.site = site;
	}



}
