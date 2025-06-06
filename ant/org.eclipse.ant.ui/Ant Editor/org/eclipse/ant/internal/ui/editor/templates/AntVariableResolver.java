/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
package org.eclipse.ant.internal.ui.editor.templates;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateVariableResolver;

/**
 * Looks up existing ant variables and proposes them. The proposals are sorted by their prefix-likeness with the variable type.
 */
public class AntVariableResolver extends TemplateVariableResolver {

	Comparator<String> comparator = new Comparator<>() {

		@Override
		public int compare(String o1, String o2) {
			return getCommonPrefixLength(getType(), o2) - getCommonPrefixLength(getType(), o1);
		}

		private int getCommonPrefixLength(String type, String var) {
			int i = 0;
			CharSequence vSeq = var.subSequence(2, var.length() - 1); // strip away ${}
			while (i < type.length() && i < vSeq.length()) {
				if (Character.toLowerCase(type.charAt(i)) == Character.toLowerCase(vSeq.charAt(i))) {
					i++;
				} else {
					break;
				}
			}
			return i;
		}
	};

	@Override
	protected String[] resolveAll(TemplateContext context) {
		String[] proposals = new String[] { "${srcDir}", "${dstDir}" }; //$NON-NLS-1$ //$NON-NLS-2$
		Arrays.sort(proposals, comparator);
		return proposals;
	}
}
