/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.help.ui;

import java.util.Dictionary;

import org.eclipse.help.search.ISearchScope;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Creates search scope objects from the provided preference store. Classes that
 * implement this interface should take settings manipulated in the search scope
 * preference pages and create scope objects that are required by search
 * engines. Search engine and scope factory are defined together in the same
 * extension point, hence the actual implementation of ISearchScope is up to the
 * contributor.
 *
 * @since 3.1
 */
public interface ISearchScopeFactory {
	/**
	 * Returns a new search scope object created from the data in the preference
	 * store. Factories should be prepared to compute default values if they are
	 * missing from the preference store. This can happen before users open
	 * scope preference pages for the first time.
	 * <p>
	 * In cases where conflicting values can be found in the preference store
	 * and parameters, preference store should win i.e. parameters should be
	 * treated as default values only.
	 *
	 * @param store
	 *            the preference store that holds the scope data
	 * @param engineId
	 *            identifier of the engine instance that needs the scope object.
	 * @param parameters
	 *            configuration parameters provided in the engine extension
	 *            point. They should be used as default values and preference
	 *            store values (if defined) should be given precedance.
	 * @return a new search scope object
	 */
	ISearchScope createSearchScope(IPreferenceStore store, String engineId,
			Dictionary<String, Object> parameters);
}
