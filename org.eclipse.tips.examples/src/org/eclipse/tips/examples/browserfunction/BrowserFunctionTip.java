/*******************************************************************************
 * Copyright (c) 2018, 2023 vogella GmbH
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     simon.scholz@vogella.com - initial API and implementation
 *******************************************************************************/
package org.eclipse.tips.examples.browserfunction;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.e4.core.commands.ECommandService;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tips.core.IHtmlTip;
import org.eclipse.tips.core.Tip;
import org.eclipse.tips.core.TipImage;
import org.eclipse.tips.examples.DateUtil;
import org.eclipse.tips.ui.IBrowserFunctionProvider;
import org.eclipse.ui.PlatformUI;

public class BrowserFunctionTip extends Tip implements IHtmlTip, IBrowserFunctionProvider {

	public BrowserFunctionTip(String providerId) {
		super(providerId);
	}

	@Override
	public Map<String, Function<Object[], Object>> getBrowserFunctions() {
		return Collections.singletonMap("openPreferencesBrowserFunction", this::openPreferences);
	}

	@Override
	public String getHTML() {
		return """
				<html><head><title>IHtmlTip with IBrowserFunctionProvider</title></head>
				<body>
					<p>This tip shows HTML and provides a BrowserFunction, which can be invoked by using JavaScript.</p>
					<p><button onclick="openPreferencesBrowserFunction()">I gonna open the preferences from the Browser</button></p>
					<p><a href="#" onclick="openPreferencesBrowserFunction()">I do the same but with a link</a></p>
				</body>
				</html>""";
	}

	@Override
	public TipImage getImage() {
		return null;
	}

	@Override
	public Date getCreationDate() {
		return DateUtil.getDateFromYYMMDD("25/06/2018");
	}

	@Override
	public String getSubject() {
		return "This is an IHtmlTip, which also implements IBrowserFunctionProvider";
	}

	private Object openPreferences(Object[] args) {
		ECommandService commandService = PlatformUI.getWorkbench().getService(ECommandService.class);
		EHandlerService handlerService = PlatformUI.getWorkbench().getService(EHandlerService.class);

		ParameterizedCommand command = commandService.createCommand("org.eclipse.ui.window.preferences",
				Collections.singletonMap("preferencePageId", "org.eclipse.ui.preferencePages.Keys"));
		Display.getDefault().asyncExec(() -> handlerService.executeHandler(command));
		return null;
	}
}