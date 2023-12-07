/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *     Alexander Kurtakov <akurtako@redhat.com> - Bug 459343
 *******************************************************************************/
package org.eclipse.core.tests.resources.regression;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.buildResources;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.removeFromWorkspace;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.ResourceTest;

public class NLTest extends ResourceTest {

	public void getFileNames(List<String> list, char begin, char end) {
		char current = begin;
		int index = 0;
		StringBuilder name = new StringBuilder();
		name.append(((int) current) + "_");
		while (current <= end) {
			if (!Character.isLetterOrDigit(current)) {
				current++;
				continue;
			}
			name.append(current);
			index++;
			current++;
			if (index == 10) {
				list.add(name.toString());
				index = 0;
				name.setLength(0);
				name.append(((int) current) + "_");
			}
		}
		if (name.length() > 0) {
			list.add(name.toString());
		}
	}

	public String[] getFileNames(String language) {
		List<String> names = new ArrayList<>(20);
		if (language.equalsIgnoreCase("en")) { // English
			getFileNames(names, '\u0041', '\u005A'); // A - Z
			getFileNames(names, '\u0061', '\u007A'); // a - z
		} else if (language.equalsIgnoreCase("ja")) { // Japanese
			getFileNames(names, '\u3040', '\u3093'); // Hiragana
			// we are skipping \u3094
			getFileNames(names, '\u3095', '\u309F'); // Hiragana
			getFileNames(names, '\u30A0', '\u30F6'); // Katakana
			// we are skipping \u30F7 to \u30FA
			getFileNames(names, '\u30FB', '\u30FF'); // Katakana
		} else if (language.equalsIgnoreCase("de") || // German
				language.equalsIgnoreCase("pt")) { // Portuguese
			getFileNames(names, '\u00C0', '\u00FF'); // Latin-1 supplement
		} else if (language.equalsIgnoreCase("he") || // Hebrew
				language.equalsIgnoreCase("iw")) { // Hebrew
			getFileNames(names, '\u0590', '\u05FF');
		}
		return names.toArray(new String[names.size()]);
	}

	public void testFileNames() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("project");
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		String[] files = getFileNames(Locale.ENGLISH.getLanguage());
		IResource[] resources = buildResources(project, files);
		createInWorkspace(resources);
		project.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		assertExistsInFileSystem(resources);
		assertExistsInWorkspace(resources);
		removeFromWorkspace(resources);

		files = getFileNames(Locale.getDefault().getLanguage());
		resources = buildResources(project, files);
		createInWorkspace(resources);
		project.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		assertExistsInFileSystem(resources);
		assertExistsInWorkspace(resources);
	}

}
