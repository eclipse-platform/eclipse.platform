/*******************************************************************************
 * Copyright (c) 2018 vogella GmbH
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     simon.scholz@vogella.com - initial API and implementation
 *******************************************************************************/
package org.eclipse.tips.ui;

import java.util.Map;
import java.util.function.Function;

import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.tips.core.IHtmlTip;
import org.eclipse.tips.core.IUrlTip;

/**
 * This interface is intended to be implemented by {@link IHtmlTip} or
 * {@link IUrlTip} instances to provide Java functions that can be invoked from
 * within an SWT browser.
 *
 * @see IHtmlTip
 * @see IUrlTip
 * @see BrowserFunction
 */
public interface IBrowserFunctionProvider {

	/**
	 * Provides a map with functions which can be invoked by JavaScript code
	 * provided by an {@link IHtmlTip} or {@link IUrlTip}.
	 *
	 * @return a {@link Map} containing names for a JavaScript function as key and
	 *         {@link Function} objects, which can be invoked from the JavaScript in
	 *         a SWT Browser.
	 *
	 * @see BrowserFunction
	 */
	Map<String, Function<Object[], Object>> getBrowserFunctions();
}
