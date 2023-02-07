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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledPageBook;
import org.eclipse.ui.internal.intro.impl.IIntroConstants;
import org.eclipse.ui.internal.intro.impl.Messages;
import org.eclipse.ui.internal.intro.impl.model.AbstractIntroPage;
import org.eclipse.ui.internal.intro.impl.model.IntroModelRoot;
import org.eclipse.ui.internal.intro.impl.model.url.IntroURLParser;
import org.eclipse.ui.internal.intro.impl.util.DialogUtil;
import org.eclipse.ui.internal.intro.impl.util.Util;
import org.eclipse.ui.intro.config.IIntroContentProviderSite;

/**
 * A Form that represents an Intro Page. It is swapped in the main page book in
 * the FormIntroPartImplementation class. It has a page book for swapping in
 * categories (content) of Intro Pages.
 */
public class PageForm implements IIntroConstants {

	protected FormToolkit toolkit;
	private ScrolledPageBook categoryPageBook;
	protected IntroModelRoot model;
	private Form parentForm;
	protected Form pageForm;
	// private SharedStyleManager sharedStyleManager;

	// Id to this page. There is only a single instance of this page in the
	// main page book.
	public static String PAGE_FORM_ID = "pageFormId"; //$NON-NLS-1$

	// site is cached to hand down to the PageWidgetFactory for creating the UI
	// for content providers..
	private IIntroContentProviderSite site;

	protected HyperlinkAdapter hyperlinkAdapter = new HyperlinkAdapter() {

		@Override
		public void linkActivated(HyperlinkEvent e) {
			String url = (String) e.getHref();
			IntroURLParser parser = new IntroURLParser(url);
			if (parser.hasIntroUrl()) {
				// execute the action embedded in the IntroURL
				parser.getIntroURL().execute();
				return;
			} else if (parser.hasProtocol()) {
				Util.openBrowser(url);
				return;
			}
			DialogUtil.displayInfoMessage(((Control) e.getSource()).getShell(),
				Messages.HyperlinkAdapter_urlIs + " " + url); //$NON-NLS-1$
		}

		@Override
		public void linkEntered(HyperlinkEvent e) {
		}

		@Override
		public void linkExited(HyperlinkEvent e) {
		}
	};

	/**
	 *
	 */
	public PageForm(FormToolkit toolkit, IntroModelRoot modelRoot,
			Form parentForm) {
		this.toolkit = toolkit;
		this.model = modelRoot;
		this.parentForm = parentForm;
	}

	/**
	 * Create a Form for holding pages without navigation.
	 *
	 * @param pageBook
	 */
	public void createPartControl(ScrolledPageBook mainPageBook,
			SharedStyleManager sharedStyleManager) {

		// Cash the shared style manager. We need to pass it around to category
		// forms. So, do not null it!
		// this.sharedStyleManager = sharedStyleManager;

		// creating page in Main page book.
		pageForm = toolkit.createForm(mainPageBook.getContainer());
		mainPageBook.registerPage(getId(), pageForm);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		pageForm.getBody().setLayout(layout);
		// Util.highlight(pageForm.getBody(), SWT.COLOR_RED);

		// Get form body. Form body is one column grid layout. Add page book
		// and navigation UI to it.
		categoryPageBook = toolkit.createPageBook(pageForm.getBody(),
			SWT.H_SCROLL | SWT.V_SCROLL);
		categoryPageBook.setLayoutData(new GridData(GridData.FILL_BOTH));

		// pageForm.setText(rootPageStyleManager.getPageSubTitle());
	}


	protected String getId() {
		return PAGE_FORM_ID;
	}



	/**
	 * This method is called when the current page changes. It creates the
	 * PageContentForm if necessary, and handles showing the page in the Page
	 * Book. It creates a model PageContentForm for the current page.
	 *
	 * @param pageID
	 */
	public void showPage(AbstractIntroPage page,
			SharedStyleManager sharedStyleManager) {

		if (!categoryPageBook.hasPage(page.getId())) {
			// if we do not have a category form for this page create one.
			PageContentForm categoryForm = new PageContentForm(toolkit, model,
				page);
			categoryForm.setContentProviderSite(site);
			// load style manager only once, here.
			PageStyleManager styleManager = new PageStyleManager(page,
				sharedStyleManager.getProperties());
			categoryForm.createPartControl(categoryPageBook, styleManager);
		}
		categoryPageBook.showPage(page.getId());

		// Get cached page subtitle from control data.
		Composite pageComposite = (Composite) categoryPageBook.getCurrentPage();
		// update main Form title.
		parentForm.setText(model.getCurrentPage().getTitle());
		// update this page form's title, ie: Page subtitle, if it exists.
		pageForm.setText((String) pageComposite.getData(PAGE_SUBTITLE));

		// TODO need to transfer focus to the first link in
		// the page somehow; we may need IIntroPage interface with
		// a few methods like 'setFocus()' etc.
		// DG
	}

	public void reflow() {
		categoryPageBook.reflow(true);
	}

	public boolean hasPage(String pageId) {
		return categoryPageBook.hasPage(pageId);
	}

	public void removePage(String pageId) {
		categoryPageBook.removePage(pageId);
	}

	public void setContentProviderSite(IIntroContentProviderSite site) {
		this.site = site;
	}


}
