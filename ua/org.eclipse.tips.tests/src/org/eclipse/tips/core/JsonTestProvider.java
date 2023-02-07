/*******************************************************************************
 * Copyright (c) 2018 Remain Software
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     wim.jongman@remainsoftware.com - initial API and implementation
 *******************************************************************************/
package org.eclipse.tips.core;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.tips.json.JsonTipProvider;

public class JsonTestProvider extends JsonTipProvider {

	public JsonTestProvider() throws MalformedURLException {
		URL resource = getClass().getResource("twittertips.json"); // $NON-NLS-1$
		if (resource != null) {
			setJsonUrl(resource.toString());
		}
	}

	@Override
	public String getID() {
		return getClass().getName();
	}
}
