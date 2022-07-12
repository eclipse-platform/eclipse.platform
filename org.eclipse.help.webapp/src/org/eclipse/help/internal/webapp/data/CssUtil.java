/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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
package org.eclipse.help.internal.webapp.data;

import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.Platform;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.util.ProductPreferences;

/**
 * Utility class for parsing the CSS preferences
 */
public class CssUtil {

	private static final String cssLink1 = "<link rel=\"stylesheet\" href=\""; //$NON-NLS-1$
	private static final String cssLink2 = "\" type=\"text/css\"></link>\n"; //$NON-NLS-1$

	private static String replaceParameters(String input) {
		final String OS = "${os}"; //$NON-NLS-1$
		int index = input.indexOf(OS);
		if (index < 0) {
			return input;
		}
		String result = input.substring(0, index) + Platform.getOS() + input.substring(index + OS.length());
		return replaceParameters(result);
	}

	/**
	 * @param filenames
	 * @return
	 */
	public static String[] getCssFilenames(String filenames ) {
		if (filenames  == null) {
			return new String[0];
		}
		StringTokenizer tok = new StringTokenizer(filenames , ","); //$NON-NLS-1$
		String[] result = new String[tok.countTokens()];
		for (int i = 0; tok.hasMoreTokens(); i++) {
			result[i] = replaceParameters(tok.nextToken().trim());
		}
		return result;
	}

	public static void addCssFiles(final String preference, List<String> list) {
		String topicCssPath = Platform.getPreferencesService().getString(HelpBasePlugin.PLUGIN_ID, preference, "", null);  //$NON-NLS-1$
		String[] cssFiles = CssUtil.getCssFilenames(topicCssPath);
		Collections.addAll(list, cssFiles);
	}

	public static String createCssIncludes(List<String> cssFiles, String backPath) {
		StringBuilder script = new StringBuilder();
		for (String cssPath : cssFiles) {
			script.append(cssLink1);
			script.append(fixCssPath(cssPath, backPath));
			script.append(cssLink2);
		}
		return script.toString();
	}

	/*
	 * Substitute for PLUGINS_ROOT and PRODUCT_PLUGIN
	 */
	private static String fixCssPath(String path, String prefix) {
		String newPath = ProductPreferences.resolveSpecialIdentifiers(path);
		return prefix + "content" + newPath; //$NON-NLS-1$
	}

}
