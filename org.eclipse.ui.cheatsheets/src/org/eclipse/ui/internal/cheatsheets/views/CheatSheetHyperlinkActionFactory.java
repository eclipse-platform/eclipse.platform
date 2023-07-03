/*******************************************************************************
 * Copyright (c) 2020 1C-Soft LLC and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     George Suaridze (1C-Soft LLC) - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal.cheatsheets.views;

import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Properties;

import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.CommandException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.internal.cheatsheets.CheatSheetPlugin;
import org.eclipse.ui.internal.cheatsheets.Messages;

/**
 * A factory that knows how to create cheatsheet URL actions.
 * <p>
 * A cheatsheet URL is a valid http url, with org.eclipse.ui.cheatsheet as a
 * host.
 * </p>
 * <p>
 * A cheatsheet url instance is created by parsing the url and retrieving the
 * embedded "command" and parameters. For example, the following urls are valid
 * cheatsheet urls:
 * <p>
 *
 * <pre>
 *  http://org.eclipse.ui.cheatsheet/showView?id=org.eclipse.pde.runtime.LogView
 *  http://org.eclipse.ui.cheatsheet/execute?command=org.eclipse.ui.newWizard%28newWizardId%3Dorg.eclipse.ui.wizards.new.project%29"
 * </pre>
 * <p>
 * When parsed, the first url has "showView" as a command, and "id" parameter.
 * While the second "execute" as a command and "command" as parameter with
 * "newWizardId" as command parameter.
 * </p>
 * <p>
 * For now it supports two commands:
 * <li>showView - to activate given view by its id</li>
 * <li>execute - to execute eclipse command</li>
 */
public class CheatSheetHyperlinkActionFactory {

	private static final String CHEAT_SHEET_PROTOCOL = "http"; //$NON-NLS-1$
	private static final String CHEAT_SHEET_HOST_ID = "org.eclipse.ui.cheatsheet"; //$NON-NLS-1$

	private static final String EXECUTE = "execute"; //$NON-NLS-1$
	private static final String SHOW_VIEW = "showView"; //$NON-NLS-1$

	private static final String KEY_ID = "id"; //$NON-NLS-1$
	private static final String KEY_COMAND = "command"; //$NON-NLS-1$
	private static final String KEY_DECODE = "decode"; //$NON-NLS-1$

	private static final String VALUE_TRUE = "true"; //$NON-NLS-1$

	/**
	 * Creates {@link CheatSheetHyperlinkAction} for given url string
	 *
	 * @param urlString
	 *            - url string representation, cannot be {@code null}
	 * @return appropriate cheatsheet action {@link CheatSheetHyperlinkAction}
	 */
	public CheatSheetHyperlinkAction create(String urlString) {
		if (urlString == null) {
			return new FallbackAction(urlString);
		}
		try {
			URL url = new URL(urlString);
			if (isCheatSheetHyperlink(url)) {
				String action = getPathAsAction(url);
				Properties parameters = getQueryParameters(url);
				switch (action) {
				case EXECUTE:
					return new CommandAction(getParameter(parameters, KEY_COMAND));
				case SHOW_VIEW:
					return new ShowViewAction(getParameter(parameters, KEY_ID));
				default:
					CheatSheetPlugin.getPlugin().getLog().error("Unsupported action: " + action, null); //$NON-NLS-1$
				}
				return new FallbackAction(urlString);
			} else if (url.getProtocol() != null) {
				return new OpenInBrowserAction(url);
			} else {
				return new FallbackAction(urlString);
			}
		} catch (MalformedURLException e) {
			CheatSheetPlugin.getPlugin().getLog().error("Malformed URL: " + urlString, e); //$NON-NLS-1$
			return new FallbackAction(urlString);
		}
	}

	private boolean isCheatSheetHyperlink(URL url) {
		if (!url.getProtocol().equalsIgnoreCase(CHEAT_SHEET_PROTOCOL)) {
			return false;
		}
		if (url.getHost().equalsIgnoreCase(CHEAT_SHEET_HOST_ID)) {
			return true;
		}
		return false;
	}

	private Properties getQueryParameters(URL url) {
		Properties properties = new Properties();
		String query = url.getQuery();
		if (query == null) {
			return properties;
		}
		String[] params = query.split("&"); //$NON-NLS-1$
		for (int i = 0; i < params.length; i++) {
			String[] keyValuePair = params[i].split("="); //$NON-NLS-1$
			if (keyValuePair.length != 2) {
				CheatSheetPlugin.getPlugin().getLog()
						.warn(MessageFormat.format("Ignoring the following Cheatsheet URL parameter: {0}", params[i])); //$NON-NLS-1$
				continue;
			}

			String key = urlDecode(keyValuePair[0]);
			if (key == null) {
				CheatSheetPlugin.getPlugin().getLog()
						.warn(MessageFormat.format("Failed to URL decode key: {0}", keyValuePair[0])); //$NON-NLS-1$
				continue;
			}

			String value = urlDecode(keyValuePair[1]);
			if (value == null) {
				CheatSheetPlugin.getPlugin().getLog()
						.warn(MessageFormat.format("Failed to URL decode value: {0}", keyValuePair[1])); //$NON-NLS-1$
				continue;
			}

			properties.setProperty(key, value);
		}
		return properties;
	}

	private String urlDecode(String encodedURL) {
		int len = encodedURL.length();
		ByteArrayOutputStream os = new ByteArrayOutputStream(len);

		for (int i = 0; i < len;) {
			switch (encodedURL.charAt(i)) {
			case '%':
				if (len >= i + 3) {
					os.write(Integer.parseInt(encodedURL.substring(i + 1, i + 3), 16));
				}
				i += 3;
				break;
			case '+': // exception from standard
				os.write(' ');
				i++;
				break;
			default:
				os.write(encodedURL.charAt(i++));
				break;
			}
		}
		return new String(os.toByteArray(), StandardCharsets.UTF_8);
	}

	private String getPathAsAction(URL url) {
		String action = url.getPath();
		if (action != null) {
			action = action.substring(1);
		}
		return action;
	}

	private String getParameter(Properties parameters, String parameterId) {
		String value = parameters.getProperty(parameterId);
		String decode = parameters.getProperty(KEY_DECODE);

		if (value != null) {
			try {
				if (decode != null && decode.equalsIgnoreCase(VALUE_TRUE)) {
					return URLDecoder.decode(value, StandardCharsets.UTF_8); // $NON-NLS-1$
				}
				return value;
			} catch (Exception e) {
				CheatSheetPlugin.getPlugin().getLog().error("Failed to decode URL: " + parameterId, e); //$NON-NLS-1$
			}
		}
		return value;
	}

	public static abstract class CheatSheetHyperlinkAction {

		/**
		 * Executes action.
		 */
		public final void execute() {
			Display display = Display.getDefault();
			BusyIndicator.showWhile(display, () -> {
				doExecute(display);
			});

		}

		protected abstract void doExecute(Display display);
	}

	private static class FallbackAction extends CheatSheetHyperlinkAction {

		private final String url;

		public FallbackAction(String url) {
			this.url = url;
		}

		@Override
		public void doExecute(Display display) {
			MessageDialog.openInformation(display.getActiveShell(),
					null,
					MessageFormat.format(Messages.CHEAT_SHEET_UNSUPPORTED_LINK_ACTIVATION_MESSAGE, url));
		}
	}

	private static class OpenInBrowserAction extends CheatSheetHyperlinkAction {

		private final URL url;

		public OpenInBrowserAction(URL url) {
			this.url = url;
		}

		@Override
		public void doExecute(Display display) {
			try {
				IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
				support.getExternalBrowser().openURL(url);
			} catch (PartInitException e) {
				CheatSheetPlugin.getPlugin().getLog().error("Cheatsheet failed to get Browser support.", e); //$NON-NLS-1$
			}
		}
	}

	private static class ShowViewAction extends CheatSheetHyperlinkAction {

		private final String viewId;

		public ShowViewAction(String viewId) {
			this.viewId = viewId;
		}

		@Override
		protected void doExecute(Display display) {
			IWorkbench workbench = PlatformUI.getWorkbench();
			IWorkbenchWindow activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();
			if (activeWorkbenchWindow != null) {
				try {
					activeWorkbenchWindow.getActivePage().showView(viewId, null, IWorkbenchPage.VIEW_ACTIVATE);
				} catch (PartInitException e) {
					CheatSheetPlugin.getPlugin().getLog().error("Error while activating view: " + viewId, e); //$NON-NLS-1$
				}
			}
		}
	}

	private static class CommandAction extends CheatSheetHyperlinkAction {

		private final String command;

		public CommandAction(String command) {
			this.command = command;
		}

		@Override
		protected void doExecute(Display display) {
			ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
			IHandlerService handlerService = PlatformUI.getWorkbench().getService(IHandlerService.class);
			if (commandService == null || handlerService == null) {
				CheatSheetPlugin.getPlugin().getLog().error(
						"Could not get ICommandService or IHandlerService while trying to execute: " + command, null); //$NON-NLS-1$
				return;
			}
			try {
				ParameterizedCommand pCommand = commandService.deserialize(command);
				handlerService.executeCommand(pCommand, null);
			} catch (CommandException e) {
				CheatSheetPlugin.getPlugin().getLog().error("Could not execute command: " + command, e); //$NON-NLS-1$
			}
		}
	}
}
