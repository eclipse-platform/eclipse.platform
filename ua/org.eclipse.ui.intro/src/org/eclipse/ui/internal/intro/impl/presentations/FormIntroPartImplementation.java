/*******************************************************************************
 * Copyright (c) 2004, 2017 IBM Corporation and others.
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
package org.eclipse.ui.internal.intro.impl.presentations;

import org.eclipse.core.runtime.IRegistryChangeEvent;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledPageBook;
import org.eclipse.ui.internal.intro.impl.IntroPlugin;
import org.eclipse.ui.internal.intro.impl.Messages;
import org.eclipse.ui.internal.intro.impl.model.AbstractIntroPage;
import org.eclipse.ui.internal.intro.impl.model.AbstractIntroPartImplementation;
import org.eclipse.ui.internal.intro.impl.model.History;
import org.eclipse.ui.internal.intro.impl.model.IntroModelRoot;
import org.eclipse.ui.internal.intro.impl.model.loader.ContentProviderManager;
import org.eclipse.ui.internal.intro.impl.swt.PageForm;
import org.eclipse.ui.internal.intro.impl.swt.PageFormWithNavigation;
import org.eclipse.ui.internal.intro.impl.swt.PageStyleManager;
import org.eclipse.ui.internal.intro.impl.swt.RootPageForm;
import org.eclipse.ui.internal.intro.impl.swt.SharedStyleManager;
import org.eclipse.ui.internal.intro.impl.util.ImageUtil;
import org.eclipse.ui.internal.intro.impl.util.Util;
import org.eclipse.ui.intro.config.CustomizableIntroPart;
import org.eclipse.ui.intro.config.IIntroContentProvider;
import org.eclipse.ui.intro.config.IIntroContentProviderSite;
import org.eclipse.ui.intro.config.IntroConfigurer;

/**
 * This is a UI Forms based implementation of an Intro Part Presentation.
 */
public class FormIntroPartImplementation extends
		AbstractIntroPartImplementation implements IIntroContentProviderSite,
		IPropertyListener {

	private FormToolkit toolkit;
	private ScrolledPageBook mainPageBook;
	private PageForm pageForm;
	private PageFormWithNavigation pageFormWithNav;
	// cache model instance for reuse.
	private IntroModelRoot model = getModel();
	private SharedStyleManager sharedStyleManager;

	// static SWT Intro. This is the link shown on the center of a page in a
	// static SWT intro.
	private Hyperlink welcomeLink;

	static {
		// REVISIT: register all common images here. Even if this part
		// implementation is created again, the images will remain in plugin
		// registry.
		ImageUtil.registerImage(ImageUtil.DEFAULT_ROOT_LINK, "overview_48.png"); //$NON-NLS-1$
		ImageUtil.registerImage(ImageUtil.DEFAULT_SMALL_ROOT_LINK,
			"overview_32.png"); //$NON-NLS-1$
		ImageUtil.registerImage(ImageUtil.DEFAULT_FORM_BG, "form_banner.png"); //$NON-NLS-1$
		ImageUtil.registerImage(ImageUtil.DEFAULT_LINK, "welcome_item.gif"); //$NON-NLS-1$
	}

	@Override
	protected void updateNavigationActionsState() {
		if (getModel().isDynamic()) {
			forwardAction.setEnabled(history.canNavigateForward());
			backAction.setEnabled(history.canNavigateBackward());
			return;
		}
		// no actions are added in static swt.
	}


	public FormIntroPartImplementation() {
		// Shared style manager
		sharedStyleManager = new SharedStyleManager(getModel());
	}

	@Override
	public void createPartControl(Composite container) {
		if (getModel().isDynamic())
			dynamicCreatePartControl(container);
		else {
			staticCreatePartControl(container);
		}
	}



	/*
	 * create dynamic UI forms Intro, ie: swt intro.
	 */
	private void dynamicCreatePartControl(Composite container) {
		// Create single toolkit instance, which is disposed of on dispose of
		// intro part. also define background of all presentation.
		toolkit = new FormToolkit(container.getDisplay());
		// Define presentation title color
		Color bg = sharedStyleManager.getColor(toolkit, "bg"); //$NON-NLS-1$
		if (bg != null) {
			toolkit.setBackground(bg);
		}
		toolkit.getHyperlinkGroup().setHyperlinkUnderlineMode(
			HyperlinkSettings.UNDERLINE_HOVER);

		// Define presentation title color and image.
		Form mainForm = toolkit.createForm(container);
		Color fg = sharedStyleManager.getColor(toolkit, "title.fg"); //$NON-NLS-1$
		if (fg != null)
			mainForm.setForeground(fg);
		Image bgImage = sharedStyleManager.getImage("title.image", null, null); //$NON-NLS-1$
		if (bgImage != null) {
			mainForm.setBackgroundImage(bgImage);
			String repeat = sharedStyleManager
				.getProperty("title.image.repeat"); //$NON-NLS-1$
			if (repeat != null && repeat.equalsIgnoreCase("true")) //$NON-NLS-1$

				mainForm.setBackgroundImageTiled(true);
		}

		mainPageBook = createMainPageBook(toolkit, mainForm);
		// Add this presentation as a listener to model.
		getModel().addPropertyListener(this);

		addToolBarActions();
	}


	/**
	 * The main page book that holds Intro pages. It has two pages, one that
	 * holds the home page, and one that holds all other pages. If the
	 * presentation is configured to not show the home page with the Home Page
	 * layout, then this page book will only have one page.
	 *
	 * @param toolkit
	 * @param form
	 * @return
	 */
	private ScrolledPageBook createMainPageBook(FormToolkit toolkit, Form form) {
		// get body and create page book in it. Body has GridLayout.
		Composite body = form.getBody();
		body.setLayout(new GridLayout());
		// make sure page book expands h and v.
		ScrolledPageBook pageBook = toolkit.createPageBook(body, SWT.V_SCROLL
				| SWT.H_SCROLL);
		pageBook.setLayoutData(new GridData(GridData.FILL_BOTH));

		// Create root page in root page layout form, only if needed.
		if (sharedStyleManager.useCustomHomePagelayout()) {
			// if we do not have a root page form, create one
			RootPageForm rootPageForm = new RootPageForm(toolkit, model, form);
			rootPageForm.createPartControl(pageBook, sharedStyleManager);
			rootPageForm.setContentProviderSite(this);
		}

		// Create the two Page forms .
		pageForm = new PageForm(toolkit, model, form);
		pageForm.setContentProviderSite(this);
		pageForm.createPartControl(pageBook, sharedStyleManager);

		pageFormWithNav = new PageFormWithNavigation(toolkit, model, form);
		pageFormWithNav.setContentProviderSite(this);
		pageFormWithNav.createPartControl(pageBook, sharedStyleManager);

		// now determine which page to show. Show it and add it to history.
		// if the cached page is a URL ignore it. We do not want to launch a
		// browser on startup.
		String cachedPage = getCachedCurrentPage();
		if (cachedPage != null && !History.isURL(cachedPage)) {
			// this will create the page in the page form.
			model.setCurrentPageId(cachedPage);
		}

		AbstractIntroPage pageToShow = getModel().getCurrentPage();
		// load style manager here to test for navigation.
		PageStyleManager styleManager = new PageStyleManager(pageToShow,
			sharedStyleManager.getProperties());
		boolean pageHasNavigation = styleManager.showHomePageNavigation();
		if (pageToShow != null) {
			if (pageBook.hasPage(pageToShow.getId()))
				// we are showing Home Page.
				pageBook.showPage(pageToShow.getId());
			else {
				if (pageHasNavigation) {
					// page or Home Page with a page layout and navigation, set
					// the page id to the static PageFormWithNavigation id.
					// first create the correct content.
					pageFormWithNav.showPage(pageToShow, sharedStyleManager);
					// then show the page
					pageBook
						.showPage(PageFormWithNavigation.PAGE_FORM_WITH_NAVIGATION_ID);
				} else {
					// page or Home Page with a regular page layout, set the
					// page id to the static PageForm id. first create the
					// correct content.
					pageForm.showPage(pageToShow, sharedStyleManager);
					// then show the page
					pageBook.showPage(PageForm.PAGE_FORM_ID);
				}
			}
			updateHistory(pageToShow);
		}

		return pageBook;
	}

	@Override
	public void dispose() {
		if (toolkit != null)
			toolkit.dispose();
	}

	/**
	 * Handle model property changes. The UI is notified here of a change to the
	 * current page in the model. This happens if an intro URL showPage method
	 * is executed.
	 *
	 * @see org.eclipse.ui.IPropertyListener#propertyChanged(java.lang.Object,
	 *      int)
	 */
	@Override
	public void propertyChanged(Object source, int propId) {
		if (propId == IntroModelRoot.CURRENT_PAGE_PROPERTY_ID) {
			String pageId = getModel().getCurrentPageId();
			if (pageId == null || pageId.isEmpty())
				// If page ID was not set properly. exit.
				return;

			showPage(getModel().getCurrentPage());
		}
	}

	@Override
	protected void addToolBarActions() {
		// Handle menus:
		IActionBars actionBars = getIntroPart().getIntroSite().getActionBars();
		IToolBarManager toolBarManager = actionBars.getToolBarManager();
		actionBars.setGlobalActionHandler(ActionFactory.FORWARD.getId(),
			forwardAction);
		actionBars.setGlobalActionHandler(ActionFactory.BACK.getId(),
			backAction);
		toolBarManager.add(new Separator(IntroConfigurer.TB_ADDITIONS));
		toolBarManager.add(homeAction);
		toolBarManager.add(backAction);
		toolBarManager.add(forwardAction);
		if (IntroPlugin.DEBUG_TOOLBAR) {
			toolBarManager.add(viewIntroModelAction);
		}
		toolBarManager.update(true);
		actionBars.updateActionBars();
		updateNavigationActionsState();
	}

	@Override
	protected void doStandbyStateChanged(boolean standby,
			boolean isStandbyPartNeeded) {
		if (getModel().isDynamic())
			dynamicStandbyStateChanged(standby, isStandbyPartNeeded);
		else
			staticStandbyStateChanged(standby);
	}


	public void dynamicStandbyStateChanged(boolean standby,
			boolean isStandbyPartNeeded) {
		// handle action enablement first
		if (isStandbyPartNeeded || standby) {
			homeAction.setEnabled(false);
			forwardAction.setEnabled(false);
			backAction.setEnabled(false);
		} else {
			homeAction.setEnabled(true);
			updateNavigationActionsState();
		}

		if (isStandbyPartNeeded)
			// we have a standby part, nothing more to do in presentation.
			return;

		// try to show a cached page.
		AbstractIntroPage pageToShow = null;
		if (standby) {
			// we are in standby. Show standby page, in PageForm.
			pageToShow = getModel().getStandbyPage();
			if (pageToShow == null)
				pageToShow = getModel().getHomePage();
		} else
			// if we are showing a regular intro page, or if the Home Page
			// has a regular page layout, set the page id to the static PageForm
			// id.
			pageToShow = getModel().getCurrentPage();

		showPage(pageToShow);
	}

	private boolean showPage(AbstractIntroPage pageToShow) {
		boolean pageisCached = showCachedPage(pageToShow);

		if (!pageToShow.isDynamic()) {
			Util.openBrowser(pageToShow.getUrl());
			return true;
		}

		if (!pageisCached) {
			// page has not been shown before.
			// load style manager here to test for navigation.
			PageStyleManager styleManager = new PageStyleManager(pageToShow,
				sharedStyleManager.getProperties());
			boolean pageHasNavigation = styleManager.showHomePageNavigation();
			if (pageHasNavigation) {
				// page or Home Page with a regular page layout, set the
				// page id to the static PageFormWithNavigation id. first
				// create the correct content.
				pageFormWithNav.showPage(pageToShow, sharedStyleManager);
				// then show the page
				mainPageBook
					.showPage(PageFormWithNavigation.PAGE_FORM_WITH_NAVIGATION_ID);
			} else {
				// page or Home Page with a regular page layout, set the
				// page id to the static PageFormWithNavigation id. first
				// create the correct content.
				pageForm.showPage(pageToShow, sharedStyleManager);
				// then show the page
				mainPageBook.showPage(PageForm.PAGE_FORM_ID);
			}
		}

		return true;
	}

	private boolean showCachedPage(AbstractIntroPage page) {
		String formPageId = null;
		if (pageForm.hasPage(page.getId())) {
			pageForm.showPage(page, sharedStyleManager);
			formPageId = PageForm.PAGE_FORM_ID;
		} else if (pageFormWithNav.hasPage(page.getId())) {
			pageFormWithNav.showPage(page, sharedStyleManager);
			formPageId = PageFormWithNavigation.PAGE_FORM_WITH_NAVIGATION_ID;
		} else if (mainPageBook.hasPage(page.getId()))
			formPageId = page.getId();
		else
			return false;

		mainPageBook.showPage(formPageId);
		return true;
	}

	private void removeCachedPage(AbstractIntroPage page) {
		if (pageForm.hasPage(page.getId()))
			pageForm.removePage(page.getId());
		else if (pageFormWithNav.hasPage(page.getId()))
			pageFormWithNav.removePage(page.getId());
		else if (mainPageBook.hasPage(page.getId()))
			mainPageBook.removePage(page.getId());
		else
			return;
	}


	/**
	 * Clear page cache for the page that contains this provider. Remove the
	 * form from the correct pagebook that refers to the page we need to
	 * refresh. This will force a call to createContents on all content
	 * providers the next time this page needs to be displayed.
	 *
	 * @see org.eclipse.ui.intro.config.IIntroContentProviderSite#reflow(org.eclipse.ui.intro.config.IIntroContentProvider,
	 *      boolean)
	 */
	@Override
	public void reflow(IIntroContentProvider provider, boolean incremental) {
		AbstractIntroPage page = ContentProviderManager.getInst()
			.getContentProviderParentPage(provider);
		if (incremental) {
			if (pageForm.hasPage(page.getId()))
				pageForm.reflow();
			else if (pageFormWithNav.hasPage(page.getId()))
				pageFormWithNav.reflow();
			else if (mainPageBook.hasPage(page.getId()))
				mainPageBook.reflow(true);
		}
		else {
			removeCachedPage(page);
			showPage(model.getCurrentPage());
		}
	}

	@Override
	public void setFocus() {
		if (model.isDynamic()) {
			if (mainPageBook.getCurrentPage() != null)
				mainPageBook.getCurrentPage().setFocus();
		}
	}

	@Override
	public boolean navigateBackward() {
		boolean success = false;
		if (getModel().isDynamic()) {
			// dynamic case. Uses navigation history.
			if (history.canNavigateBackward()) {
				history.navigateHistoryBackward();
				if (history.currentLocationIsUrl())
					success = Util.openBrowser(history
						.getCurrentLocationAsUrl());
				else {
					// Set current page, and this will triger regen.
					CustomizableIntroPart currentIntroPart = (CustomizableIntroPart) IntroPlugin
						.getIntro();
					currentIntroPart.getControl().setRedraw(false);
					success = getModel().setCurrentPageId(
						history.getCurrentLocationAsPage().getId());
					currentIntroPart.getControl().setRedraw(true);
				}
			}
		}

		updateNavigationActionsState();
		return success;
	}

	@Override
	public boolean navigateForward() {
		boolean success = false;

		if (getModel().isDynamic()) {
			// dynamic case. Uses navigation history.
			if (history.canNavigateForward()) {
				history.navigateHistoryForward();
				if (history.currentLocationIsUrl())
					success = Util.openBrowser(history
						.getCurrentLocationAsUrl());
				else {
					// Set current page, and this will triger regen.
					CustomizableIntroPart currentIntroPart = (CustomizableIntroPart) IntroPlugin
						.getIntro();
					currentIntroPart.getControl().setRedraw(false);
					success = getModel().setCurrentPageId(
						history.getCurrentLocationAsPage().getId());
					currentIntroPart.getControl().setRedraw(true);
				}
			}
		}
		updateNavigationActionsState();
		return success;
	}

	@Override
	public boolean navigateHome() {
		AbstractIntroPage homePage = getModel().getHomePage();
		if (getModel().isDynamic()) {
			CustomizableIntroPart currentIntroPart = (CustomizableIntroPart) IntroPlugin
				.getIntro();
			currentIntroPart.getControl().setRedraw(false);
			boolean success = false;
			success = getModel().setCurrentPageId(homePage.getId());
			updateHistory(homePage);
			currentIntroPart.getControl().setRedraw(true);
			return success;
		}
		// static model. Nothing to do.
		return false;
	}

	@Override
	protected void handleRegistryChanged(IRegistryChangeEvent event) {
		if (getModel().isDynamic()) {
			IntroPlugin.closeIntro();
			IntroPlugin.showIntro(false);
		}
	}



	// *********** Static case ******************
	/*
	 * create static UI forms Intro. For this, we only launch the url of the
	 * root page.
	 */
	private void staticCreatePartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		toolkit.getHyperlinkGroup().setHyperlinkUnderlineMode(
			HyperlinkSettings.UNDERLINE_HOVER);

		// create a page that has only one link. The URL and tooltip will be set
		// by the standby listener.
		welcomeLink = createStaticPage(parent);
	}


	private Hyperlink createStaticPage(Composite parent) {
		Form mainForm = toolkit.createForm(parent);
		Composite body = mainForm.getBody();

		GridLayout gl = new GridLayout();
		body.setLayout(gl);
		String label = Messages.StaticHTML_welcome;
		Hyperlink link = toolkit.createHyperlink(body, label, SWT.WRAP);
		link.setFont(PageStyleManager.getHeaderFont());
		GridData gd = new GridData(GridData.GRAB_HORIZONTAL
				| GridData.GRAB_VERTICAL);
		gd.horizontalAlignment = GridData.CENTER;
		gd.verticalAlignment = GridData.CENTER;
		link.setLayoutData(gd);
		link.addHyperlinkListener(new HyperlinkAdapter() {

			@Override
			public void linkActivated(HyperlinkEvent e) {
				Hyperlink link = (Hyperlink) e.getSource();
				Util.openBrowser((String) link.getHref());
				return;
			}
		});

		return link;
	}

	public void staticStandbyStateChanged(boolean standby) {
		AbstractIntroPage homePage = getModel().getHomePage();
		AbstractIntroPage standbyPage = getModel().getStandbyPage();
		if (standbyPage == null)
			standbyPage = homePage;

		if (standby) {
			welcomeLink.setHref(standbyPage.getUrl());
			welcomeLink.setToolTipText(standbyPage.getUrl());
		} else {
			welcomeLink.setHref(homePage.getUrl());
			welcomeLink.setToolTipText(homePage.getUrl());
		}
	}


}
