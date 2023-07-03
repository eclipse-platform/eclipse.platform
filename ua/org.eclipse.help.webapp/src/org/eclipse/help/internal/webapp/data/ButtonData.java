/*******************************************************************************
 * Copyright (c) 2009, 2020 IBM Corporation and others.
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

package org.eclipse.help.internal.webapp.data;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.webapp.AbstractButton;

public class ButtonData extends RequestData {

	private static final String BUTTON_EXTENSION_POINT = "org.eclipse.help.webapp.toolbarButton"; //$NON-NLS-1$
	private List<AbstractButton> allButtons;

	public ButtonData(ServletContext context, HttpServletRequest request,
			HttpServletResponse response) {
		super(context, request, response);
	}

	public AbstractButton[] getButtons() {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] elements = registry
				.getConfigurationElementsFor(BUTTON_EXTENSION_POINT);
		if (allButtons == null) {
			allButtons = new ArrayList<>();
			for (IConfigurationElement element : elements) {
				Object obj = null;
				try {
					obj = element.createExecutableExtension("class"); //$NON-NLS-1$
				} catch (CoreException e) {
					ILog.of(getClass()).error("Create extension failed:[" //$NON-NLS-1$
							+ BUTTON_EXTENSION_POINT + "].", e); //$NON-NLS-1$
				}
				if (obj instanceof AbstractButton button) {
					allButtons.add(button);
				}
			}
			allButtons.sort(null);
		}

		List<AbstractButton> buttonList = new ArrayList<>();
		buttonList.addAll(allButtons);
		AbstractButton[] buttons = buttonList.toArray(new AbstractButton[buttonList.size()]);
		return buttons;
	}

	public String getImageUrl(AbstractButton button) {
		return request.getContextPath() + button.getImageURL();
	}

}
