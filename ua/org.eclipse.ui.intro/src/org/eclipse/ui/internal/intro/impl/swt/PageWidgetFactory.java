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
import org.eclipse.swt.SWTError;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.ui.internal.intro.impl.Messages;
import org.eclipse.ui.internal.intro.impl.model.AbstractBaseIntroElement;
import org.eclipse.ui.internal.intro.impl.model.AbstractIntroElement;
import org.eclipse.ui.internal.intro.impl.model.IntroContentProvider;
import org.eclipse.ui.internal.intro.impl.model.IntroGroup;
import org.eclipse.ui.internal.intro.impl.model.IntroHTML;
import org.eclipse.ui.internal.intro.impl.model.IntroImage;
import org.eclipse.ui.internal.intro.impl.model.IntroLink;
import org.eclipse.ui.internal.intro.impl.model.IntroSeparator;
import org.eclipse.ui.internal.intro.impl.model.IntroText;
import org.eclipse.ui.internal.intro.impl.model.loader.ContentProviderManager;
import org.eclipse.ui.internal.intro.impl.model.url.IntroURLParser;
import org.eclipse.ui.internal.intro.impl.util.DialogUtil;
import org.eclipse.ui.internal.intro.impl.util.ImageUtil;
import org.eclipse.ui.internal.intro.impl.util.Log;
import org.eclipse.ui.internal.intro.impl.util.StringUtil;
import org.eclipse.ui.internal.intro.impl.util.Util;
import org.eclipse.ui.intro.config.IIntroContentProvider;
import org.eclipse.ui.intro.config.IIntroContentProviderSite;

/**
 * Factory to create all UI forms widgets for the Forms intro presentation.
 */
public class PageWidgetFactory {

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


	protected FormToolkit toolkit;
	protected PageStyleManager styleManager;
	protected IIntroContentProviderSite site;


	/*
	 * protect bad creation.
	 */
	protected PageWidgetFactory(FormToolkit toolkit,
			PageStyleManager styleManager) {
		this.toolkit = toolkit;
		this.styleManager = styleManager;
	}

	public void setContentProviderSite(IIntroContentProviderSite site) {
		this.site = site;
	}

	public void createIntroElement(Composite parent,
			AbstractIntroElement element) {
		// check if this element is filtered, and if yes, do not create it.
		boolean isFiltered = getFilterState(element);
		if (isFiltered)
			return;

		Control c = null;
		switch (element.getType()) {
		case AbstractIntroElement.GROUP:
			IntroGroup group = (IntroGroup) element;
			c = createGroup(parent, group);
			updateLayoutData(c, element);
			// c must be a composite.
			Composite newParent = (Composite) c;
			if (c instanceof Section)
				// client is a composite also.
				newParent = (Composite) ((Section) newParent).getClient();
			AbstractIntroElement[] children = group.getChildren();
			for (int i = 0; i < children.length; i++)
				createIntroElement(newParent, children[i]);
			break;
		case AbstractIntroElement.LINK:
			IntroLink link = (IntroLink) element;
			c = createImageHyperlink(parent, link);
			updateLayoutData(c, element);
			break;
		case AbstractIntroElement.TEXT:
			IntroText text = (IntroText) element;
			c = createText(parent, text);
			updateLayoutData(c, element);
			break;
		case AbstractIntroElement.IMAGE:
			IntroImage image = (IntroImage) element;
			c = createImage(parent, image);
			if (c!=null)
				updateLayoutData(c, element);
			break;
		case AbstractIntroElement.HTML:
			IntroHTML html = (IntroHTML) element;
			if (html.isInlined()) {
				IntroText htmlText = html.getIntroText();
				if (htmlText != null)
					c = createText(parent, htmlText);
				else {
					IntroImage htmlImage = html.getIntroImage();
					if (htmlImage != null)
						c = createImage(parent, htmlImage);
				}
			} else {
				// embedded HTML, so we can show it from a link.
				String embddedLink = html.getSrc();
				if (embddedLink == null)
					break;
				String linkText = StringUtil.concat(
						"<p><a href=\"http://org.eclipse.ui.intro/openBrowser?url=", //$NON-NLS-1$
						embddedLink, "\">", //$NON-NLS-1$
						Messages.HTML_embeddedLink, "</a></p>"); //$NON-NLS-1$
				linkText = generateFormText(linkText);
				c = createFormText(parent, linkText, null);
			}
			if (c != null)
				updateLayoutData(c, element);
			break;
		case AbstractIntroElement.CONTENT_PROVIDER:
			IntroContentProvider provider = (IntroContentProvider) element;
			c = createContentProvider(parent, provider);
			updateLayoutData(c, element);
			break;
		case AbstractIntroElement.HR:
			IntroSeparator sep = (IntroSeparator)element;
			c = createSeparator(parent, sep);
			updateLayoutData(c, element);
			break;

		default:
			break;
		}
	}


	private void updateLayoutData(Control c, AbstractIntroElement element) {
		TableWrapData currentTd = (TableWrapData) c.getLayoutData();
		if (currentTd == null) {
			currentTd = new TableWrapData(TableWrapData.FILL,
				TableWrapData.FILL);
			currentTd.grabHorizontal = true;
			c.setLayoutData(currentTd);
		}

		currentTd.colspan = styleManager
			.getColSpan((AbstractBaseIntroElement) element);
		currentTd.rowspan = styleManager
			.getRowSpan((AbstractBaseIntroElement) element);

	}

	private Composite createGroup(Composite parent, IntroGroup group) {
		String label = group.getLabel();
		String description = styleManager.getDescription(group);
		boolean expandable = group.isExpandable();
		boolean expanded = group.isExpanded();
		Composite client = null;
		Composite control = null;
		if (description != null || label != null || expandable) {
			int style = description != null ? Section.DESCRIPTION : SWT.NULL;
			if (expandable)
				style |= ExpandableComposite.TWISTIE | ExpandableComposite.FOCUS_TITLE | ExpandableComposite.CLIENT_INDENT;
			if (expanded)
				style |= ExpandableComposite.EXPANDED;
			Section section = toolkit.createSection(parent, style);
			if (label != null)
				section.setText(label);
			if (description != null)
				section.setDescription(description);
			colorControl(section, group);
			client = toolkit.createComposite(section, SWT.WRAP);
			section.setClient(client);
			control = section;
		} else {
			client = toolkit.createComposite(parent, SWT.WRAP);
			control = client;
		}

		TableWrapLayout layout = new TableWrapLayout();
		int numColumns = styleManager.getNumberOfColumns(group);
		numColumns = numColumns < 1 ? 1 : numColumns;
		layout.numColumns = numColumns;
		layout.makeColumnsEqualWidth = styleManager.getEqualWidth(group);
		layout.verticalSpacing = styleManager.getVerticalSpacing(group);
		layout.horizontalSpacing = styleManager.getHorizantalSpacing(group);
		client.setLayout(layout);
		// Util.highlight(client, SWT.COLOR_YELLOW);
		return control;
	}

	/**
	 * Creates an Image Hyperlink from an IntroLink. Model object is NOT cached.
	 *
	 * @param body
	 * @param link
	 */
	private Control createImageHyperlink(Composite parent, IntroLink link) {
		Control control;
		Hyperlink linkControl;
		boolean showLinkDescription = styleManager.getShowLinkDescription();
		Image linkImage = styleManager.getImage(link, "link-icon", //$NON-NLS-1$
			ImageUtil.DEFAULT_LINK);

		if (showLinkDescription && link.getText() != null) {
			Composite container = toolkit.createComposite(parent);
			TableWrapLayout layout = new TableWrapLayout();
			layout.leftMargin = layout.rightMargin = 0;
			layout.topMargin = layout.bottomMargin = 0;
			layout.verticalSpacing = 0;
			layout.numColumns = 2;
			container.setLayout(layout);

			//Label ilabel = toolkit.createLabel(container, null);
			ImageHyperlink ilabel = toolkit.createImageHyperlink(container, SWT.NULL);
			ilabel.setImage(linkImage);
			ilabel.setHoverImage(styleManager.getImage(link, "hover-icon", //$NON-NLS-1$
				null));
			ilabel.setHref(link.getUrl());
			ilabel.addHyperlinkListener(hyperlinkAdapter);
			TableWrapData td = new TableWrapData();
			td.valign = TableWrapData.TOP;
			td.rowspan = 2;
			ilabel.setLayoutData(td);

			linkControl = toolkit.createHyperlink(container, null, SWT.WRAP);
			td = new TableWrapData(TableWrapData.LEFT, TableWrapData.BOTTOM);
			td.grabVertical = true;
			linkControl.setLayoutData(td);
			// Util.highlight(linkControl, SWT.COLOR_RED);
			// Util.highlight(container, SWT.COLOR_DARK_YELLOW);

			Control desc = createText(container, link.getIntroText());
			td = new TableWrapData(TableWrapData.FILL, TableWrapData.TOP);
			td.grabHorizontal = true;
			td.grabVertical = true;
			desc.setLayoutData(td);
			control = container;
		} else {
			ImageHyperlink imageLink = toolkit.createImageHyperlink(parent,
				SWT.WRAP | SWT.CENTER);
			imageLink.setImage(linkImage);
			imageLink.setHoverImage(styleManager.getImage(link, "hover-icon", //$NON-NLS-1$
				null));
			TableWrapData td = new TableWrapData();
			td.grabHorizontal = true;
			imageLink.setLayoutData(td);
			linkControl = imageLink;
			control = linkControl;
		}
		linkControl.setText(link.getLabel());
		linkControl.setFont(PageStyleManager.getBannerFont());
		colorControl(linkControl, link);
		linkControl.setHref(link.getUrl());
		linkControl.addHyperlinkListener(hyperlinkAdapter);
		// Util.highlight(linkControl, SWT.COLOR_DARK_YELLOW);
		return control;
	}

	/**
	 * Creates a forms Text or FormattedText.
	 *
	 * @param body
	 * @param link
	 */
	protected Control createText(Composite parent, IntroText text) {
		Color fg = styleManager.getColor(toolkit, text);
		boolean isBold = styleManager.isBold(text);
		// formatted case. If text is alredy formatted, the bold property is
		// ignored.
		if (text.isFormatted())
			return createFormText(parent, generateFormText(text.getText()), fg);

		// non formatted case.
		if (isBold)
			return createFormText(parent, generateBoldFormText(text.getText()),
				fg);
		return createText(parent, StringUtil.normalizeWhiteSpace(text.getText()), fg);
	}

	private Control createFormText(Composite parent, String text, Color fg) {
		FormText formText = toolkit.createFormText(parent, false);
		formText.addHyperlinkListener(hyperlinkAdapter);
		try {
			formText.setText(text, true, true);
		} catch (SWTError e) {
			Log.error(e.getMessage(), e);
			return createText(parent, text, fg);
		}
		if (fg != null)
			formText.setForeground(fg);
		return formText;
	}


	private Control createText(Composite parent, String text, Color fg) {
		Label label = toolkit.createLabel(parent, text, SWT.WRAP);
		if (fg != null)
			label.setForeground(fg);
		return label;
	}



	protected Control createImage(Composite parent, IntroImage image) {
		Label ilabel = null;
		Image imageFile = styleManager.getImage(image);
		if (imageFile != null) {
			ilabel = toolkit.createLabel(parent, null, SWT.LEFT);
			ilabel.setImage(imageFile);
			if (image.getAlt() != null)
				ilabel.setToolTipText(image.getAlt());
		}
		// for images, do not use default layout. Grab horizontal is not what we
		// want.
		if (ilabel!=null) {
			TableWrapData td = new TableWrapData();
			ilabel.setLayoutData(td);
		}
		return ilabel;
	}

	public Control createContentProvider(Composite parent,
			IntroContentProvider provider) {
		// If we've already loaded the content provider for this element,
		// retrieve it, otherwise load the class.
		// Create parent composite to hold dynamic content, and set layout
		// accordingly.
		Composite container = toolkit.createComposite(parent);
		TableWrapLayout layout = new TableWrapLayout();
		layout.topMargin = 0;
		layout.bottomMargin = 0;
		layout.leftMargin = 0;
		layout.rightMargin = 0;
		container.setLayout(layout);
		container.setData(provider);


		IIntroContentProvider providerClass = ContentProviderManager.getInst()
			.getContentProvider(provider);
		if (providerClass == null)
			// content provider never created before, create it.
			providerClass = ContentProviderManager.getInst()
				.createContentProvider(provider, site);

		if (providerClass != null) {
			try {
				providerClass.createContent(provider.getId(), container,
					toolkit);
			} catch (Exception e) {
				Log.error(
					"Failed to create the content of Intro model content provider: " //$NON-NLS-1$
							+ provider.getClassName(), e);
				// null provider.
				providerClass = null;
			}
		}

		if (providerClass == null) {
			// we failed to create a provider class, create the embedded text.
			IntroText text = provider.getIntroText();
			if (text != null)
				createText(container, text);
		}
		return container;
	}

	protected Control createSeparator(Composite parent, IntroSeparator sep) {
		String key = sep.getParentPage().getId()+".separator.fg"; //$NON-NLS-1$
		Color fg = styleManager.getColor(toolkit, key);
		//Composite l = toolkit.createCompositeSeparator(parent);
		Composite l = new Composite(parent, SWT.NULL);
		if (fg!=null)
			l.setBackground(fg);
		else
			l.setBackground(toolkit.getColors().getColor(IFormColors.SEPARATOR));
		TableWrapData td = new TableWrapData(TableWrapData.FILL,
				TableWrapData.FILL);
		td.grabHorizontal = true;
		td.maxHeight = 1;
		l.setLayoutData(td);
		return l;
	}

	private void colorControl(Control elementControl,
			AbstractBaseIntroElement element) {
		Color fg = styleManager.getColor(toolkit, element);
		if (fg != null)
			elementControl.setForeground(fg);
		Color bg = styleManager.getBackgrond(toolkit, element);
		if (bg != null)
			elementControl.setBackground(bg);
	}


	/*
	 * creates form text on a formatted string. A formatted string is any string
	 * that has a " <" in it. If it starts with a <p> then it is assumed that
	 * the text if a proper UI forms formatted text. If not, the <p> tag is
	 * added.
	 */
	private String generateFormText(String text) {
		StringBuilder sbuf = new StringBuilder();
		sbuf.append("<form>"); //$NON-NLS-1$
		if (text.startsWith("<p>")) //$NON-NLS-1$
			sbuf.append(text);
		else {
			sbuf.append("<p>"); //$NON-NLS-1$
			sbuf.append(text);
			sbuf.append("</p>"); //$NON-NLS-1$
		}
		sbuf.append("</form>"); //$NON-NLS-1$
		return sbuf.toString();
	}

	/**
	 * Will be only called for non formatted text.
	 *
	 * @param text
	 * @return
	 */
	private String generateBoldFormText(String text) {
		StringBuilder sbuf = new StringBuilder();
		sbuf.append("<form>"); //$NON-NLS-1$
		sbuf.append("<p>"); //$NON-NLS-1$
		sbuf.append("<b>"); //$NON-NLS-1$
		sbuf.append(text);
		sbuf.append("</b>"); //$NON-NLS-1$
		sbuf.append("</p>"); //$NON-NLS-1$
		sbuf.append("</form>"); //$NON-NLS-1$
		return sbuf.toString();
	}

	/**
	 * Check the filter state of the element. Only base elements have the filter
	 * attribute.
	 *
	 * @param element
	 * @return
	 */
	private boolean getFilterState(AbstractIntroElement element) {
		if (element.isOfType(AbstractIntroElement.BASE_ELEMENT))
			return ((AbstractBaseIntroElement) element).isFiltered();
		return false;
	}


}
