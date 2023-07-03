/*******************************************************************************
 *  Copyright (c) 2004, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ua.tests.intro.util;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.eclipse.ui.internal.intro.impl.model.AbstractIntroContainer;
import org.eclipse.ui.internal.intro.impl.model.AbstractIntroElement;
import org.eclipse.ui.internal.intro.impl.model.AbstractIntroPage;
import org.eclipse.ui.internal.intro.impl.model.IntroAnchor;
import org.eclipse.ui.internal.intro.impl.model.IntroContentProvider;
import org.eclipse.ui.internal.intro.impl.model.IntroGroup;
import org.eclipse.ui.internal.intro.impl.model.IntroHTML;
import org.eclipse.ui.internal.intro.impl.model.IntroHead;
import org.eclipse.ui.internal.intro.impl.model.IntroHomePage;
import org.eclipse.ui.internal.intro.impl.model.IntroImage;
import org.eclipse.ui.internal.intro.impl.model.IntroInclude;
import org.eclipse.ui.internal.intro.impl.model.IntroLink;
import org.eclipse.ui.internal.intro.impl.model.IntroModelRoot;
import org.eclipse.ui.internal.intro.impl.model.IntroPage;
import org.eclipse.ui.internal.intro.impl.model.IntroPageTitle;
import org.eclipse.ui.internal.intro.impl.model.IntroText;
import org.osgi.framework.Bundle;

public class IntroModelSerializer {

	private StringBuilder buffer;

	public IntroModelSerializer(IntroModelRoot root) {
		this.buffer = new StringBuilder();
		printModelRootInfo(root, buffer);

		// Root Page
		IntroHomePage rootPage = root.getRootPage();
		printHomePage(rootPage, buffer);
		printPageChildren(rootPage, buffer);

		IntroPage[] pages = root.getPages();
		printPages(pages, buffer);

		buffer.append("\n\n"); //$NON-NLS-1$
		printModelFlagTests(root, buffer);
	}

	/*
	 * Removes any platform/machine-specific paths from the given URL. This is
	 * used to remove absolute paths from the serialized model in order to compare
	 * results in automated tests.
	 */
	private String filterURL(String url) {
		if (url != null && url.startsWith("file:/")) {
			return "file:/" + "<filtered>" + url.substring(url.lastIndexOf('/'));
		}
		return url;
	}

	private void printModelRootInfo(IntroModelRoot model, StringBuilder text) {
		text.append("Intro Model Content:"); //$NON-NLS-1$
		text.append("\n======================"); //$NON-NLS-1$
		text.append("\n\nModel has valid config = " + model.hasValidConfig()); //$NON-NLS-1$
		text.append("\nPresentation type = " //$NON-NLS-1$
				+ model.getPresentation().getType());
		text.append("\nHome page id = " //$NON-NLS-1$
				+ model.getPresentation().getHomePageId());
		IntroHead headContent = model.getPresentation().getHead();
		if (headContent != null)
			text.append("\nPresentation Shared Head = " + headContent.getSrc()); //$NON-NLS-1$
		text.append("\nNumber of pages (not including Root Page) = " //$NON-NLS-1$
				+ model.getPages().length);
		text.append("\nNumber of shared groups = " //$NON-NLS-1$
				+ model.getChildrenOfType(AbstractIntroElement.GROUP).length);
		text
			.append("\nNumber of unresolved extensions = " //$NON-NLS-1$
					+ model
						.getChildrenOfType(AbstractIntroElement.CONTAINER_EXTENSION).length);
	}

	/**
	 * @param text
	 * @param root
	 */
	private void printHomePage(AbstractIntroPage rootPage, StringBuilder text) {
		text.append("\n\nHOME PAGE: "); //$NON-NLS-1$
		text.append("\n--------------"); //$NON-NLS-1$
		text
			.append("\n\tis dynamic= " + ((IntroModelRoot) rootPage.getParent()).isDynamic()); //$NON-NLS-1$

		text.append("\n\tid = " + rootPage.getId()); //$NON-NLS-1$
		text.append("\n\ttitle = " + rootPage.getTitle()); //$NON-NLS-1$
		text.append("\n\tstyle = " + filterURL(rootPage.getStyle())); //$NON-NLS-1$
		text.append("\n\talt-style = " + filterURL(rootPage.getAltStyle())); //$NON-NLS-1$
		text.append("\n\turl = " + filterURL(rootPage.getUrl())); //$NON-NLS-1$
		text.append("\n\tstyle-id = " + rootPage.getStyleId()); //$NON-NLS-1$
		printPageStyles(rootPage, text);
	}

	private void printPageStyles(AbstractIntroPage page, StringBuilder text) {
		text.append("\n\tpage styles are = "); //$NON-NLS-1$
		String[] styles = page.getStyles();
		for (String style : styles)
			text.append(filterURL(style + "\n\t\t\t")); //$NON-NLS-1$
		text.append("\n\tpage alt-styles are = "); //$NON-NLS-1$

		final Map<String, Bundle> altStylesHashtable = page.getAltStyles();
		if (altStylesHashtable == null)
			return;

		Set<String> set = altStylesHashtable.keySet();
		String[] sorted = new String[set.size()];
		set.toArray(sorted);
		for (int i=0;i<sorted.length;++i) {
			Bundle bundle = altStylesHashtable.get(sorted[i]);
			sorted[i] = filterURL(sorted[i]) + " from " + bundle.getSymbolicName(); //$NON-NLS-1$
		}
		Arrays.sort(sorted);

		for (String element : sorted) {
			text.append(element + "\n\t\t"); //$NON-NLS-1$
		}
	}

	private void printPageChildren(AbstractIntroPage page, StringBuilder text) {

		text.append("\n\tpage children = " + page.getChildren().length); //$NON-NLS-1$
		text.append("\n"); //$NON-NLS-1$
		printContainerChildren(page, text, "\n\t\t"); //$NON-NLS-1$

	}

	private void printContainerChildren(AbstractIntroContainer container,
			StringBuilder text, String indent) {

		AbstractIntroElement[] children = container.getChildren();
		for (AbstractIntroElement element : children) {
			int childType = element.getType();
			switch (childType) {
			case AbstractIntroElement.ELEMENT:
				text.append("SHOULD NEVER BE HERE"); //$NON-NLS-1$
				break;
			case AbstractIntroElement.GROUP:
				printGroup(text, (IntroGroup) element, indent);
				break;
			case AbstractIntroElement.LINK:
				printLink(text, (IntroLink) element, indent);
				break;
			case AbstractIntroElement.TEXT:
				printText(text, (IntroText) element, indent);
				break;
			case AbstractIntroElement.IMAGE:
				printImage(text, (IntroImage) element, indent);
				break;
			case AbstractIntroElement.HTML:
				printHtml(text, (IntroHTML) element, indent);
				break;
			case AbstractIntroElement.INCLUDE:
				printInclude(text, (IntroInclude) element, indent);
				break;
			case AbstractIntroElement.HEAD:
				printHead(text, (IntroHead) element, indent);
				break;
			case AbstractIntroElement.PAGE_TITLE:
				printPageTitle(text, (IntroPageTitle) element, indent);
				break;
			case AbstractIntroElement.ANCHOR:
				printAnchor(text, (IntroAnchor) element, indent);
				break;
			case AbstractIntroElement.CONTENT_PROVIDER:
				printContentProvidor(text, (IntroContentProvider) element,
					indent);
				break;

			}
		}
	}

	private void printGroup(StringBuilder text, IntroGroup group, String indent) {
		text.append(indent + "GROUP: id = " + group.getId()); //$NON-NLS-1$
		indent = indent + "\t\t"; //$NON-NLS-1$
		text.append(indent + "label = " + group.getLabel()); //$NON-NLS-1$
		text.append(indent + "children = " + group.getChildren().length); //$NON-NLS-1$
		text.append(indent + "style-id = " + group.getStyleId()); //$NON-NLS-1$
		printContainerChildren(group, text, indent + "\t\t"); //$NON-NLS-1$
	}

	private void printLink(StringBuilder text, IntroLink link, String indent) {
		text.append(indent + "LINK: id = " + link.getId()); //$NON-NLS-1$
		indent = indent + "\t\t"; //$NON-NLS-1$
		text.append(indent + "label = " + link.getLabel()); //$NON-NLS-1$
		text.append(indent + "text = " + link.getText()); //$NON-NLS-1$
		text.append(indent + "url = " + link.getUrl()); //$NON-NLS-1$
		text.append(indent + "style-id = " + link.getStyleId()); //$NON-NLS-1$
	}

	private void printText(StringBuilder text, IntroText introText, String indent) {
		text.append(indent + "TEXT: id = " + introText.getId()); //$NON-NLS-1$
		indent = indent + "\t\t"; //$NON-NLS-1$
		text.append(indent + "text = " + introText.getText()); //$NON-NLS-1$
		text.append(indent + "style-id = " + introText.getStyleId()); //$NON-NLS-1$
	}

	private void printImage(StringBuilder text, IntroImage image, String indent) {
		text.append(indent + "IMAGE: id = " + image.getId()); //$NON-NLS-1$
		indent = indent + "\t\t"; //$NON-NLS-1$
		text.append(indent + "src = " + image.getSrc()); //$NON-NLS-1$
		text.append(indent + "alt = " + image.getAlt()); //$NON-NLS-1$
		text.append(indent + "style-id = " + image.getStyleId()); //$NON-NLS-1$
	}

	private void printHtml(StringBuilder text, IntroHTML html, String indent) {
		text.append(indent + "HTML: id = " + html.getId()); //$NON-NLS-1$
		indent = indent + "\t\t"; //$NON-NLS-1$
		text.append(indent + "src = " + html.getSrc()); //$NON-NLS-1$
		text.append(indent + "isInlined = " + html.isInlined()); //$NON-NLS-1$
		text.append(indent + "style-id = " + html.getStyleId()); //$NON-NLS-1$
		if (html.getIntroImage() != null)
			printImage(text, html.getIntroImage(), indent + "\t\t"); //$NON-NLS-1$
		if (html.getIntroText() != null)
			printText(text, html.getIntroText(), indent + "\t\t"); //$NON-NLS-1$

	}

	private void printInclude(StringBuilder text, IntroInclude include,
			String indent) {
		text.append(indent + "INCLUDE: configId = " + include.getConfigId()); //$NON-NLS-1$
		indent = indent + "\t\t"; //$NON-NLS-1$
		text.append(indent + "path = " + include.getPath()); //$NON-NLS-1$
		text.append(indent + "merge-style = " + include.getMergeStyle()); //$NON-NLS-1$
	}

	private void printHead(StringBuilder text, IntroHead head, String indent) {
		text.append(indent + "HEAD: src = " + head.getSrc()); //$NON-NLS-1$
	}

	private void printPageTitle(StringBuilder text, IntroPageTitle title,
			String indent) {
		text.append(indent + "TITLE: id = " + title.getId()); //$NON-NLS-1$
		indent = indent + "\t\t"; //$NON-NLS-1$
		text.append(indent + "title = " + title.getTitle()); //$NON-NLS-1$
		text.append(indent + "style-id = " + title.getStyleId()); //$NON-NLS-1$
	}

	private void printAnchor(StringBuilder text, IntroAnchor anchor,
			String indent) {
		text.append(indent + "ANCHOR: id = " + anchor.getId()); //$NON-NLS-1$
	}

	private void printContentProvidor(StringBuilder text,
			IntroContentProvider provider, String indent) {
		text.append(indent + "CONTENT PROVIDER: id = " + provider.getId()); //$NON-NLS-1$
		indent = indent + "\t\t"; //$NON-NLS-1$
		text.append(indent + "class = " + provider.getClassName()); //$NON-NLS-1$
		text.append(indent + "pluginId = " + provider.getPluginId()); //$NON-NLS-1$
		if (provider.getIntroText() != null)
			printText(text, provider.getIntroText(), indent + "\t\t"); //$NON-NLS-1$

	}

	/**
	 * Appends a given page's categories to the Text buffer.
	 *
	 * @param text
	 */
	private void printPages(IntroPage[] pages, StringBuilder text) {
		for (IntroPage page : pages) {
			text.append("\n\nPAGE id = " + page.getId()); //$NON-NLS-1$
			text.append("\n----------"); //$NON-NLS-1$
			text.append("\n\ttitle = " + page.getTitle()); //$NON-NLS-1$
			text.append("\n\tstyle = " + filterURL(page.getStyle())); //$NON-NLS-1$
			text.append("\n\talt-style = " + filterURL(page.getAltStyle())); //$NON-NLS-1$
			text.append("\n\tstyle-id = " + page.getStyleId()); //$NON-NLS-1$
			printPageStyles(page, text);
			printPageChildren(page, text);
		}
	}

	private void printModelFlagTests(IntroModelRoot model, StringBuilder text) {
		text.append("Model Flag Tests: "); //$NON-NLS-1$
		text.append("\n----------------"); //$NON-NLS-1$
		if (model.getPages().length == 0) {
			text.append("\nNo first page in model\n\n"); //$NON-NLS-1$
			return;
		}
		IntroPage firstPage = model.getPages()[0];
		if ("standby".equals(firstPage.getId())) {
			firstPage = model.getPages()[1];
		}
		text.append("\n\t\tFirst page children are: "); //$NON-NLS-1$
		text
			.append("\n\t\t\tGroups: " //$NON-NLS-1$
					+ firstPage.getChildrenOfType(AbstractIntroElement.GROUP).length);
		text
			.append("\n\t\t\tLinks: " //$NON-NLS-1$
					+ firstPage.getChildrenOfType(AbstractIntroElement.LINK).length);
		text
			.append("\n\t\t\tTexts: " //$NON-NLS-1$
					+ firstPage.getChildrenOfType(AbstractIntroElement.TEXT).length);
		text
			.append("\n\t\t\tHTMLs: " //$NON-NLS-1$
					+ firstPage.getChildrenOfType(AbstractIntroElement.HTML).length);
		text
			.append("\n\t\t\tImages: " //$NON-NLS-1$
					+ firstPage.getChildrenOfType(AbstractIntroElement.IMAGE).length);
		text
			.append("\n\t\t\tIncludes: " //$NON-NLS-1$
					+ firstPage.getChildrenOfType(AbstractIntroElement.INCLUDE).length);
		text
			.append("\n\t\t\tPage Titles: " //$NON-NLS-1$
					+ firstPage
						.getChildrenOfType(AbstractIntroElement.PAGE_TITLE).length);
		text
			.append("\n\t\t\tPage Heads: " //$NON-NLS-1$
					+ firstPage.getChildrenOfType(AbstractIntroElement.HEAD).length);
		text
			.append("\n\t\t\tModel Elements: " //$NON-NLS-1$
					+ firstPage.getChildrenOfType(AbstractIntroElement.ELEMENT).length);
		text
			.append("\n\t\t\tContainers: " //$NON-NLS-1$
					+ firstPage
						.getChildrenOfType(AbstractIntroElement.ABSTRACT_CONTAINER).length);
		text
			.append("\n\t\t\tAll Pages: " //$NON-NLS-1$
					+ firstPage
						.getChildrenOfType(AbstractIntroElement.ABSTRACT_PAGE).length);
		text
			.append("\n\t\t\tAnchors: " //$NON-NLS-1$
					+ firstPage.getChildrenOfType(AbstractIntroElement.ANCHOR).length);
		text
			.append("\n\t\t\tContent providers: " //$NON-NLS-1$
					+ firstPage
						.getChildrenOfType(AbstractIntroElement.CONTENT_PROVIDER).length);
		text
			.append("\n\t\t\tElements with Text child(AbstractTextElemets): " //$NON-NLS-1$
					+ firstPage
						.getChildrenOfType(AbstractIntroElement.ABSTRACT_TEXT).length);

		AbstractIntroElement[] linksAndGroups = (AbstractIntroElement[]) firstPage
			.getChildrenOfType(AbstractIntroElement.GROUP
					| AbstractIntroElement.LINK);
		text.append("\n\t\t\tGroups and Links: " + linksAndGroups.length); //$NON-NLS-1$
	}

	/**
	 * @return Returns the textUI.
	 */
	@Override
	public String toString() {
		return buffer.toString();
	}
}
