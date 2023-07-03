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
package org.eclipse.ui.internal.intro.impl.model.loader;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.ui.internal.intro.impl.model.AbstractIntroPage;
import org.eclipse.ui.internal.intro.impl.model.IntroContentProvider;
import org.eclipse.ui.internal.intro.impl.util.Log;
import org.eclipse.ui.intro.config.IIntroContentProvider;
import org.eclipse.ui.intro.config.IIntroContentProviderSite;

/**
 * Class for handling/caching all the loaded Intro Content providers, from all loaded models.
 * <p>
 * Design notes:
 * <ul>
 * <li>content providers are only ever created once. The init method is only called once, and so
 * this is why they need to be cached.</li>
 * <li>Content provider ids are used as keys in the hashtable, and their corresponding wrapper
 * classes as values.</li>
 * <li>In the case of HTML presentation, html is cached and so model is not consuloted when we need
 * to redisplay a page. When content provider asks for a reflow, the page is removed from HTML cache
 * and intro model is consulted again. This is when the calls happen to this class.</li>
 * <li>In the case of SWT presentation, same design. SWT pages are cached in a page book. When a
 * content provider needs to refresh a page, the page is removed from the page book and recreated
 * from intro model.</li>
 * </ul>
 */

public class ContentProviderManager {

	// singleton instance. Can be retrieved from here or from the Intro Plugin.
	private static ContentProviderManager inst = new ContentProviderManager();


	// Holds all created content providers, to prevent the need to recreate the
	// class on each navigation. Key is the contentProvider id, value
	// is a wrapper class to hold the actual Intro content provider instance and
	// the intro page that holds it.
	private Map<String, ContentProviderWrapper> contentProviders = new HashMap<>();


	static class ContentProviderWrapper {

		IIntroContentProvider provider;
		AbstractIntroPage parentPage;

		ContentProviderWrapper(IIntroContentProvider provider, AbstractIntroPage parentPage) {
			this.provider = provider;
			this.parentPage = parentPage;
		}

		IIntroContentProvider getIIntroContentProvider() {
			return provider;
		}

		AbstractIntroPage getParentPage() {
			return parentPage;
		}
	}



	/*
	 * Prevent creation.
	 */
	protected ContentProviderManager() {
		// do nothing
	}

	/**
	 * @return Returns the inst.
	 */
	public static ContentProviderManager getInst() {
		return inst;
	}

	/**
	 * Retrieve an existing content provider class, or null if never created before.
	 *
	 * @param provider
	 * @return
	 */
	public IIntroContentProvider getContentProvider(IntroContentProvider provider) {
		// safe to cast since we know the object class in table.
		ContentProviderWrapper providerWrapper = contentProviders.get(provider.getId());
		if (providerWrapper == null)
			// return null if provider has not been created yet.
			return null;
		IIntroContentProvider providerClass = providerWrapper.getIIntroContentProvider();
		return providerClass;
	}

	/**
	 * Tries to create an intro content provider class. may return null if creation fails. This will
	 * be logged.
	 *
	 * @param provider
	 * @param site
	 * @return
	 */
	public IIntroContentProvider createContentProvider(IntroContentProvider provider,
			IIntroContentProviderSite site) {

		// the content provider has never been created before. Create and cache
		// one.
		String pluginId = (provider.getPluginId() != null) ? provider.getPluginId() : provider.getBundle()
				.getSymbolicName();
		Object aClass = ModelLoaderUtil.createClassInstance(pluginId, provider.getClassName());
		IIntroContentProvider providerClass = null;
		if (aClass != null && aClass instanceof IIntroContentProvider) {
			providerClass = ((IIntroContentProvider) aClass);
			providerClass.init(site);
			if (provider.getId() != null) {
				// cache only when an id is defined.
				ContentProviderWrapper wrapper = new ContentProviderWrapper(providerClass, provider
						.getParentPage());
				contentProviders.put(provider.getId(), wrapper);
			}
		} else
			Log.warning("Failed to create Intro model content provider: " //$NON-NLS-1$
					+ provider.getClassName());
		return providerClass;
	}


	public AbstractIntroPage getContentProviderParentPage(IIntroContentProvider provider) {
		for (ContentProviderWrapper wrapper : contentProviders.values()) {
			boolean foundKey = wrapper.getIIntroContentProvider().equals(provider) ? true : false;
			if (foundKey) {
				return wrapper.getParentPage();
			}
		}
		return null;
	}

	public void clear() {
		for (Iterator it = contentProviders.values().iterator(); it.hasNext();) {
			ContentProviderWrapper providerWrapper = (ContentProviderWrapper) it.next();
			IIntroContentProvider provider = providerWrapper.getIIntroContentProvider();
			provider.dispose();
		}
		contentProviders.clear();
		if (Log.logInfo)
			Log.info("Cleared Intro model content providers"); //$NON-NLS-1$
	}


}
