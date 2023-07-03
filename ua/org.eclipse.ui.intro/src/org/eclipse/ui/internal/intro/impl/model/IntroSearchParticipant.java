/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
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

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.help.search.IHelpSearchIndex;
import org.eclipse.help.search.ISearchDocument;
import org.eclipse.help.search.SearchParticipant;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.intro.impl.IntroPlugin;
import org.eclipse.ui.internal.intro.impl.model.loader.ExtensionPointManager;
import org.eclipse.ui.intro.IIntroManager;
import org.eclipse.ui.intro.IIntroPart;
import org.eclipse.ui.intro.config.IIntroURL;
import org.eclipse.ui.intro.config.IntroURLFactory;
import org.osgi.framework.Bundle;

/**
 * An implementation of SearchParticipant that adds Welcome content into the local help
 * index so that it can be searched.
 *
 */

public class IntroSearchParticipant extends SearchParticipant {

	private IntroModelRoot model;

	private static class TitleAndSummary {
		String title;
		String summary;
	}

	@Override
	public Set<String> getContributingPlugins() {
		IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(
				"org.eclipse.ui.intro.config"); //$NON-NLS-1$
		Set<String> set = new HashSet<>();
		for (int i = 0; i < elements.length; i++) {
			IConfigurationElement element = elements[i];
			if (!element.getName().equals("config")) //$NON-NLS-1$
				continue;
			set.add(element.getContributor().getName());
		}
		elements = Platform.getExtensionRegistry().getConfigurationElementsFor(
				"org.eclipse.ui.intro.configExtension"); //$NON-NLS-1$
		for (int i = 0; i < elements.length; i++) {
			IConfigurationElement element = elements[i];
			if (!element.getName().equals("configExtension")) //$NON-NLS-1$
				continue;
			set.add(element.getContributor().getName());
		}
		return set;
	}

	@Override
	public Set<String> getAllDocuments(String locale) {
		HashSet<String> set = new HashSet<>();
		IProduct product = Platform.getProduct();
		if (product == null) {
			return set;
		}
		String productId = product.getId();
		IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(
				"org.eclipse.ui.intro"); //$NON-NLS-1$
		String targetIntroId = null;
		for (int i = 0; i < elements.length; i++) {
			IConfigurationElement element = elements[i];
			if (element.getName().equals("introProductBinding")) { //$NON-NLS-1$
				String pid = element.getAttribute("productId"); //$NON-NLS-1$
				String iid = element.getAttribute("introId"); //$NON-NLS-1$
				if (productId.equals(pid)) {
					targetIntroId = iid;
					break;
				}
			}
		}
		if (targetIntroId == null)
			return set;
		elements = Platform.getExtensionRegistry().getConfigurationElementsFor("org.eclipse.ui.intro.config"); //$NON-NLS-1$
		IConfigurationElement config = null;
		for (int i = 0; i < elements.length; i++) {
			IConfigurationElement element = elements[i];
			if (element.getName().equals("config")) { //$NON-NLS-1$
				String iid = element.getAttribute("introId"); //$NON-NLS-1$
				if (targetIntroId.equals(iid)) {
					config = element;
					break;
				}
			}
		}
		if (config == null)
			return set;
		String configId = config.getAttribute("id"); //$NON-NLS-1$
		ExtensionPointManager extensionPointManager = IntroPlugin.getDefault().getExtensionPointManager();
		model = extensionPointManager.getModel(configId);
		if (model != null && model.hasValidConfig())
			loadFromModel(model, set, locale);
		return set;
	}

	private void loadFromModel(IntroModelRoot model, Set<String> set, String locale) {
		IntroPage[] pages = model.getPages();
		for (int i = 0; i < pages.length; i++) {
			IntroPage page = pages[i];
			if (page.isDynamic()) {
				Bundle bundle = page.getBundle();
				String bundleId = bundle.getSymbolicName();
				String content = page.getRawContent();
				String pageId = page.getId();
				String href;
				if (content != null)
					href = resolveVariables(bundleId, content, locale);
				else
					href = pageId;
				set.add("/" + bundleId + "/" + href + "?id=" + pageId); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
	}

	@Override
	public IStatus addDocument(IHelpSearchIndex index, String pluginId, String name, URL url, String id,
			ISearchDocument doc) {
		if (model == null)
			return Status.CANCEL_STATUS;
		IntroPage page = getPage(id);
		if (page == null)
			return Status.CANCEL_STATUS;
		return addPage(index, pluginId, name, url, page, doc);
	}

	private IntroPage getPage(String id) {
		IntroPage[] pages = model.getPages();
		for (int i = 0; i < pages.length; i++) {
			if (pages[i].getId().equals(id))
				return pages[i];
		}
		return null;
	}

	private IStatus addPage(IHelpSearchIndex index, String pluginId, String name, URL url, IntroPage page,
			ISearchDocument doc) {
		AbstractIntroElement[] children = page.getChildren();
		if (children.length > 0) {
			StringBuilder buf = new StringBuilder();
			TitleAndSummary titleSummary = new TitleAndSummary();
			addChildren(children, buf, doc, titleSummary);
			String contents = buf.toString();
			if (titleSummary.title != null) {
				 addTitle(titleSummary.title, doc);
			}
			if (titleSummary.summary != null) {
				doc.setSummary(titleSummary.summary);
			}
			doc.addContents(contents);
			return Status.OK_STATUS;
		}
		// delegate to the help system
		return index.addSearchableDocument(pluginId, name, url, page.getId(), doc);
	}

	private void addChildren(AbstractIntroElement[] children, StringBuilder buf, ISearchDocument doc, TitleAndSummary titleSummary) {
		for (int i = 0; i < children.length; i++) {
			AbstractIntroElement child = children[i];
			if (child instanceof IntroLink) {
				String text = ((IntroLink)child).getLabel();
				appendNewText(buf, text);
			} else if (child instanceof IntroGroup) {
				String text = ((IntroGroup)child).getLabel();
				appendNewText(buf, text);
			} else if (child instanceof IntroText) {
				IntroText childIntroText = (IntroText) child;
				appendNewText(buf, childIntroText.getText());
				String childId = childIntroText.getId();
				String title = null;
				if ("page-title".equals(childId)) { //$NON-NLS-1$
					title = childIntroText.getText();
				} else if (child instanceof IntroPageTitle) {
					title = ((IntroPageTitle) child).getTitle();
				}
				if (title != null) {
					titleSummary.title = title;
				}
				if  ("page-description".equals(childId)) { //$NON-NLS-1$
					titleSummary.summary = childIntroText.getText();
				}
			}
			if (child instanceof AbstractIntroContainer) {
				AbstractIntroContainer container = (AbstractIntroContainer) child;
				if (!"navigation-links".equals(container.getId())) { //$NON-NLS-1$
					AbstractIntroElement[] cc = container.getChildren();
					addChildren(cc, buf, doc, titleSummary);
				}
			}
		}
	}

	private void appendNewText(StringBuilder buf, String text) {
		if (text == null) return;
		if (buf.length() > 0)
			buf.append(" "); //$NON-NLS-1$
		buf.append(text);
	}

	@Override
	public void clear() {
		model = null;
	}

	@Override
	public boolean open(String id) {
		IIntroManager introManager = PlatformUI.getWorkbench().getIntroManager();
		IIntroPart intro = introManager
				.showIntro(PlatformUI.getWorkbench().getActiveWorkbenchWindow(), false);
		if (intro == null)
			return false;
		IIntroURL url = IntroURLFactory.createIntroURL("http://org.eclipse.ui.intro/showPage?id=" + id); //$NON-NLS-1$
		return url.execute();
	}

}
