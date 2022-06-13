/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
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
package org.eclipse.core.tests.internal.preferences;

import java.util.*;
import org.eclipse.core.runtime.preferences.AbstractPreferenceStorage;

public class TestNodeStorage extends AbstractPreferenceStorage {

	private Map<String, Properties> storage = new HashMap<>();

	@Override
	public Properties load(String nodePath) {
		return storage.get(nodePath);
	}

	@Override
	public void save(String nodePath, Properties properties) {
		storage.put(nodePath, properties);
	}

	@Override
	public String[] childrenNames(String nodePath) {
		return null;
	}

	@Override
	public void removed(String nodePath) {
		storage.remove(nodePath);
	}

}
