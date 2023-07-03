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
package org.eclipse.ui.internal.intro.impl.html;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;

import org.eclipse.core.runtime.Platform;
import org.eclipse.help.internal.util.ProductPreferences;
import org.eclipse.swt.browser.Browser;
import org.eclipse.ui.internal.intro.impl.FontSelection;
import org.eclipse.ui.internal.intro.impl.IIntroConstants;
import org.eclipse.ui.internal.intro.impl.IntroPlugin;
import org.eclipse.ui.internal.intro.impl.model.AbstractBaseIntroElement;
import org.eclipse.ui.internal.intro.impl.model.AbstractIntroElement;
import org.eclipse.ui.internal.intro.impl.model.AbstractIntroPage;
import org.eclipse.ui.internal.intro.impl.model.IntroContentProvider;
import org.eclipse.ui.internal.intro.impl.model.IntroGroup;
import org.eclipse.ui.internal.intro.impl.model.IntroHTML;
import org.eclipse.ui.internal.intro.impl.model.IntroHead;
import org.eclipse.ui.internal.intro.impl.model.IntroImage;
import org.eclipse.ui.internal.intro.impl.model.IntroInjectedIFrame;
import org.eclipse.ui.internal.intro.impl.model.IntroLink;
import org.eclipse.ui.internal.intro.impl.model.IntroPageTitle;
import org.eclipse.ui.internal.intro.impl.model.IntroSeparator;
import org.eclipse.ui.internal.intro.impl.model.IntroText;
import org.eclipse.ui.internal.intro.impl.model.IntroTheme;
import org.eclipse.ui.internal.intro.impl.model.loader.ContentProviderManager;
import org.eclipse.ui.internal.intro.impl.model.util.BundleUtil;
import org.eclipse.ui.internal.intro.impl.presentations.BrowserIntroPartImplementation;
import org.eclipse.ui.internal.intro.impl.util.Log;
import org.eclipse.ui.intro.config.IIntroContentProvider;
import org.eclipse.ui.intro.config.IIntroContentProviderSite;

public class IntroHTMLGenerator {

	private AbstractIntroPage introPage;

	private IIntroContentProviderSite providerSite;
	private boolean backgroundSizeWorks;

	/**
	 * Generates the HTML code that will be presented in the browser widget for the provided intro
	 * page.
	 *
	 * @param page
	 *            the page to generate HTML for
	 * @param presentation
	 *            the presentation associated with this page.
	 */
	public HTMLElement generateHTMLforPage(AbstractIntroPage page, IIntroContentProviderSite providerSite) {
		if (page == null)
			return null;
		this.introPage = page;
		this.providerSite = providerSite;

		initializeBackgroundSizeWorks();

		// generate and add the appropriate encoding to the top of the document
		// generateEncoding();
		// create the main HTML element, and all of its contents.
		return generateHTMLElement();
	}

	private void initializeBackgroundSizeWorks() {
		// Internet Explorer <= 9 doesn't properly handle background-size
		backgroundSizeWorks = true;
		try {
			if (isIE()) {
				Class<?> ieClass = Class.forName("org.eclipse.swt.browser.IE"); //$NON-NLS-1$
				Field field = ieClass.getDeclaredField("IEVersion"); //$NON-NLS-1$
				field.setAccessible(true);
				int value = field.getInt(ieClass);
				// We specifically care about background-size which works in 9+
				backgroundSizeWorks = value <= 0 || value >= 9;
			}
		} catch(Exception e) {
			// IE not found
		}
	}

	private boolean isIE() {
		return getBrowser() != null && "ie".equals(getBrowser().getBrowserType()); //$NON-NLS-1$
	}

	/**
	 * Return the SWT Browser instance being used to render the intro.
	 *
	 * @return the browser or {@code null} if the browser could not be determined
	 */
	private Browser getBrowser() {
		if (providerSite instanceof BrowserIntroPartImplementation) {
			return ((BrowserIntroPartImplementation) providerSite).getBrowser();
		}
		return null;
	}

	/*
	 * private HTMLElement generateEncoding() { HTMLElement encoding = new HTMLElement("");
	 * //$NON-NLS-1$ // TODO: figure out how to handle locale based encoding // As far as the HTML
	 * generator is concerned, this is probably as // simple as asking the model for the information
	 * return encoding; }
	 */

	/**
	 * Generates the HTML element and its content:
	 *
	 * <pre>
	 *
	 *                        &lt;HTML&gt;
	 *                        &lt;HEAD&gt;
	 *                        head content
	 *                        &lt;/HEAD&gt;
	 *                        &lt;BODY&gt;
	 *                        body content
	 *                        &lt;/BODY&gt;
	 *                        &lt;/HTML&gt;
	 *
	 * </pre>
	 *
	 * @return the html HTMLElement
	 */
	private HTMLElement generateHTMLElement() {
		// this is the outermost element, so it has no indent
		int indentLevel = 0;
		HTMLElement html = new FormattedHTMLElement(IIntroHTMLConstants.ELEMENT_HTML, indentLevel, true);
		HTMLElement head = generateHeadElement(indentLevel + 1);
		HTMLElement body = generateBodyElement(indentLevel + 1, head);
		html.addContent(head);
		html.addContent(body);
		return html;
	}

	/**
	 * Generates the HEAD element and its content:
	 *
	 * <pre>
	 *
	 *
	 *                        &lt;HEAD&gt;
	 *                        &lt;BASE href=&quot;base_plugin_location&gt;
	 *                        &lt;style type=&quot;text/css&quot;&gt;HTML, IMG { border: 0px; } &lt;/style&gt;
	 *                        &lt;TITLE&gt;page title &lt;/TITLE&gt;
	 *                        &lt;LINK href=&quot;style sheet&quot;&gt;
	 *                        additional head content, if specified
	 *                        &lt;/HEAD&gt;
	 *
	 * </pre>
	 *
	 * @param indentLevel
	 *            the number of indents to insert before the element when it is printed
	 * @return the head HTMLElement
	 */
	private HTMLElement generateHeadElement(int indentLevel) {
		HTMLElement head = new FormattedHTMLElement(IIntroHTMLConstants.ELEMENT_HEAD, indentLevel, true);
		addBrowserRenderingDirectives(head, indentLevel + 1);
		// add the title
		head.addContent(generateTitleElement(introPage.getTitle(), indentLevel + 1));
		head.addContent(generateUTF8CharsetElement(indentLevel + 1));
		// create the BASE element
		String basePath = BundleUtil.getResolvedResourceLocation(introPage.getBase(), introPage.getBundle());
		HTMLElement base = generateBaseElement(indentLevel + 1, basePath);
		if (base != null)
			head.addContent(base);
		// create the HTML style block
		head.addContent(generateStyleElement(indentLevel + 1));
		// add the presentation style
		String[] presentationStyles = IntroPlugin.getDefault().getIntroModelRoot().getPresentation()
				.getImplementationStyles();
		if (presentationStyles != null && introPage.injectSharedStyle()) {
			for (int i=0; i<presentationStyles.length; i++)
				head.addContent(generateLinkElement(presentationStyles[i], indentLevel + 1));
		}
		String pageStyle = introPage.getStyle();
		if (pageStyle != null)
			head.addContent(generateLinkElement(pageStyle, indentLevel + 1));
		// add javascript
		head.addContent(generateJavascriptElement(indentLevel + 1));

		// add the page's inherited style(s)
		String[] pageStyles = introPage.getStyles();
		for (int i = 0; i < pageStyles.length; i++) {
			pageStyle = pageStyles[i];
			if (pageStyle != null)
				head.addContent(generateLinkElement(pageStyle, indentLevel + 1));
		}
		// if there is additional head conent specified in an external file,
		// include it. Additional head content can be specified at the
		// implementation level (which would apply to ALL pages) and at the
		// page level (which would apply only to that particular page).
		// For the implementation's head contribution:
		StringBuilder content = null;
		IntroHead introHead = IntroPlugin.getDefault().getIntroModelRoot().getPresentation().getHead();
		if (introHead != null) {
			content = readFromFile(introHead.getSrc(), introHead.getInlineEncoding());
			if (content != null)
				head.addContent(content);
		}
		// For the page's head contribution:
		// TODO: there should only be one of these at the page level, not a
		// collection..
		IntroHead[] htmlHeads = introPage.getHTMLHeads();
		for (int i = 0; i < htmlHeads.length; i++) {
			introHead = htmlHeads[i];
			if (introHead != null) {
				content = readFromFile(introHead.getSrc(), introHead.getInlineEncoding());
				if (content != null)
					head.addContent(content);
			}
		}
		return head;
	}

	/**
	 * Add any browser-specific rendering quirks
	 *
	 * @param indentLevel
	 */
	private void addBrowserRenderingDirectives(HTMLElement head, int indentLevel) {
		/* IE renders intranet content in Compat View by default */
		if (isIE() && "html5".equals(getThemeProperty("standardSupport"))) { //$NON-NLS-1$ //$NON-NLS-2$
			// "Edge mode tells Internet Explorer to display content in the highest mode available"
			HTMLElement meta = new FormattedHTMLElement(IIntroHTMLConstants.ELEMENT_META, indentLevel, true);
			meta.addAttribute(IIntroHTMLConstants.ATTRIBUTE_HTTP_EQUIV, "X-UA-Compatible"); //$NON-NLS-1$
			meta.addAttribute(IIntroHTMLConstants.ATTRIBUTE_CONTENT, "IE=edge"); //$NON-NLS-1$
			head.addContent(meta);
		}
	}

	private String getThemeProperty(String key) {
		IntroTheme theme = introPage.getModelRoot().getTheme();
		if (theme == null) {
			return null;
		}
		Map<String, String> properties = theme.getProperties();
		if (properties == null) {
			return null;
		}
		return properties.get(key);
	}

	private HTMLElement generateUTF8CharsetElement(int indentLevel) {
		HTMLElement meta = new FormattedHTMLElement(IIntroHTMLConstants.ELEMENT_META, indentLevel, false);
		meta.addAttribute(IIntroHTMLConstants.ATTRIBUTE_HTTP_EQUIV, IIntroHTMLConstants.CONTENT_TYPE);
		meta.addAttribute(IIntroHTMLConstants.ATTRIBUTE_CONTENT, IIntroHTMLConstants.TYPE_HTML_UTF_8);
		return meta;
	}

	private HTMLElement generateJavascriptElement(int indentLevel) {
		String rel = "javascript/common.js"; //$NON-NLS-1$
		String abs = BundleUtil.getResolvedResourceLocation(rel, IntroPlugin.getDefault().getBundle());
		HTMLElement jselement = new FormattedHTMLElement("script", indentLevel, false); //$NON-NLS-1$
		jselement.addAttribute("type", "text/javascript"); //$NON-NLS-1$ //$NON-NLS-2$
		jselement.addAttribute("src", abs); //$NON-NLS-1$
		return jselement;
	}

	/**
	 * Generates the BODY element and its content:
	 *
	 * <pre>
	 *
	 *
	 *                        &lt;BODY&gt;
	 *                        &lt;DIV id=&quot;pageId&quot; class=&quot;pageClass&quot;&gt;
	 *                        page content
	 *                        &lt;/DIV&gt;
	 *                        &lt;/BODY&gt;
	 *
	 * </pre>
	 *
	 * @param indentLevel
	 *            the number of indents to insert before the element when it is printed
	 * @return the body HTMLElement
	 */
	private HTMLElement generateBodyElement(int indentLevel, HTMLElement head) {
		HTMLElement body = new FormattedHTMLElement(IIntroHTMLConstants.ELEMENT_BODY, indentLevel, true);
		// Create the div that contains the page content
		String pageId = (introPage.getId() != null) ? introPage.getId() : IIntroHTMLConstants.DIV_ID_PAGE;
		HTMLElement pageContentDiv = generateDivElement(pageId, indentLevel + 1);
		if (introPage.getStyleId() != null)
			pageContentDiv.addAttribute(IIntroHTMLConstants.ATTRIBUTE_CLASS, introPage.getStyleId());
		if (introPage.getBackgroundImage() != null)
			pageContentDiv.addAttribute(IIntroHTMLConstants.ATTRIBUTE_STYLE,
					"background-image : url(" + introPage.getBackgroundImage() + ")"); //$NON-NLS-1$ //$NON-NLS-2$

		// Add any children of the page, in the order they are defined
		AbstractIntroElement[] children = introPage.getChildren();
		for (int i = 0; i < children.length; i++) {
			AbstractIntroElement child = children[i];
			// use indentLevel + 2 here, since this element is contained within
			// the pageContentDiv
			HTMLElement childElement = generateIntroElement(child, indentLevel + 2);
			if (childElement != null) {
				addMixinStyle(childElement, child.getMixinStyle());
				pageContentDiv.addContent(childElement);
			}
		}
		body.addContent(pageContentDiv);
		return body;
	}

	/**
	 * Given an IntroElement, generate the appropriate HTMLElement
	 *
	 * @param element
	 *            the IntroElement
	 * @param indentLevel
	 *            the number of indents to insert before the element when it is printed
	 * @return an HTMLElement
	 */
	private HTMLElement generateIntroElement(AbstractIntroElement element, int indentLevel) {
		if (element == null)
			return null;
		// check to see if this element should be filtered from the HTML
		// presentation
		if (filteredFromPresentation(element))
			return null;
		switch (element.getType()) {
		case AbstractIntroElement.GROUP:
			return generateIntroDiv((IntroGroup) element, indentLevel);
		case AbstractIntroElement.LINK:
			return generateIntroLink((IntroLink) element, indentLevel);
		case AbstractIntroElement.HTML:
			return generateIntroHTML((IntroHTML) element, indentLevel);
		case AbstractIntroElement.CONTENT_PROVIDER:
			return generateIntroContent((IntroContentProvider) element, indentLevel);
		case AbstractIntroElement.IMAGE:
			return generateIntroImage((IntroImage) element, indentLevel);
		case AbstractIntroElement.HR:
			return generateIntroSeparator((IntroSeparator) element, indentLevel);
		case AbstractIntroElement.TEXT:
			return generateIntroText((IntroText) element, indentLevel);
		case AbstractIntroElement.PAGE_TITLE:
			return generateIntroTitle((IntroPageTitle) element, indentLevel);
		case AbstractIntroElement.INJECTED_IFRAME:
			return generateIntroInjectedIFrame((IntroInjectedIFrame) element, indentLevel);
		default:
			return null;
		}
	}

	/**
	 * Create a div element and its content from an IntroDiv:
	 *
	 * <pre>
	 *
	 *
	 *                        &lt;div id=&quot;attrvalue&quot;&gt;
	 *                        &lt;h4&gt;&lt;span class=&quot;div-label&quot;&gt;attrvalue&lt;/span&gt;&lt;h4&gt;
	 *                        any defined divs, links, html, images, text, includes
	 *                        &lt;/div&gt;
	 *
	 * </pre>
	 *
	 * @param element
	 *            the IntroDiv
	 * @param indentLevel
	 *            the number of indents to insert before the element when it is printed
	 * @return a div HTMLElement
	 */
	private HTMLElement generateIntroDiv(IntroGroup element, int indentLevel) {
		// Create the outer div element
		HTMLElement divElement = generateDivElement(element.getId(), indentLevel);
		HTMLElement childContainer = divElement;
		// if a div class was specified, add it
		if (element.getStyleId() != null)
			divElement.addAttribute(IIntroHTMLConstants.ATTRIBUTE_CLASS, element.getStyleId());
		// Create the div label, if specified
		if (element.getLabel() != null) {
			if (element.isExpandable()) {
				HTMLElement divLabel = new FormattedHTMLElement(IIntroHTMLConstants.ELEMENT_SPAN,
						indentLevel + 2, false);
				divLabel.addContent(element.getLabel());
				divLabel.addAttribute(IIntroHTMLConstants.ATTRIBUTE_CLASS,
												"section-title");//$NON-NLS-1$
				String clientId = element.getId() + "-content"; //$NON-NLS-1$
				String toggleClosedId = element.getId() + "-toggle-closed"; //$NON-NLS-1$
				String toggleOpenId = element.getId() + "-toggle-open"; //$NON-NLS-1$
				String href = "#"; //$NON-NLS-1$
				HTMLElement link = new FormattedHTMLElement(IIntroHTMLConstants.ELEMENT_ANCHOR,
						indentLevel + 1, true);
				link.addAttribute(IIntroHTMLConstants.ATTRIBUTE_HREF, href);
				link.addAttribute(IIntroHTMLConstants.ATTRIBUTE_CLASS, "section-title-link"); //$NON-NLS-1$
				StringBuilder call = new StringBuilder();
				call.append("return (toggleSection('");//$NON-NLS-1$
				call.append(clientId);
				call.append("','");//$NON-NLS-1$
				call.append(toggleClosedId);
				call.append("','");//$NON-NLS-1$
				call.append(toggleOpenId);
				call.append("'))"); //$NON-NLS-1$
				link.addAttribute("onClick", call.toString()); //$NON-NLS-1$
				link.addContent(divLabel);
				divElement.addContent(link);
				// Add toggle images
				HTMLElement toggleImageClosed = new FormattedHTMLElement(IIntroHTMLConstants.ELEMENT_IMG,
						indentLevel + 2, false, false);
				toggleImageClosed.addAttribute(IIntroHTMLConstants.ATTRIBUTE_ID, toggleClosedId);
				toggleImageClosed.addAttribute(IIntroHTMLConstants.ATTRIBUTE_SRC, BundleUtil
						.getResolvedResourceLocation(IIntroHTMLConstants.IMAGE_SRC_BLANK,
								IIntroConstants.PLUGIN_ID));
				toggleImageClosed.addAttribute(IIntroHTMLConstants.ATTRIBUTE_CLASS, "section-toggle-image-closed"); //$NON-NLS-1$
				if (element.isExpanded())
					toggleImageClosed.addAttribute(IIntroHTMLConstants.ATTRIBUTE_STYLE, "display: none"); //$NON-NLS-1$
				link.addContent(toggleImageClosed);
				HTMLElement toggleImageOpen = new FormattedHTMLElement(IIntroHTMLConstants.ELEMENT_IMG,
						indentLevel + 2, false, false);
				toggleImageOpen.addAttribute(IIntroHTMLConstants.ATTRIBUTE_ID, toggleOpenId);
				toggleImageOpen.addAttribute(IIntroHTMLConstants.ATTRIBUTE_SRC, BundleUtil
						.getResolvedResourceLocation(IIntroHTMLConstants.IMAGE_SRC_BLANK,
								IIntroConstants.PLUGIN_ID));
				toggleImageOpen.addAttribute(IIntroHTMLConstants.ATTRIBUTE_CLASS, "section-toggle-image-open"); //$NON-NLS-1$
				if (element.isExpanded())
					toggleImageOpen.addAttribute(IIntroHTMLConstants.ATTRIBUTE_STYLE, "display: inline"); //$NON-NLS-1$
				link.addContent(toggleImageOpen);
				childContainer = generateDivElement(clientId, indentLevel + 1);
				childContainer.addAttribute("class", "section-body"); //$NON-NLS-1$//$NON-NLS-2$
				if (element.isExpanded())
					childContainer.addAttribute(IIntroHTMLConstants.ATTRIBUTE_STYLE, "display: block"); //$NON-NLS-1$
				divElement.addContent(childContainer);
			} else {
				HTMLElement divLabel = generateTextElement(IIntroHTMLConstants.ELEMENT_H4, null,
						IIntroHTMLConstants.SPAN_CLASS_DIV_LABEL, element.getLabel(), indentLevel + 1);
				divElement.addContent(divLabel);
			}
		}
		if (element.getBackgroundImage() != null) {
			String imageUrl = element.getBackgroundImage();
			imageUrl = BundleUtil.getResolvedResourceLocation(element.getBase(), imageUrl, element
					.getBundle());
			String style;
			if (Platform.getWS().equals(Platform.WS_WIN32) && !backgroundSizeWorks && imageUrl.toLowerCase().endsWith(".png")) { //$NON-NLS-1$
				// IE 5.5+ does not handle alphas in PNGs without
				// this hack. Remove when IE7 becomes widespread
				style = "filter:progid:DXImageTransform.Microsoft.AlphaImageLoader(src='" + imageUrl + "', sizingMethod='crop');"; //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				style = "background-image : url(" + imageUrl + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			}
			divElement.addAttribute(IIntroHTMLConstants.ATTRIBUTE_STYLE, style);
		}
		// Add any children of the div, in the order they are defined
		AbstractIntroElement[] children = element.getChildren();
		for (int i = 0; i < children.length; i++) {
			AbstractIntroElement child = children[i];
			HTMLElement childElement = generateIntroElement(child, indentLevel + 1);
			if (childElement != null) {
				addMixinStyle(childElement, child.getMixinStyle());
				childContainer.addContent(childElement);
			}
		}
		return divElement;
	}

	private void addMixinStyle(HTMLElement element, String mixinStyle) {
		if (mixinStyle == null)
			return;
		String key = "class"; //$NON-NLS-1$
		String original = element.getElementAttributes().get(key);
		if (original == null)
			original = mixinStyle;
		else
			original += " " + mixinStyle; //$NON-NLS-1$
		element.addAttribute(key, original);
	}

	/**
	 * Generates an anchor (link) element and its content from an IntroLink:
	 *
	 * <pre>
	 *
	 *                        &lt;A id=linkId class=&quot;link&quot; href=linkHref&gt;
	 *                        &lt;IMG src=&quot;blank.gif&quot;&gt;
	 *                        &lt;SPAN class=&quot;link-label&quot;&gt;linkLabel &lt;/SPAN&gt;
	 *                        &lt;P&gt;&lt;SPAN&gt;text&lt;/SPAN&gt;&lt;/P&gt;
	 *                        &lt;/A&gt;
	 *
	 * </pre>
	 *
	 * @param element
	 *            the IntroLink
	 * @param indentLevel
	 *            the number of indents to insert before the element when it is printed
	 * @return an anchor (&lt;A&gt;) HTMLElement
	 */
	private HTMLElement generateIntroLink(IntroLink element, int indentLevel) {
		String styleId = element.getStyleId();
		boolean useTable = ProductPreferences.isRTL() && "content-link".equals(styleId); //$NON-NLS-1$
		HTMLElement anchor1 = generateAnchorElement(element, indentLevel);
		HTMLElement anchor2 = null;
		HTMLElement labelAnchor = anchor1;
		int indentBase = indentLevel;
		if (useTable) {
			indentBase = indentLevel + 1;
			anchor2 = generateAnchorElement(element, indentLevel + 1);
			labelAnchor = anchor2;
		}
		// add <IMG src="blank.gif">
		String blankImageURL = BundleUtil.getResolvedResourceLocation(IIntroHTMLConstants.IMAGE_SRC_BLANK,
				IIntroConstants.PLUGIN_ID);
		if (blankImageURL != null) {
			anchor1.addContent(generateImageElement(blankImageURL, null, null, IIntroHTMLConstants.IMAGE_CLASS_BG,
					indentBase + 1));
		}
		// add link image, if one is specified
		if (element.getImg() != null) {
			HTMLElement img = generateIntroElement(element.getImg(), indentBase + 1);
			if (img != null)
				anchor1.addContent(img);
		}
		if (!useTable) {
			HTMLElement imageDiv = new FormattedHTMLElement(IIntroHTMLConstants.ELEMENT_DIV, indentBase+1, false);
			imageDiv.addAttribute(IIntroHTMLConstants.ATTRIBUTE_CLASS,
					IIntroHTMLConstants.LINK_EXTRA_DIV);
			anchor1.addContent(imageDiv);
		}
		// add <SPAN class="link-label">linkLabel</SPAN>
		if (element.getLabel() != null) {
			HTMLElement label = generateSpanElement(IIntroHTMLConstants.SPAN_CLASS_LINK_LABEL,
					indentBase + 2);
			label.addContent(element.getLabel());
			labelAnchor.addContent(label);
		}
		IntroText linkText = element.getIntroText();
		if (linkText != null && linkText.getText() != null) {
			HTMLElement text = generateIntroElement(linkText, indentBase + 3);
			if (text != null)
				labelAnchor.addContent(text);
		}
		if (!useTable) {
			return anchor1;
		}
		HTMLElement table = new FormattedHTMLElement(IIntroHTMLConstants.ELEMENT_TABLE, indentLevel, false);
		HTMLElement tr = new FormattedHTMLElement(IIntroHTMLConstants.ELEMENT_TR, indentLevel + 1, false);
		table.addContent(tr);
		HTMLElement td1 = new FormattedHTMLElement(IIntroHTMLConstants.ELEMENT_TD, indentLevel + 1, false);
		HTMLElement td2 = new FormattedHTMLElement(IIntroHTMLConstants.ELEMENT_TD, indentLevel + 1, false);
		tr.addContent(td1);
		tr.addContent(td2);
		td1.addContent(anchor1);
		td2.addContent(anchor2);
		return table;
	}

	/**
	 * Generate the appropriate HTML from an IntroHTML. If the IntroHTML type is "inline", then the
	 * content from the referenced file is emitted as-is into a div element. If the type is "embed",
	 * an OBJECT html element is created whose <code>data</code> attribute is equal to the
	 * IntroHTML's <code>src</code> value
	 *
	 * @param element
	 *            the IntroHTML
	 * @param indentLevel
	 *            the number of indents to insert before the element when it is printed
	 * @return an HTMLElement
	 */
	private HTMLElement generateIntroHTML(IntroHTML element, int indentLevel) {
		if (element.isInlined())
			return generateInlineIntroHTML(element, indentLevel);

		return generateEmbeddedIntroHTML(element, indentLevel);
	}

	/**
	 * Generate an image element from an IntroImage:
	 *
	 * <pre>
	 *
	 *                        &lt;IMG src=imageSrc id=imageId&gt;
	 *
	 * </pre>
	 *
	 * @param element
	 *            the IntroImage
	 * @param indentLevel
	 *            the number of indents to insert before the element when it is printed
	 * @return an img HTMLElement
	 */
	private HTMLElement generateIntroImage(IntroImage element, int indentLevel) {
		HTMLElement imageElement = generateImageElement(element.getSrc(), element.getAlt(),element.getTitle(), element
				.getStyleId(), indentLevel);
		if (element.getId() != null)
			imageElement.addAttribute(IIntroHTMLConstants.ATTRIBUTE_ID, element.getId());
		return imageElement;
	}

	private HTMLElement generateIntroSeparator(IntroSeparator element, int indentLevel) {
		HTMLElement hrElement = new FormattedHTMLElement(IIntroHTMLConstants.ELEMENT_HR, indentLevel, false);
		if (element.getId() != null)
			hrElement.addAttribute(IIntroHTMLConstants.ATTRIBUTE_ID, element.getId());
		if (element.getStyleId() != null)
			hrElement.addAttribute(IIntroHTMLConstants.ATTRIBUTE_STYLE, element.getStyleId());
		return hrElement;
	}

	/**
	 * Generate a paragraph (&lt;P&gt;) element from an IntroText. The paragraph element will
	 * contain a span element that will contain the actual text. Providing the span element provides
	 * additional flexibility for CSS designers.
	 *
	 * <pre>
	 *
	 *
	 *                        &lt;P&gt;&lt;SPAN&gt;spanContent&lt;/SPAN&gt;&lt;/P&gt;
	 *
	 * </pre>
	 *
	 * @param element
	 *            the IntroText
	 * @param indentLevel
	 *            the number of indents to insert before the element when it is printed
	 * @return a paragraph HTMLElement
	 */
	private HTMLElement generateIntroText(IntroText element, int indentLevel) {
		String spanClass = (element.getStyleId() != null) ? element.getStyleId()
				: IIntroHTMLConstants.SPAN_CLASS_TEXT;
		HTMLElement textElement = generateTextElement(IIntroHTMLConstants.ELEMENT_PARAGRAPH, element.getId(),
				spanClass, element.getText(), indentLevel);
		return textElement;
	}

	/**
	 * @param element
	 * @param indentLevel
	 * @return
	 */
	private HTMLElement generateIntroInjectedIFrame(IntroInjectedIFrame element, int indentLevel) {
		HTMLElement iframe = generateIFrameElement(element.getIFrameURL(), "0", //$NON-NLS-1$
				"auto", indentLevel); //$NON-NLS-1$
		return iframe;
	}

	/**
	 * @param element
	 * @param indentLevel
	 * @return
	 */
	private HTMLElement generateIntroTitle(IntroPageTitle element, int indentLevel) {
		HTMLElement titleElement = generateHeaderDiv(element.getId(), element.getStyleId(),
				IIntroHTMLConstants.ELEMENT_H1, element.getTitle(), indentLevel);
		return titleElement;
	}

	/**
	 * Generate "inline" content from an IntroHTML. The content from the file referenced by the
	 * IntroHTML's <code>src</code> attribute is emitted as-is into a div element:
	 *
	 * <pre>
	 *
	 *
	 *                        &lt;div id=&quot;attrvalue&quot; class=&quot;attrvalue2&quot;&gt;
	 *                        content from file specified in src attribute
	 *                        &lt;/div&gt;
	 *
	 * </pre>
	 *
	 * @param element
	 *            the IntroHTML
	 * @param indentLevel
	 *            the number of indents to insert before the element when it is printed
	 * @return a div HTMLElement, or null if there was a problem reading from the file
	 */
	private HTMLElement generateInlineIntroHTML(IntroHTML element, int indentLevel) {
		// make sure to ask model for encoding. If encoding is null (ie: not
		// specified in
		// markup, local encoding is used.
		StringBuilder content = readFromFile(element.getSrc(), element.getInlineEncoding());
		if (content != null && content.length() > 0) {
			// Create the outer div element
			String divClass = (element.getStyleId() != null) ? element.getStyleId()
					: IIntroHTMLConstants.DIV_CLASS_INLINE_HTML;
			HTMLElement divElement = generateDivElement(element.getId(), divClass, indentLevel);
			// add the content of the specified file into the div element
			divElement.addContent(content);
			return divElement;
		}
		return null;
	}

	/**
	 * Includes HTML content that is created by an IIntroContentProvider implementation.
	 *
	 * @param element
	 * @param indentLevel
	 * @return
	 */
	private HTMLElement generateIntroContent(IntroContentProvider element, int indentLevel) {
		// create a new div to wrap the content
		HTMLElement divElement = generateDivElement(element.getId(),
				IIntroHTMLConstants.DIV_CLASS_PROVIDED_CONTENT, indentLevel);

		// If we've already loaded the content provider for this element,
		// retrieve it, otherwise load the class
		IIntroContentProvider providerClass = ContentProviderManager.getInst().getContentProvider(element);
		if (providerClass == null)
			// content provider never created before, create it.
			providerClass = ContentProviderManager.getInst().createContentProvider(element, providerSite);

		if (providerClass != null) {
			StringWriter stringWriter = new StringWriter();
			try (PrintWriter pw = new PrintWriter(stringWriter)) {
				// create the specialized content
				providerClass.createContent(element.getId(), pw);
				// add the content of the specified file into the div element
				stringWriter.flush();
				divElement.addContent(stringWriter.toString());
			}
		} else {
			// we couldn't load the content provider, so add any alternate
			// text content if there is any
			IntroText htmlText = element.getIntroText();
			if (htmlText != null && htmlText.getText() != null) {
				String textClass = (htmlText.getStyleId() != null) ? htmlText.getStyleId()
						: IIntroHTMLConstants.SPAN_CLASS_TEXT;
				HTMLElement text = generateTextElement(IIntroHTMLConstants.ELEMENT_PARAGRAPH, htmlText
						.getId(), textClass, element.getText(), indentLevel);
				if (text != null)
					divElement.addContent(text);
			}
		}
		return divElement;
	}

	/**
	 * Generate "embedded" content from an IntroHTML. An OBJECT html element is created whose
	 * <code>data</code> attribute is equal to the IntroHTML's <code>src</code> value.
	 *
	 * <pre>
	 *
	 *                        &lt;OBJECT type=&quot;text/html&quot; data=&quot;attrvalue&quot;&gt;
	 *                        alternative text in case the object can not be rendered
	 *                        &lt;/OBJECT&gt;
	 *
	 * </pre>
	 *
	 * @param element
	 *            the IntroHTML
	 * @param indentLevel
	 *            the number of indents to insert before the element when it is printed
	 * @return an object HTMLElement
	 */
	private HTMLElement generateEmbeddedIntroHTML(IntroHTML element, int indentLevel) {
		HTMLElement objectElement = new FormattedHTMLElement(IIntroHTMLConstants.ELEMENT_OBJECT, indentLevel,
				true);
		objectElement.addAttribute(IIntroHTMLConstants.ATTRIBUTE_TYPE, IIntroHTMLConstants.OBJECT_TYPE);
		if (element.getId() != null)
			objectElement.addAttribute(IIntroHTMLConstants.ATTRIBUTE_ID, element.getId());
		if (element.getSrc() != null)
			objectElement.addAttribute(IIntroHTMLConstants.ATTRIBUTE_DATA, element.getSrc());
		if (element.getStyleId() != null)
			objectElement.addAttribute(IIntroHTMLConstants.ATTRIBUTE_CLASS, element.getStyleId());
		// The alternative content is added in case the browser can not render
		// the specified content.
		IntroText htmlText = element.getIntroText();
		if (htmlText != null && htmlText.getText() != null) {
			String textClass = (htmlText.getStyleId() != null) ? htmlText.getStyleId()
					: IIntroHTMLConstants.SPAN_CLASS_TEXT;
			HTMLElement text = generateTextElement(IIntroHTMLConstants.ELEMENT_PARAGRAPH, htmlText.getId(),
					textClass, element.getText(), indentLevel);
			if (text != null)
				objectElement.addContent(text);
		}
		if (element.getIntroImage() != null) {
			HTMLElement img = generateIntroImage(element.getIntroImage(), indentLevel);
			if (img != null)
				objectElement.addContent(img);
		}
		return objectElement;
	}

	/**
	 * Generates the BASE element for the head of the html document. Each document can have only one
	 * base element
	 *
	 * <pre>
	 *
	 *
	 *                      	&lt;BASE href=baseURL&gt;
	 * </pre>
	 *
	 * @param indentLevel
	 * @param baseURL
	 * @return
	 */
	private HTMLElement generateBaseElement(int indentLevel, String baseURL) {
		HTMLElement base = new FormattedHTMLElement(IIntroHTMLConstants.ELEMENT_BASE, indentLevel, true,
				false);
		if (baseURL != null)
			base.addAttribute(IIntroHTMLConstants.ATTRIBUTE_HREF, baseURL);
		return base;
	}

	/**
	 * Generates the style element that goes into HEAD:
	 *
	 * <pre>
	 *
	 *                        &lt;style type=&quot;text/css&quot;&gt;HTML, IMG { border: 0px; } &lt;/style&gt;
	 *
	 * </pre>
	 *
	 * @param indentLevel
	 *            the number of indents to insert before the element when it is printed
	 * @return the style HTMLElement
	 */
	private HTMLElement generateStyleElement(int indentLevel) {
		HTMLElement style = new FormattedHTMLElement(IIntroHTMLConstants.ELEMENT_STYLE, indentLevel, false);
		style.addAttribute(IIntroHTMLConstants.ATTRIBUTE_TYPE, IIntroHTMLConstants.LINK_STYLE);
		style.addContent(IIntroHTMLConstants.STYLE_HTML);
		IntroTheme theme = introPage.getModelRoot().getTheme();
		if (theme != null && theme.isScalable()
				&& FontSelection.FONT_RELATIVE.equals(FontSelection.getFontStyle())) {
			String sizeStyle = FontSelection.generatePageFontStyle();
			style.addContent(sizeStyle);
		}
		return style;
	}

	/**
	 * Generates the title element and its content:
	 *
	 * <pre>
	 *
	 *                        &lt;TITLE&gt;intro title&lt;/TITLE&gt;
	 *
	 * </pre>
	 *
	 * @param title
	 *            the title of this intro page
	 * @param indentLevel
	 *            the number of indents to insert before the element when it is printed
	 * @return the title HTMLElement
	 */
	private HTMLElement generateTitleElement(String title, int indentLevel) {
		HTMLElement titleElement = new FormattedHTMLElement(IIntroHTMLConstants.ELEMENT_TITLE, indentLevel,
				false);
		if (title != null)
			titleElement.addContent(title);
		return titleElement;
	}

	/**
	 * Generates a link element that refers to a cascading style sheet (CSS):
	 *
	 * <pre>
	 *
	 *
	 *                        &lt;LINK rel=&quot;stylesheet&quot; style=&quot;text/css&quot; href=&quot;style sheet&quot;&gt;
	 * </pre>
	 *
	 * @param href
	 *            the value of the href attribute for this link element
	 * @param indentLevel
	 *            the number of indents to insert before the element when it is printed
	 * @return a link HTMLElement
	 */
	private HTMLElement generateLinkElement(String href, int indentLevel) {
		HTMLElement link = new FormattedHTMLElement(IIntroHTMLConstants.ELEMENT_LINK, indentLevel, true,
				false);
		link.addAttribute(IIntroHTMLConstants.ATTRIBUTE_RELATIONSHIP, IIntroHTMLConstants.LINK_REL);
		link.addAttribute(IIntroHTMLConstants.ATTRIBUTE_TYPE, IIntroHTMLConstants.LINK_STYLE);
		if (href != null)
			link.addAttribute(IIntroHTMLConstants.ATTRIBUTE_HREF, href);
		return link;
	}

	/**
	 * Generate an anchor element:
	 *
	 * <pre>
	 *
	 *                        &lt;A id=linkId class=linkClass href=linkHref&gt; &lt;/A&gt;
	 *
	 * </pre>
	 *
	 * @param link
	 *            the IntroLink element that contains the value for the id and href attributes
	 * @param indentLevel
	 *            the number of indents to insert before the element when it is printed
	 * @return an anchor (&lt;A&gt;) HTMLElement
	 */
	private HTMLElement generateAnchorElement(IntroLink link, int indentLevel) {
		HTMLElement anchor = new FormattedHTMLElement(IIntroHTMLConstants.ELEMENT_ANCHOR, indentLevel, true);
		if (link.getId() != null)
			anchor.addAttribute(IIntroHTMLConstants.ATTRIBUTE_ID, link.getId());
		if (link.getUrl() != null)
			anchor.addAttribute(IIntroHTMLConstants.ATTRIBUTE_HREF, link.getUrl());
		if (link.getStyleId() != null)
			anchor.addAttribute(IIntroHTMLConstants.ATTRIBUTE_CLASS, link.getStyleId());
		else
			anchor.addAttribute(IIntroHTMLConstants.ATTRIBUTE_CLASS, IIntroHTMLConstants.ANCHOR_CLASS_LINK);
		return anchor;
	}

	/**
	 * Generates a div block that contains a header and span element:
	 *
	 * <pre>
	 *
	 *
	 *                        &lt;DIV id=divId&gt;
	 *                        &lt;H&gt;&lt;SPAN&gt;spanContent &lt;/SPAN&gt; &lt;/H&gt;
	 *                        &lt;/DIV&gt;
	 *
	 * </pre>
	 *
	 * @param divId
	 *            the id of the div to create
	 * @param divClass
	 *            the class of the div
	 * @param headerType
	 *            what type of header to create (e.g., H1, H2, etc)
	 * @param spanContent
	 *            the span content
	 * @param indentLevel
	 *            the number of indents to insert before the element when it is printed
	 * @return a div HTMLElement that contains a header
	 */
	private HTMLElement generateHeaderDiv(String divId, String divClass, String headerType,
			String spanContent, int indentLevel) {
		// create the text element: <P><SPAN>spanContent</SPAN></P>
		HTMLElement text = generateTextElement(headerType, null, null, spanContent, indentLevel + 1);
		// create the containing div element
		HTMLElement div = generateDivElement(divId, divClass, indentLevel);
		div.addContent(text);
		return div;
	}

	/**
	 * Generates a span element inside a text element, where the text element can be a P
	 * (paragraph), or any of the H (Header) elements. Providing the span element provides
	 * additional flexibility for CSS designers.
	 *
	 * <pre>
	 *
	 *                        &lt;P&gt;&lt;SPAN&gt;spanContent&lt;/SPAN&gt;&lt;/P&gt;
	 *
	 * </pre>
	 *
	 * @param type
	 *            the type of text element to create (e.g., P, H1, H2, etc)
	 * @param spanID
	 *            the id of the span element, or null
	 * @param spanClass
	 *            the class of the span element, or null
	 * @param spanContent
	 *            the span content
	 * @param indentLevel
	 *            the number of indents to insert before the element when it is printed
	 * @return a text HTMLElement that contains a span element
	 */
	private HTMLElement generateTextElement(String type, String spanID, String spanClass, String spanContent,
			int indentLevel) {
		// Create the span: <SPAN>spanContent</SPAN>
		HTMLElement span = new HTMLElement(IIntroHTMLConstants.ELEMENT_SPAN);
		if (spanID != null)
			span.addAttribute(IIntroHTMLConstants.ATTRIBUTE_ID, spanID);
		if (spanClass != null)
			span.addAttribute(IIntroHTMLConstants.ATTRIBUTE_CLASS, spanClass);
		if (spanContent != null)
			span.addContent(spanContent);
		if (type != null) {
		// Create the enclosing text element: <P><SPAN>spanContent</SPAN></P>
		HTMLElement text = new FormattedHTMLElement(type, indentLevel, false);
		text.addContent(span);
		return text;
		} else {
			return span;
		}
	}

	/**
	 * Generates a DIV element with the provided indent, id, and class.
	 *
	 * @param divId
	 *            value for the div's id attribute
	 * @param divClass
	 *            value for the div's class attribute
	 * @param indentLevel
	 *            the number of indents to insert before the element when it is printed
	 * @return a div HTMLElement
	 */
	private HTMLElement generateDivElement(String divId, String divClass, int indentLevel) {
		HTMLElement div = generateDivElement(divId, indentLevel);
		div.addAttribute(IIntroHTMLConstants.ATTRIBUTE_CLASS, divClass);
		return div;
	}

	/**
	 * Generates a DIV element with the provided indent and id.
	 *
	 * @param divId
	 *            value for the div's id attribute
	 * @param indentLevel
	 *            the number of indents to insert before the element when it is printed
	 * @return a div HTMLElement
	 */
	private HTMLElement generateDivElement(String divId, int indentLevel) {
		HTMLElement div = new FormattedHTMLElement(IIntroHTMLConstants.ELEMENT_DIV, indentLevel, true);
		if (divId != null)
			div.addAttribute(IIntroHTMLConstants.ATTRIBUTE_ID, divId);
		return div;
	}

	/**
	 * Generates an IMG element:
	 *
	 * <pre>
	 *
	 *
	 *                        &lt;IMG src=imageSrc alt=altText&gt;
	 *
	 * </pre>
	 *
	 * @param imageSrc
	 *            the value to be supplied to the src attribute
	 * @param indentLevel
	 *            the number of indents to insert before the element when it is printed
	 * @return an img HTMLElement
	 */
	private HTMLElement generateImageElement(String imageSrc, String altText, String title, String imageClass,
			int indentLevel) {
		HTMLElement image = new FormattedHTMLElement(IIntroHTMLConstants.ELEMENT_IMG, indentLevel, true,
				false);
		boolean pngOnWin32 = imageSrc != null && Platform.getWS().equals(Platform.WS_WIN32)
				&& !backgroundSizeWorks && imageSrc.toLowerCase().endsWith(".png"); //$NON-NLS-1$
		if (imageSrc == null || pngOnWin32) {
			// we must handle PNGs here - IE does not support alpha blanding well.
			// We will set the alpha image loader and load the real image
			// that way. The 'src' attribute in the image itself will
			// get the blank image.
			String blankImageURL = BundleUtil.getResolvedResourceLocation(
					IIntroHTMLConstants.IMAGE_SRC_BLANK, IIntroConstants.PLUGIN_ID);
			if (blankImageURL != null) {
				image.addAttribute(IIntroHTMLConstants.ATTRIBUTE_SRC, blankImageURL);
				if (pngOnWin32) {
					String style = "filter:progid:DXImageTransform.Microsoft.AlphaImageLoader(src='" + imageSrc + "', sizingMethod='image')"; //$NON-NLS-1$//$NON-NLS-2$
					image.addAttribute(IIntroHTMLConstants.ATTRIBUTE_STYLE, style);
				}
			}
		} else
			image.addAttribute(IIntroHTMLConstants.ATTRIBUTE_SRC, imageSrc);
		if (altText == null)
			altText = ""; //$NON-NLS-1$
		image.addAttribute(IIntroHTMLConstants.ATTRIBUTE_ALT, altText);
		if (title != null) {
			image.addAttribute(IIntroHTMLConstants.ATTRIBUTE_TITLE, title);
		}
		if (imageClass != null)
			image.addAttribute(IIntroHTMLConstants.ATTRIBUTE_CLASS, imageClass);
		return image;
	}

	/**
	 * Generate a span element
	 *
	 * <pre>
	 *
	 *                        &lt;SPAN class=spanClass&gt; &lt;/SPAN&gt;
	 *
	 *
	 * </pre>
	 *
	 * @param spanClass
	 *            the value to be supplied to the class attribute
	 * @param indentLevel
	 *            the number of indents to insert before the element when it is printed
	 * @return a span HTMLElement
	 */
	private HTMLElement generateSpanElement(String spanClass, int indentLevel) {
		HTMLElement span = new FormattedHTMLElement(IIntroHTMLConstants.ELEMENT_SPAN, indentLevel, false);
		span.addAttribute(IIntroHTMLConstants.ATTRIBUTE_CLASS, spanClass);
		return span;
	}

	/**
	 * Generate a span element
	 *
	 * <pre>
	 *
	 *                     &lt;iframe src=&quot;localPage1.xhtml&quot; frameborder=&quot;1&quot; scrolling=&quot;auto&quot; longdesc=&quot;localPage1.xhtml&quot;&gt;
	 * </pre>
	 *
	 * @param spanClass
	 *            the value to be supplied to the class attribute
	 * @param indentLevel
	 *            the number of indents to insert before the element when it is printed
	 * @return a span HTMLElement
	 */
	private HTMLElement generateIFrameElement(String src, String frameborder, String scrolling,
			int indentLevel) {
		HTMLElement iframe = new FormattedHTMLElement(IIntroHTMLConstants.ELEMENT_IFrame, indentLevel, false);
		if (src != null)
			iframe.addAttribute(IIntroHTMLConstants.ATTRIBUTE_SRC, src);
		if (frameborder != null)
			iframe.addAttribute(IIntroHTMLConstants.ATTRIBUTE_FRAMEBORDER, frameborder);
		if (scrolling != null)
			iframe.addAttribute(IIntroHTMLConstants.ATTRIBUTE_SCROLLING, scrolling);
		return iframe;
	}




	private boolean filteredFromPresentation(AbstractIntroElement element) {
		if (element.isOfType(AbstractIntroElement.BASE_ELEMENT))
			return ((AbstractBaseIntroElement) element).isFiltered();

		return false;
	}

	/**
	 * Reads the content of the file referred to by the <code>src</code> parameter and returns the
	 * content in the form of a StringBuilder. If the file read contains substitution segments of the
	 * form $plugin:plugin_id$ then this method will make the proper substitution (the segment will
	 * be replaced with the absolute path to the plugin with id plugin_id).
	 *
	 * @param src -
	 *            the file that contains the target conent
	 * @param charsetName -
	 *            the encoding of the file to be read. If null, local encoding is used. But the
	 *            default of the model is UTF-8, so we should not get a null encoding.
	 * @return a StringBuilder containing the content in the file, or null
	 */
	private StringBuilder readFromFile(String src, String charsetName) {
		if (src == null)
			return null;
		StringBuilder content = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(src).openStream(),
				(charsetName == null) ? Charset.defaultCharset().name() : charsetName))) {

			// TODO: Do we need to worry about the encoding here? e.g.:
			// reader = new BufferedReader(new InputStreamReader(stream,
			// ResourcesPlugin.getEncoding()));
			while (true) {
				int character = reader.read();
				if (character == -1) // EOF
					break; // done reading file

				else if (character == PluginIdParser.SUBSTITUTION_BEGIN) { // possible
					// substitution
					PluginIdParser parser = new PluginIdParser(character, reader);
					// If a valid plugin id was found in the proper format, text
					// will be the absolute path to that plugin. Otherwise, text
					// will simply be all characters read up to (but not
					// including)
					// the next dollar sign that follows the one just found.
					String text = parser.parsePluginId();
					if (text != null)
						content.append(text);
				} else {
					// make sure character is in char range before making cast
					if (character > 0x00 && character < 0xffff)
						content.append((char) character);
					else
						content.append(character);
				}
			}
		} catch (Exception exception) {
			Log.error("Error reading from file", exception); //$NON-NLS-1$
		}
		return content;
	}

	/**
	 * A helper class to help identify substitution strings in a content file. A properly formatted
	 * substitution string is of the form: <code>$plugin:plugin_id$</code> where plugin_id is the
	 * valid id of an installed plugin. The substitution string will be replaced with the absolute
	 * path to the plugin.
	 *
	 * An example usage of the string substution: The html file <code>inline.html</code> is
	 * included in your intro via the html inline mechanism . This file needs to reference a
	 * resource that is located in another plugin. The following might be found in inline.html:
	 * {@code
	 *    <a href="$plugin:test.plugin$html/test.html">link to file</a>
	 * } When this file
	 * is read in, the relevant section will be replaced as follows: {@code
	 *   <a href="file:/install_path/plugins/test.plugin/html/test.html">link to file</a>
	 * }
	 */
	private static class PluginIdParser {

		private BufferedReader reader;

		private static final char SUBSTITUTION_BEGIN = '$';

		private static final char SUBSTITUTION_END = '$';

		// tokenContent will contain all characters read by the parser, starting
		// with and including the initial $ token.
		private StringBuilder tokenContent;

		// pluginId will contain the content between the "$plugin:" segment
		// and the closing "$" token
		private StringBuilder pluginId;

		protected PluginIdParser(int tokenBegin, BufferedReader bufferedreader) {
			reader = bufferedreader;
			tokenContent = new StringBuilder();
			pluginId = new StringBuilder();
			// make sure tokenBegin is in char range before making cast
			if (tokenBegin > 0x00 && tokenBegin < 0xffff)
				tokenContent.append((char) tokenBegin);
		}

		/**
		 * This method should be called after the initial substitution identifier has been read in
		 * (the substition string begins and ends with the "$" character). A properly formatted
		 * substitution string is of the form: <code>"$plugin:plugin_id$</code>- the initial "$"
		 * is immediately followed by the "plugin:" segment - the <code>plugin_id </code> refers to
		 * a valid, installed plugin - the substitution string is terminated by a closing "$" If the
		 * above conditions are not met, no substitution occurs. If the above conditions are met,
		 * the content between (and including) the opening and closing "$" characters will be
		 * replaced by the absolute path to the plugin
		 *
		 * @return
		 */
		protected String parsePluginId() {
			if (reader == null || tokenContent == null || pluginId == null)
				return null;

			try {
				// Mark the current position of the reader so we can roll
				// back to this point if the proper "plugin:" segment is not
				// found.
				// Use 1024 as our readAheadLimit
				reader.mark(0x400);
				if (findValidPluginSegment()) {
					String pluginPath = getPluginPath();
					if (pluginPath == null) {
						// Didn't find a valid plugin id.
						// return tokenContent, which contains all characters
						// read up to (not including) the last $. (if the
						// last $ is part of a subsequent "$plugin:" segment
						// it can still be processed properly)
						return tokenContent.toString();
					}
					return pluginPath;
				}

				// The "plugin:" segment was not found. Reset the reader
				// so we can continue reading character by character.
				reader.reset();
				return tokenContent.toString();

			} catch (IOException exception) {
				Log.error("Error reading from file", exception); //$NON-NLS-1$
				return tokenContent.toString();
			}
		}

		/**
		 * This method should be called after an initial substitution character has been found (that
		 * is, after a $). It looks at the subsequent characters in the input stream to determine if
		 * they match the expected <code>plugin:</code> segment of the substitution string. If the
		 * expected characters are found, they will be appended to the tokenContent StringBuilder and
		 * the method will return true. If they are not found, false is returned and the caller
		 * should reset the BufferedReader to the position it was in before this method was called.
		 *
		 * Resetting the reader ensures that the characters read in this method can be re-examined
		 * in case one of them happens to be the beginning of a valid substitution segment.
		 *
		 * @return true if the next characters match <code>plugin:</code>, and false otherwise.
		 */
		private boolean findValidPluginSegment() {
			final char[] PLUGIN_SEGMENT = { 'p', 'l', 'u', 'g', 'i', 'n', ':' };
			char[] streamContent = new char[PLUGIN_SEGMENT.length];
			try {
				int peek = reader.read(streamContent, 0, PLUGIN_SEGMENT.length);
				if ((peek == PLUGIN_SEGMENT.length)
						&& (HTMLUtil.equalCharArrayContent(streamContent, PLUGIN_SEGMENT))) {
					// we have found the "$plugin:" segment
					tokenContent.append(streamContent);
					return true;
				}
				// The "plugin:" segment did not immediately follow the initial
				// $.
				return false;
			} catch (IOException exception) {
				Log.error("Error reading from file", exception); //$NON-NLS-1$
				return false;
			}
		}

		/**
		 * This method continues to read from the input stream until either the end of the file is
		 * reached, or until a character is found that indicates the end of the substitution. If the
		 * SUBSTITUTION_END character is found, the method looks up the plugin id that has been
		 * built up to see if it is a valid id. If so, return the absolute path to that plugin. If
		 * not, return null.
		 *
		 * This method assumes that the reader is positioned just after a valid <code>plugin:</code>
		 * segment in a substitution string.
		 *
		 * @return absolute path of the plugin id, if valid. null otherwise
		 */
		private String getPluginPath() {
			try {
				while (true) {
					int nextChar = reader.read();

					if (nextChar == -1) {
						// reached EOF while looking for closing $
						return null;
					} else if (nextChar == SUBSTITUTION_END) { // end of plugin
						// id
						// look up the plugin id. If it is a valid id
						// return the absolute path to this plugin.
						// otherwise return null.
						String path = BundleUtil.getResolvedBundleLocation(pluginId.toString());

						// If the plugin id was not valid, reset reader to the
						// previous mark. The mark should be at the character
						// just before the last dollar sign.
						if (path == null)
							reader.reset();

						return path;
					} else { // we have a regular character
						// mark the most recent non-dollar char in case we don't
						// find a valid plugin id and have to roll back
						// Use 1024 as our readAheadLimit
						reader.mark(0x400);
						// Add this character to the pluginId and tokenContent
						// String.
						// make sure we have a valid character before performing
						// cast
						if (nextChar > 0x00 && nextChar < 0xffff) {
							tokenContent.append((char) nextChar);
							// only include non-whitespace characters in plugin
							// id
							if (!Character.isWhitespace((char) nextChar))
								pluginId.append((char) nextChar);
						} else {
							tokenContent.append(nextChar);
							pluginId.append(nextChar);
						}
					}
				}
			} catch (IOException exception) {
				Log.error("Error reading from file", exception); //$NON-NLS-1$
				return null;
			}
		}
	}

}
