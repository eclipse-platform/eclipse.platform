/*******************************************************************************
 * Copyright (c) 2004, 2017 IBM Corporation and others.
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

package org.eclipse.ui.internal.intro.impl.model.url;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Properties;

import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.CommandException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.internal.intro.impl.IIntroConstants;
import org.eclipse.ui.internal.intro.impl.IntroPlugin;
import org.eclipse.ui.internal.intro.impl.Messages;
import org.eclipse.ui.internal.intro.impl.model.AbstractIntroElement;
import org.eclipse.ui.internal.intro.impl.model.AbstractIntroPage;
import org.eclipse.ui.internal.intro.impl.model.IntroLaunchBarElement;
import org.eclipse.ui.internal.intro.impl.model.IntroModelRoot;
import org.eclipse.ui.internal.intro.impl.model.IntroPartPresentation;
import org.eclipse.ui.internal.intro.impl.model.IntroURLAction;
import org.eclipse.ui.internal.intro.impl.model.loader.ExtensionPointManager;
import org.eclipse.ui.internal.intro.impl.model.loader.ModelLoaderUtil;
import org.eclipse.ui.internal.intro.impl.model.util.ModelUtil;
import org.eclipse.ui.internal.intro.impl.parts.StandbyPart;
import org.eclipse.ui.internal.intro.impl.presentations.BrowserIntroPartImplementation;
import org.eclipse.ui.internal.intro.impl.presentations.IntroLaunchBar;
import org.eclipse.ui.internal.intro.impl.util.DialogUtil;
import org.eclipse.ui.internal.intro.impl.util.Log;
import org.eclipse.ui.internal.intro.impl.util.StringUtil;
import org.eclipse.ui.internal.intro.impl.util.Util;
import org.eclipse.ui.intro.IIntroPart;
import org.eclipse.ui.intro.IIntroSite;
import org.eclipse.ui.intro.config.CustomizableIntroPart;
import org.eclipse.ui.intro.config.IIntroAction;
import org.eclipse.ui.intro.config.IIntroURL;
import org.eclipse.ui.intro.config.IntroURLFactory;

/**
 * An intro url. An intro URL is a valid http url, with org.eclipse.ui.intro as
 * a host. This class holds all logic to execute Intro URL commands, ie: an
 * Intro URL knows how to execute itself.
 */
public class IntroURL implements IIntroURL {


	/**
	 * Intro URL constants.
	 */
	public static final String INTRO_PROTOCOL = "http"; //$NON-NLS-1$
	public static final String INTRO_HOST_ID = "org.eclipse.ui.intro"; //$NON-NLS-1$

	/**
	 * Constants that represent Intro URL actions.
	 */
	public static final String SET_STANDBY_MODE = "setStandbyMode"; //$NON-NLS-1$
	public static final String SHOW_STANDBY = "showStandby"; //$NON-NLS-1$
	public static final String CLOSE = "close"; //$NON-NLS-1$
	public static final String SHOW_HELP_TOPIC = "showHelpTopic"; //$NON-NLS-1$
	public static final String SHOW_HELP = "showHelp"; //$NON-NLS-1$
	public static final String OPEN_BROWSER = "openBrowser"; //$NON-NLS-1$
	public static final String OPEN_URL = "openURL"; //$NON-NLS-1$
	public static final String RUN_ACTION = "runAction"; //$NON-NLS-1$
	public static final String SHOW_PAGE = "showPage"; //$NON-NLS-1$
	public static final String SHOW_MESSAGE = "showMessage"; //$NON-NLS-1$
	public static final String NAVIGATE = "navigate"; //$NON-NLS-1$
	public static final String SWITCH_TO_LAUNCH_BAR = "switchToLaunchBar"; //$NON-NLS-1$
	public static final String EXECUTE = "execute"; //$NON-NLS-1$

	/**
	 * Constants that represent valid action keys.
	 */
	public static final String KEY_ID = "id"; //$NON-NLS-1$
	public static final String KEY_PLUGIN_ID = "pluginId"; //$NON-NLS-1$
	public static final String KEY_CLASS = "class"; //$NON-NLS-1$
	public static final String KEY_STANDBY = "standby"; //$NON-NLS-1$
	public static final String KEY_PART_ID = "partId"; //$NON-NLS-1$
	public static final String KEY_INPUT = "input"; //$NON-NLS-1$
	public static final String KEY_MESSAGE = "message"; //$NON-NLS-1$
	public static final String KEY_URL = "url"; //$NON-NLS-1$
	public static final String KEY_DIRECTION = "direction"; //$NON-NLS-1$
	public static final String KEY_EMBED = "embed"; //$NON-NLS-1$
	public static final String KEY_EMBED_TARGET = "embedTarget"; //$NON-NLS-1$
	public static final String KEY_DECODE = "decode"; //$NON-NLS-1$
	public static final String KEY_COMAND = "command"; //$NON-NLS-1$


	public static final String VALUE_BACKWARD = "backward"; //$NON-NLS-1$
	public static final String VALUE_FORWARD = "forward"; //$NON-NLS-1$
	public static final String VALUE_HOME = "home"; //$NON-NLS-1$
	public static final String VALUE_TRUE = "true"; //$NON-NLS-1$
	public static final String VALUE_FALSE = "false"; //$NON-NLS-1$
	public static final String VALUE_CLOSE = "close"; //$NON-NLS-1$
	public static final String VALUE_STANDBY = "standby"; //$NON-NLS-1$
	public static final String VALUE_LAUNCHBAR = "launchbar"; //$NON-NLS-1$



	private String action = null;
	private Properties parameters = null;

	/**
	 * Prevent creation. Must be created through an IntroURLParser. This
	 * constructor assumed we have a valid intro url.
	 *
	 * @param url
	 */
	IntroURL(String action, Properties parameters) {
		this.action = action;
		this.parameters = parameters;
	}

	/**
	 * Executes whatever valid Intro action is embedded in this Intro URL.
	 *
	 */
	@Override
	public boolean execute() {
		final boolean[] result = new boolean[1];
		Display display = Display.getCurrent();

		BusyIndicator.showWhile(display, () -> result[0] = doExecute());
		return result[0];
	}

	protected boolean doExecute() {
		if (Log.logInfo) {
			String msg = StringUtil.concat("Running Intro URL action ", action, //$NON-NLS-1$
				" with parameters: ", parameters.toString()); //$NON-NLS-1$
			Log.info(msg);
		}

		// check for all supported Intro actions first.
		switch (action) {
		case CLOSE:
			return closeIntro();
		case SET_STANDBY_MODE:
			// Sets the state of the intro part. Does not care about passing
			// input to the part.
			return setStandbyState(getParameter(KEY_STANDBY));
		case SHOW_STANDBY:
			return handleStandbyState(getParameter(KEY_PART_ID),
					getParameter(KEY_INPUT));
		case SHOW_HELP:
			// display the full Help System.
			return showHelp();
		case SHOW_HELP_TOPIC:
			// display a Help System Topic. It can be displayed in the Help
			// system window, or embedded as an intro page.
			// return showHelpTopic(getParameter(KEY_ID));
			return showHelpTopic(getParameter(KEY_ID), getParameter(KEY_EMBED),
					getParameter(KEY_EMBED_TARGET));
		case OPEN_BROWSER:
			// display url in external browser
			return openBrowser(getParameter(KEY_URL),
					getParameter(KEY_PLUGIN_ID));
		default:
			break;
		}
		switch (action) {
		case OPEN_URL:
			// display url embedded in intro browser.
			return openURL(getParameter(KEY_URL), getParameter(KEY_PLUGIN_ID));
		case RUN_ACTION:
			// run an Intro action. Get the pluginId and the class keys. Pass
			// the parameters and the standby state.
			return runAction(getParameter(KEY_PLUGIN_ID),
					getParameter(KEY_CLASS), parameters, getParameter(KEY_STANDBY));
		case EXECUTE:
			// execute a serialized command
			return executeCommand(getParameter(KEY_COMAND), getParameter(KEY_STANDBY));
		case SHOW_PAGE:
			// display an Intro Page.
			return showPage(getParameter(KEY_ID), getParameter(KEY_STANDBY));
		case SHOW_MESSAGE:
			return showMessage(getParameter(KEY_MESSAGE));
		case NAVIGATE:
			return navigate(getParameter(KEY_DIRECTION));
		case SWITCH_TO_LAUNCH_BAR:
			return switchToLaunchBar();
		default:
			return handleCustomAction();
		}
	}


	private boolean closeIntro() {
		// Relies on Workbench.
		return IntroPlugin.closeIntro();
	}

	/**
	 * Sets the into part to standby, and shows the passed standby part, with
	 * the given input. Forces the Intro view to open, if not yet created.
	 *
	 * @param partId
	 * @param input
	 */
	private boolean handleStandbyState(String partId, String input) {
		// set intro to standby mode. we know we have a customizable part.
		CustomizableIntroPart introPart = (CustomizableIntroPart) IntroPlugin
			.getIntro();
		if (introPart == null)
			introPart = (CustomizableIntroPart) IntroPlugin.showIntro(true);
		// store the flag to indicate that standbypart is needed.
		introPart.getControl().setData(IIntroConstants.SHOW_STANDBY_PART,
			VALUE_TRUE);
		IntroPlugin.setIntroStandby(true);
		StandbyPart standbyPart = introPart
			.getAdapter(StandbyPart.class);

		// We may not have a valid partId or we might fail to instantiate part or
		// create the part content. An empty part will be shown. Signal failure.
		return standbyPart != null && standbyPart.showContentPart(partId, input);
	}

	/**
	 * Set the Workbench Intro Part state. Forces the Intro view to open, if not yet created.
	 *
	 * Historically this value was "true" (show standby) or "false" (show normal). In Neon we add
	 * "close", "standby" and "launchbar".
	 *
	 * @param state
	 * @return true if the intro was shown, or false if the intro could not be shown
	 */
	private boolean setStandbyState(String state) {
		if (state == null)
			return false;
		if (state.equals(VALUE_CLOSE)) {
			return IntroPlugin.closeIntro();
		} else if (state.equals(VALUE_LAUNCHBAR)) {
			return switchToLaunchBar();
		}
		boolean standby = state.equals(VALUE_TRUE) || state.equals(VALUE_STANDBY);

		IIntroPart introPart = IntroPlugin.showIntro(standby);
		if (introPart == null)
			return false;
		return true;
	}


	/**
	 * Run an action
	 */
	private boolean runAction(String pluginId, String className,
			Properties parameters, String standbyState) {

		Object actionObject = ModelLoaderUtil.createClassInstance(pluginId,
			className);
		try {
			if (actionObject instanceof IIntroAction) {
				IIntroAction introAction = (IIntroAction) actionObject;
				IIntroSite site = IntroPlugin.getDefault().getIntroModelRoot()
					.getPresentation().getIntroPart().getIntroSite();
				introAction.run(site, parameters);
			} else if (actionObject instanceof IAction) {
				IAction action = (IAction) actionObject;
				action.run();

			} else if (actionObject instanceof IActionDelegate) {
				final IActionDelegate delegate = (IActionDelegate) actionObject;
				if (delegate instanceof IWorkbenchWindowActionDelegate)
					((IWorkbenchWindowActionDelegate) delegate).init(PlatformUI
						.getWorkbench().getActiveWorkbenchWindow());
				Action proxy = new Action(this.action) {

					@Override
					public void run() {
						delegate.run(this);
					}
				};
				proxy.run();
			} else
				// we could not create the class.
				return false;
			// ran action successfully. Now set intro intro standby if needed.
			if (standbyState == null)
				return true;
			return setStandbyState(standbyState);
		} catch (Exception e) {
			Log.error("Could not run action: " + className, e); //$NON-NLS-1$
			return false;
		}
	}

	/**
	 * Executes a serialized <code>ParameterizedCommand</code>. Uses
	 * {@link ICommandService#deserialize(String)} to convert the <code>command</code> argument
	 * into the parameterized command.
	 */
	private boolean executeCommand(String command, String standbyState) {
		ICommandService commandService = getCommandService();
		IHandlerService handlerService = getHandlerService();
		if (commandService == null || handlerService == null) {
			Log.error("Could not get ICommandService or IHandlerService while trying to execute: " + command, null); //$NON-NLS-1$
			return false;
		}

		try {
			ParameterizedCommand pCommand = commandService.deserialize(command);
			handlerService.executeCommand(pCommand, null);

			// Executed command successfully. Now set intro standby if needed.
			if (standbyState == null)
				return true;
			return setStandbyState(standbyState);
		} catch (CommandException ex) {
			Log.error("Could not execute command: " + command, ex); //$NON-NLS-1$
			return false;
		}
	}

	private ICommandService getCommandService() {
		IWorkbench wb =	PlatformUI.getWorkbench();
		if (wb != null) {
			Object serviceObject = wb.getAdapter(ICommandService.class);
			if (serviceObject != null) {
				ICommandService service = (ICommandService)serviceObject;
				return service;
			}
		}
		return null;
	}

	private IHandlerService getHandlerService() {
		IWorkbench wb =	PlatformUI.getWorkbench();
		if (wb != null) {
			Object serviceObject = wb.getAdapter(IHandlerService.class);
			if (serviceObject != null) {
				IHandlerService service = (IHandlerService)serviceObject;
				return service;
			}
		}
		return null;
	}

	/**
	 * Open a help topic. If embed="true", open the help href as an intro page.
	 * If false, open the href in the Help system window. If embedTarget is set,
	 * then the Help System topic is embedded instead of the content of the
	 * specified div.<br>
	 * In the case of SWT presentation, embedd flag is ignored and the topic is
	 * opened in the Help system window.
	 */
	private boolean showHelpTopic(String href, String embed, String embedTarget) {
		return new ShowHelpURLHandler(this).showHelpTopic(href, embed,
			embedTarget);
	}



	/**
	 * Open the help system.
	 */
	private boolean showHelp() {
		PlatformUI.getWorkbench().getHelpSystem().displayHelp();
		return true;
	}

	/**
	 * Launch external browser.
	 */
	private boolean openBrowser(String url, String pluginId) {
		// no need to decode url because we will create another url from this
		// url anyway. Resolve the url just in case we are trying to load a
		// plugin relative file.
		url = ModelUtil.resolveURL(url, pluginId);
		return Util.openBrowser(url);
	}


	/**
	 * Show a URL in an intro page. This is the embedded version of the intro
	 * action openBrowser(). It is useful when trying to show an html file
	 * relative to another plugin. When the presentation is UI forms
	 * presentation, this call behaves exactly as the openBrowser intro action.
	 */

	private boolean openURL(String url, String pluginId) {
		IntroModelRoot model = IntroPlugin.getDefault().getIntroModelRoot();
		String presentationStyle = model.getPresentation()
			.getImplementationKind();

		if (presentationStyle.equals(IntroPartPresentation.BROWSER_IMPL_KIND)) {
			// HTML presentation
			url = ModelUtil.resolveURL(url, pluginId);
			BrowserIntroPartImplementation impl = (BrowserIntroPartImplementation) IntroPlugin
				.getDefault().getIntroModelRoot().getPresentation()
				.getIntroPartImplementation();
			Browser browser = impl.getBrowser();
			return browser.setUrl(url);
		}
		// SWT presentation.
		return openBrowser(url, pluginId);
	}


	private boolean showMessage(String message) {
		if (message == null)
			return false;
		DialogUtil.displayInfoMessage(null, message);
		return true;
	}

	/**
	 * Display an Intro Page.
	 * <p>
	 * INTRO: revisit picking first page.
	 */
	boolean showPage(String pageId, String standbyState) {
		// set the current page id in the model. This will trigger appropriate
		// listener event to the UI. If setting the page in the model fails (ie:
		// the page was not found in the current model, look for it in loaded
		// models. return false if failed.
		// avoid flicker.
		CustomizableIntroPart currentIntroPart = (CustomizableIntroPart) IntroPlugin
			.getIntro();
		currentIntroPart.getControl().setRedraw(false);

		IntroModelRoot modelRoot = IntroPlugin.getDefault().getIntroModelRoot();
		boolean success = modelRoot.setCurrentPageId(pageId);
		if (!success)
			success = includePageToShow(modelRoot, pageId);

		// we turned drawing off. Turn it on again.
		currentIntroPart.getControl().setRedraw(true);

		if (success) {
			// found page. Set the history
			modelRoot.getPresentation().updateHistory(
				modelRoot.getCurrentPage());
			// ran action successfully. Now set intro intro standby if needed.
			if (standbyState == null)
				return true;
			return setStandbyState(standbyState);
		}
		// could not find referenced page.
		return false;
	}

	/**
	 * Finds the target page and includes it in passed model.
	 *
	 * @param pageId
	 * @return
	 */
	private boolean includePageToShow(IntroModelRoot model, String pageId) {
		AbstractIntroPage page = findPageToShow(pageId);
		if (page == null) {
			Log.warning("Could not find an Intro page with id=" + pageId); //$NON-NLS-1$
			return false;
		}
		// now clone the target page because original model should be kept
		// intact. Resolve target page first to resolve its includes
		// properly. Insert presentation shared style at the top of the shared
		// styles list because once reparented, the shared style is lost.
		// Finally, add clone page to current model.
		page.getChildren();
		// current kind.
		String currentPresentationKind = model.getPresentation()
			.getImplementationKind();
		// load shared style corresponding to same presentation kind from target
		// model.
		IntroPartPresentation targetPresentation = ((IntroModelRoot) page
			.getParent()).getPresentation();
		String targetSharedStyle = targetPresentation
			.getSharedStyle(currentPresentationKind);
		// clone.
		AbstractIntroPage clonedPage = null;
		try {
			clonedPage = (AbstractIntroPage) page.clone();
		} catch (CloneNotSupportedException ex) {
			// should never be here.
			Log.error("Failed to clone Intro model node.", ex); //$NON-NLS-1$
			return false;
		}
		// reparent cloned target to current model.
		clonedPage.setParent(model);
		// REVISIT: SWT presentation does not support multiple shared
		// styles.
		if (targetSharedStyle != null)
			// add target model shared style.
			clonedPage.insertStyle(targetSharedStyle, 0);
		model.addChild(clonedPage);
		return model.setCurrentPageId(clonedPage.getId());
	}


	/**
	 * Searches all loaded models for the first page with the given id.
	 *
	 * @param pageId
	 * @return
	 */
	private AbstractIntroPage findPageToShow(String pageId) {
		// check all cached models.
		for (IntroModelRoot model : ExtensionPointManager.getInst().getIntroModels().values()) {
			AbstractIntroPage page = (AbstractIntroPage) model.findChild(
				pageId, AbstractIntroElement.ABSTRACT_PAGE);
			if (page != null)
				return page;
		}
		// could not find page in any model.
		return null;
	}

	/**
	 * Navigate foward in the presentation, whichever one it is.
	 *
	 * @return
	 */
	private boolean navigate(String direction) {
		// set intro to standby mode. we know we have a customizable part.
		CustomizableIntroPart introPart = (CustomizableIntroPart) IntroPlugin
			.getIntro();
		if (introPart == null)
			// intro is closed. Do nothing.
			return false;

		IntroPartPresentation presentation = introPart
			.getAdapter(IntroPartPresentation.class);

		if (direction.equalsIgnoreCase(VALUE_BACKWARD))
			return presentation.navigateBackward();
		else if (direction.equalsIgnoreCase(VALUE_FORWARD))
			return presentation.navigateForward();
		else if (direction.equalsIgnoreCase(VALUE_HOME))
			return presentation.navigateHome();
		return false;
	}


	/**
	 * @return Returns the action imbedded in this URL.
	 */
	@Override
	public String getAction() {
		return action;
	}

	/**
	 * Return a parameter defined in the Intro URL. Returns null if the
	 * parameter is not defined. If this intro url has a decode=true parameter,
	 * then all parameters are returned decoded using UTF-8.
	 *
	 * @param parameterId
	 * @return
	 */
	@Override
	public String getParameter(String parameterId) {
		// make sure to decode only on return, since we may need to recreate the
		// url when handling custom urls.
		String value = parameters.getProperty(parameterId);
		String decode = parameters.getProperty(KEY_DECODE);

		if (value != null)
			try {
				if (decode!=null && decode.equalsIgnoreCase(VALUE_TRUE))
					// we are told to decode the parameters of the url through
					// the decode parameter. Assume that parameters are
					// UTF-8 encoded.
					return URLDecoder.decode(value, StandardCharsets.UTF_8);
				return value;
			} catch (Exception e) {
				// should never be here.
				Log.error("Failed to decode URL: " + parameterId, e); //$NON-NLS-1$
			}
		return value;
	}

	private boolean handleCustomAction() {
		IntroURLAction command = ExtensionPointManager.getInst()
			.getSharedConfigExtensionsManager().getCommand(action);
		if (command == null) {
			String message = Messages.IntroURL_badCommand;
			DialogUtil.displayInfoMessage(null, message,
				new Object[] { action });
			return false;
		}

		// custom command. execute it.
		StringBuilder url = new StringBuilder();
		url.append("http://org.eclipse.ui.intro/"); //$NON-NLS-1$
		url.append(command.getReplaceValue().trim());
		if (!command.getReplaceValue().contains("?")) //$NON-NLS-1$
			// command does not have parameters.
			url.append("?"); //$NON-NLS-1$
		else
			// command already has parameters.
			url.append("&"); //$NON-NLS-1$
		url.append(retrieveInitialQuery());
		IIntroURL introURL = IntroURLFactory.createIntroURL(url.toString());
		if (introURL != null)
			return introURL.execute();
		return false;
	}


	/**
	 * Recreate the initial query passed to this URL.
	 *
	 * @return
	 */
	private String retrieveInitialQuery() {
		StringBuilder query = new StringBuilder();
		Enumeration keys = parameters.keys();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			query.append(key);
			query.append("="); //$NON-NLS-1$
			query.append(parameters.get(key));
			if (keys.hasMoreElements())
				query.append("&"); //$NON-NLS-1$
		}
		return query.toString();
	}


	private boolean switchToLaunchBar() {
		IIntroPart intro = PlatformUI.getWorkbench().getIntroManager().getIntro();
		if (intro == null)
			return false;

		IntroModelRoot modelRoot = IntroPlugin.getDefault().getIntroModelRoot();

		IntroLaunchBarElement launchBarElement = modelRoot.getPresentation().getLaunchBarElement();
		if (launchBarElement == null)
			return true;
		IWorkbenchWindow window = intro.getIntroSite().getWorkbenchWindow();
		IntroLaunchBar.create(window, modelRoot, launchBarElement);

		PlatformUI.getWorkbench().getIntroManager().setIntroStandby(intro, true);

		closeIntro();

		return true;
	}
}
