/*******************************************************************************
 * Copyright (c) 2010, 2020 IBM Corporation and others.
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
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 560168
 *******************************************************************************/

package org.eclipse.help.internal.criteria;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.AbstractCriteriaProvider;
import org.eclipse.help.ICriteria;
import org.eclipse.help.IToc;
import org.eclipse.help.IToc2;
import org.eclipse.help.ITopic;
import org.eclipse.help.ITopic2;

public class CriteriaProviderRegistry {

	public static final String PROVIDER_XP_NAME = "org.eclipse.help.criteriaProvider"; //$NON-NLS-1$

	private static List<AbstractCriteriaProvider> providers = null;

	private boolean initialized = false;

	private static class RegistryHolder {
		static final CriteriaProviderRegistry instance = new CriteriaProviderRegistry();
	}

	private CriteriaProviderRegistry() {
	}

	public static CriteriaProviderRegistry getInstance() {
		return RegistryHolder.instance;
	}

	synchronized private void readProviders() {
		if (initialized ) {
			return;
		}
		providers = new ArrayList<>();
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] elements = registry
				.getConfigurationElementsFor(PROVIDER_XP_NAME);
		for (int i = 0; i < elements.length; i++) {

			Object obj = null;
			try {
				obj = elements[i].createExecutableExtension("class"); //$NON-NLS-1$
			} catch (CoreException e) {
				ILog.of(getClass()).error("Create extension failed:[" //$NON-NLS-1$
						+ PROVIDER_XP_NAME + "].", e); //$NON-NLS-1$
			}
			if (obj instanceof AbstractCriteriaProvider) {
				providers.add((AbstractCriteriaProvider) obj);
			}
		}
		initialized = true;
	}

	public AbstractCriteriaProvider[] getScopes() {
		readProviders();
		return providers.toArray(new AbstractCriteriaProvider[providers.size()]);
	}

	public ICriteria[] getAllCriteria(ITopic topic) {
		readProviders();
		ICriteria[] criteria;
		if (topic instanceof ITopic2) {
			criteria = ((ITopic2) topic).getCriteria();
		} else {
			criteria = new ICriteria[0];
		}
		for (Iterator<AbstractCriteriaProvider> iter = providers.iterator(); iter.hasNext();) {
			AbstractCriteriaProvider provider = iter.next();
			ICriteria[] newCriteria = provider.getCriteria(topic);
			if (newCriteria.length > 0) {
				if (criteria.length == 0) {
					criteria = newCriteria;
				} else {
					ICriteria[] union = new ICriteria[criteria.length + newCriteria.length];
					System.arraycopy(criteria, 0, union, 0, criteria.length);
					System.arraycopy(newCriteria, 0, union, criteria.length, newCriteria.length);
					criteria = union;
				}
			}
		}
		return criteria;
	}

	public ICriteria[] getAllCriteria(IToc toc) {
		readProviders();
		ICriteria[] criteria;
		if (toc instanceof IToc2) {
			criteria = ((IToc2) toc).getCriteria();
		} else {
			criteria = new ICriteria[0];
		}
		for (Iterator<AbstractCriteriaProvider> iter = providers.iterator(); iter.hasNext();) {
			AbstractCriteriaProvider provider = iter.next();
			ICriteria[] newCriteria = provider.getCriteria(toc);
			if (newCriteria.length > 0) {
				if (criteria.length == 0) {
					criteria = newCriteria;
				} else {
					ICriteria[] union = new ICriteria[criteria.length + newCriteria.length];
					System.arraycopy(criteria, 0, union, 0, criteria.length);
					System.arraycopy(newCriteria, 0, union, criteria.length, newCriteria.length);
					criteria = union;
				}
			}
		}
		return criteria;
	}

}
