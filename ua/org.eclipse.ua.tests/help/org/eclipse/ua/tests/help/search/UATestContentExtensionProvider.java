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

package org.eclipse.ua.tests.help.search;

import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.help.AbstractContentExtensionProvider;
import org.eclipse.help.IContentExtension;
import org.eclipse.help.IUAElement;

public class UATestContentExtensionProvider extends
		AbstractContentExtensionProvider {

	private IContentExtension extension;

	public UATestContentExtensionProvider() {
		// TODO Auto-generated constructor stub
		extension = new IContentExtension() {

			@Override
			public boolean isEnabled(IEvaluationContext context) {
				// TODO Auto-generated method stub
				return true;
			}

			@Override
			public IUAElement[] getChildren() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public int getType() {
				return REPLACEMENT;
			}

			@Override
			public String getPath() {
				return "org.eclipse.ua.tests/data/help/search/test6.xhtml#test6_paragraph_to_replace2";
			}

			@Override
			public String getContent() {
				return "/org.eclipse.ua.tests/data/help/search/testProvider.xhtml";
			}
		};
	}

	@Override
	public IContentExtension[] getContentExtensions(String locale) {
		return new IContentExtension[] { extension };
	}

}
