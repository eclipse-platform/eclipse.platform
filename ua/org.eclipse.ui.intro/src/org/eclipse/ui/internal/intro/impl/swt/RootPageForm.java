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
package org.eclipse.ui.internal.intro.impl.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledPageBook;
import org.eclipse.ui.internal.intro.impl.IIntroConstants;
import org.eclipse.ui.internal.intro.impl.Messages;
import org.eclipse.ui.internal.intro.impl.model.AbstractBaseIntroElement;
import org.eclipse.ui.internal.intro.impl.model.AbstractIntroElement;
import org.eclipse.ui.internal.intro.impl.model.IntroContentProvider;
import org.eclipse.ui.internal.intro.impl.model.IntroGroup;
import org.eclipse.ui.internal.intro.impl.model.IntroHomePage;
import org.eclipse.ui.internal.intro.impl.model.IntroLink;
import org.eclipse.ui.internal.intro.impl.model.IntroModelRoot;
import org.eclipse.ui.internal.intro.impl.model.url.IntroURLParser;
import org.eclipse.ui.internal.intro.impl.util.DialogUtil;
import org.eclipse.ui.internal.intro.impl.util.ImageUtil;
import org.eclipse.ui.internal.intro.impl.util.StringUtil;
import org.eclipse.ui.internal.intro.impl.util.Util;
import org.eclipse.ui.intro.config.IIntroContentProviderSite;

/**
 * A Composite that represents the Root Page. It is swapped in the main page
 * book in the FormIntroPartImplementation class.
 */
public class RootPageForm implements IIntroConstants {

	private FormToolkit toolkit;
	private IntroHomePage rootPage;
	private Form parentForm;
	protected Label descriptionLabel;
	private PageStyleManager rootPageStyleManager;
	private IIntroContentProviderSite site;
	private PageWidgetFactory factory;

	class PageComposite extends Composite {

		public PageComposite(Composite parent, int style) {
			super(parent, style);
		}

		// Do not allow composite to take wHint as-is - layout manager
		// can reject the hint and compute larger width.
		@Override
		public Point computeSize(int wHint, int hHint, boolean changed) {
			return ((RootPageLayout) getLayout()).computeSize(this, wHint,
				hHint, changed);
		}
	}

	static class RootPageLayout extends Layout {

		// gap between link composite and description label.
		private int VERTICAL_SPACING = 20;

		private int LABEL_MARGIN_WIDTH = 5;

		/*
		 * Custom layout for Root Page Composite.
		 */
		@Override
		protected Point computeSize(Composite composite, int wHint, int hHint,
				boolean flushCache) {
			int innerWHint = wHint;
			if (wHint != SWT.DEFAULT)
				innerWHint -= LABEL_MARGIN_WIDTH + LABEL_MARGIN_WIDTH;
			Control[] children = composite.getChildren();
			Point s1 = children[0].computeSize(SWT.DEFAULT, SWT.DEFAULT);
			Point s2 = children[1].computeSize(innerWHint, SWT.DEFAULT);
			s2.x += LABEL_MARGIN_WIDTH;
			int height = 2 * (s2.y + VERTICAL_SPACING + s1.y / 2);
			Point size = new Point(Math.max(s1.x, s2.x), height + 5);
			return size;
		}

		@Override
		protected void layout(Composite composite, boolean flushCache) {
			Control[] children = composite.getChildren();
			Rectangle carea = composite.getClientArea();
			Control content = children[0];
			Control label = children[1];
			Point contentSize = content.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			Point labelSize = label.computeSize(carea.width - 2
					- LABEL_MARGIN_WIDTH * 2, SWT.DEFAULT);
			content.setBounds(carea.width / 2 - contentSize.x / 2, carea.height
					/ 2 - contentSize.y / 2, contentSize.x, contentSize.y);
			label.setBounds(LABEL_MARGIN_WIDTH, content.getLocation().y
					+ contentSize.y + VERTICAL_SPACING, carea.width
					- LABEL_MARGIN_WIDTH * 2, labelSize.y);
		}
	}

	private HyperlinkAdapter hyperlinkAdapter = new HyperlinkAdapter() {

		@Override
		public void linkActivated(HyperlinkEvent e) {
			ImageHyperlink imageLink = (ImageHyperlink) e.getSource();
			IntroLink introLink = (IntroLink) imageLink.getData(INTRO_LINK);
			IntroURLParser parser = new IntroURLParser(introLink.getUrl());
			if (parser.hasIntroUrl()) {
				// execute the action embedded in the IntroURL
				parser.getIntroURL().execute();
				return;
			} else if (parser.hasProtocol()) {
				Util.openBrowser(introLink.getUrl());
				return;
			}
			DialogUtil.displayInfoMessage(imageLink.getShell(),
				Messages.HyperlinkAdapter_urlIs + introLink.getUrl());
		}

		@Override
		public void linkEntered(HyperlinkEvent e) {
			ImageHyperlink imageLink = (ImageHyperlink) e.getSource();
			IntroLink introLink = (IntroLink) imageLink.getData(INTRO_LINK);
			updateDescription(introLink.getText());
		}

		@Override
		public void linkExited(HyperlinkEvent e) {
			// empty text on exit.
			updateDescription(""); //$NON-NLS-1$
		}

		private void updateDescription(String text) {
			if (text == null)
				text = ""; //$NON-NLS-1$
			descriptionLabel.setText(text);
			descriptionLabel.getParent().layout();
		}
	};

	/**
	 *
	 */
	public RootPageForm(FormToolkit toolkit, IntroModelRoot modelRoot,
			Form parentForm) {
		this.toolkit = toolkit;
		this.rootPage = modelRoot.getRootPage();
		this.parentForm = parentForm;
	}

	/**
	 * Create the form for the root page. Number of columns there is equal to
	 * the number of links.
	 *
	 * @param pageBook
	 */
	public void createPartControl(ScrolledPageBook mainPageBook,
			SharedStyleManager shardStyleManager) {
		// first, create the root page style manager from shared style manager.
		rootPageStyleManager = new PageStyleManager(rootPage, shardStyleManager
			.getProperties());

		// Set title of Main form from root page title.
		parentForm.setText(rootPage.getTitle());

		// Composite for full root page. It has custom layout, and two
		// children: the content composite and the description label.
		Composite rootPageComposite = new PageComposite(mainPageBook
			.getContainer(), SWT.NULL);
		toolkit.adapt(rootPageComposite);

		mainPageBook.registerPage(rootPage.getId(), rootPageComposite);
		rootPageComposite.setLayout(new RootPageLayout());
		// Util.highlight(pageComposite, SWT.COLOR_DARK_CYAN);

		// create the contents composite in the center of the root page.
		createRootPageContent(rootPageComposite);

		// create description label for links description.
		descriptionLabel = createHoverLabel(rootPageComposite);

		// Clear memory. No need for style manager any more.
		rootPageStyleManager = null;
	}

	/**
	 * Creates content of the root page.
	 */
	private void createRootPageContent(Composite rootPageComposite) {
		// setup page composite/layout
		Composite contentComposite = toolkit.createComposite(rootPageComposite);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
		contentComposite.setLayoutData(gd);
		AbstractIntroElement[] children = (AbstractIntroElement[]) rootPage
			.getChildrenOfType(AbstractIntroElement.GROUP
					| AbstractIntroElement.LINK | AbstractIntroElement.CONTENT_PROVIDER);
		int numChildren = children.length;
		GridLayout layout = new GridLayout();
		// separate links a bit more.
		layout.horizontalSpacing = rootPageStyleManager
			.getPageHorizantalSpacing();
		layout.verticalSpacing = rootPageStyleManager.getPageVerticalSpacing();
		// set number of columns.
		int numColumns = rootPageStyleManager.getPageNumberOfColumns();
		numColumns = numColumns == 0 ? numChildren : numColumns;
		layout.numColumns = numColumns;
		layout.horizontalSpacing = rootPageStyleManager
			.getPageHorizantalSpacing();
		layout.verticalSpacing = rootPageStyleManager.getPageVerticalSpacing();
		contentComposite.setLayout(layout);
		for (int i = 0; i < children.length; i++) {
			if (((AbstractBaseIntroElement) children[i]).isFiltered())
				continue;
			if (children[i].getType() == AbstractIntroElement.GROUP)
				createGroupContent(contentComposite, (IntroGroup) children[i]);
			else if (children[i].getType() == AbstractIntroElement.LINK)
				createImageHyperlink(contentComposite, (IntroLink) children[i]);
			else if (children[i].getType() == AbstractIntroElement.CONTENT_PROVIDER)
				createContentProvider(contentComposite, (IntroContentProvider)children[i]);
		}
	}

	/**
	 * Creates content of the root page.
	 */
	private void createGroupContent(Composite parent, IntroGroup group) {
		AbstractIntroElement[] children = (AbstractIntroElement[]) group
			.getChildrenOfType(AbstractIntroElement.GROUP
					| AbstractIntroElement.LINK |
					AbstractIntroElement.CONTENT_PROVIDER);
		int numChildren = children.length;
		// setup page composite/layout
		Composite contentComposite = toolkit.createComposite(parent);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
		gd.horizontalSpan = rootPageStyleManager.getColSpan(group);
		gd.verticalSpan = rootPageStyleManager.getRowSpan(group);
		contentComposite.setLayoutData(gd);
		GridLayout layout = new GridLayout();
		// separate links a bit more.
		layout.horizontalSpacing = 20;
		// set number of columns.
		int numColumns = rootPageStyleManager.getNumberOfColumns(group);
		numColumns = numColumns < 1 ? numChildren : numColumns;
		layout.numColumns = numColumns;
		layout.verticalSpacing = rootPageStyleManager.getVerticalSpacing(group);
		layout.horizontalSpacing = rootPageStyleManager
			.getHorizantalSpacing(group);
		contentComposite.setLayout(layout);
		for (int i = 0; i < children.length; i++) {
			if (((AbstractBaseIntroElement) children[i]).isFiltered())
				continue;
			if (children[i].getType() == AbstractIntroElement.GROUP)
				createGroupContent(contentComposite, (IntroGroup) children[i]);
			else if (children[i].getType() == AbstractIntroElement.LINK)
				createImageHyperlink(contentComposite, (IntroLink) children[i]);
			else if (children[i].getType() == AbstractIntroElement.CONTENT_PROVIDER)
				createContentProvider(contentComposite, (IntroContentProvider)children[i]);
		}
	}

	/**
	 * Creates an Image Hyperlink from an IntroLink. Model object is cached in
	 * link.
	 *
	 * @param body
	 * @param link
	 */
	private void createImageHyperlink(Composite parent, IntroLink link) {
		// create the container composite that will hold the imageHyperLink and
		// the label for the description.
		Composite container = toolkit.createComposite(parent);
		// Util.highlight(container, SWT.COLOR_CYAN);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
		gd.horizontalSpan = rootPageStyleManager.getColSpan(link);
		gd.verticalSpan = rootPageStyleManager.getRowSpan(link);
		container.setLayoutData(gd);

		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		container.setLayout(layout);
		ImageHyperlink imageLink = toolkit.createImageHyperlink(container,
			SWT.NULL);
		imageLink.setImage(rootPageStyleManager.getImage(link, "link-icon", //$NON-NLS-1$
			ImageUtil.DEFAULT_ROOT_LINK));
		imageLink.setHoverImage(rootPageStyleManager.getImage(link,
			"hover-icon", null)); //$NON-NLS-1$
		// each link is centered in cell.
		gd = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
		imageLink.setLayoutData(gd);
		// cache the intro link model object for description and URL.
		imageLink.setData(INTRO_LINK, link);
		imageLink.addHyperlinkListener(hyperlinkAdapter);

		// description label.
		Label linkLabel = toolkit.createLabel(container, link.getLabel());
		gd = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
		linkLabel.setFont(PageStyleManager.getBannerFont());
		linkLabel.setLayoutData(gd);
	}

	/**
	 * Creates a label to display the link description when you hover over a
	 * hyperlink.
	 *
	 * @param body
	 */
	private Label createHoverLabel(Composite body) {
		Label label = toolkit.createLabel(body, "", SWT.WRAP); //$NON-NLS-1$
		String key = StringUtil.concat(rootPage.getId(), ".", "hover-text.fg"); //$NON-NLS-1$ //$NON-NLS-2$
		Color fg = rootPageStyleManager.getColor(toolkit, key);
		if (fg == null)
			fg = toolkit.getColors().getColor(IFormColors.TITLE);
		label.setForeground(fg);
		label.setAlignment(SWT.CENTER);
		label.setFont(PageStyleManager.getBannerFont());
		return label;
	}

	private void createContentProvider(Composite parent, IntroContentProvider providerElement) {
		if (factory==null) {
			factory = new PageWidgetFactory(toolkit, rootPageStyleManager);
			factory.setContentProviderSite(site);
		}
		factory.createContentProvider(parent, providerElement);
	}
	public void setContentProviderSite(IIntroContentProviderSite site) {
		this.site = site;
	}
}
