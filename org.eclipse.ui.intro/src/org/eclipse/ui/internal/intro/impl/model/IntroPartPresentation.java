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

package org.eclipse.ui.internal.intro.impl.model;

import java.util.ArrayList;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IRegistryChangeEvent;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.internal.intro.impl.IntroPlugin;
import org.eclipse.ui.internal.intro.impl.model.loader.ModelLoaderUtil;
import org.eclipse.ui.internal.intro.impl.model.util.ModelUtil;
import org.eclipse.ui.internal.intro.impl.presentations.BrowserIntroPartImplementation;
import org.eclipse.ui.internal.intro.impl.presentations.FormIntroPartImplementation;
import org.eclipse.ui.internal.intro.impl.presentations.TextIntroPartImplementation;
import org.eclipse.ui.internal.intro.impl.util.Log;
import org.eclipse.ui.intro.IIntroPart;

/**
 * This class models the presentation element contributed to a config extension point. The
 * Presentation class delegates UI creation to the actual Implementation class, and passes the
 * IntroPart along to this implementation class. Also, dynamic awarness is honored here.
 * <p>
 * Rules:
 * <ul>
 * <li>There is no model class for the "implementation" markup element. This presentation class
 * inherits information from the implementation class that is picked (based on OS, ...).</li>
 * <li>ID attribute of this model class is the id of the picked implementation element.</li>
 * <li>Style attribute in this model class is the style of the picked implementation element.</li>
 * <li>HTMLHeadContent in this model class is the HEAD element under the picked implementation
 * element, only if the implementation element is a Browser implementation.</li>
 * <li>The UI model class, AbstractIntroPartImplementation, that represents the IntroPart
 * implementation is cached here for quick access. It is used by intro url actions for manipulation
 * of UI.<br>
 * INTRO:This really should be in a UI model class.
 * </ul>
 */
public class IntroPartPresentation extends AbstractIntroElement {

	protected static final String TAG_PRESENTATION = "presentation"; //$NON-NLS-1$
	private static final String TAG_IMPLEMENTATION = "implementation"; //$NON-NLS-1$

	private static final String ATT_KIND = "kind"; //$NON-NLS-1$
	private static final String ATT_STYLE = "style"; //$NON-NLS-1$
	private static final String ATT_OS = "os"; //$NON-NLS-1$
	private static final String ATT_WS = "ws"; //$NON-NLS-1$
	protected static final String ATT_HOME_PAGE_ID = "home-page-id"; //$NON-NLS-1$
	protected static final String ATT_STANDBY_PAGE_ID = "standby-page-id"; //$NON-NLS-1$

	public static final String BROWSER_IMPL_KIND = "html"; //$NON-NLS-1$
	public static final String FORMS_IMPL_KIND = "swt"; //$NON-NLS-1$
	// this implementation kind if not public api. Only used internally for
	// debugging.
	private static final String TEXT_IMPL_KIND = "text"; //$NON-NLS-1$


	// private String title;
	private String [] implementationStyles;
	private String implementationKind;
	private String homePageId;
	private String standbyPageId;

	// The Head contributions to this preentation (inherited from child
	// implementation).
	private IntroHead head;

	private AbstractIntroPartImplementation implementation;

	private IntroLaunchBarElement launchBar;

	// CustomizableIntroPart and memento instances. Passed to the Implementation
	// classes.
	private IIntroPart introPart;
	private IMemento memento;

	/**
	 *
	 */
	IntroPartPresentation(IConfigurationElement element) {
		super(element);
		homePageId = element.getAttribute(ATT_HOME_PAGE_ID);
		standbyPageId = element.getAttribute(ATT_STANDBY_PAGE_ID);
	}

	private void updatePresentationAttributes(IConfigurationElement element) {
		if (element != null) {
			// reset (ie: inherit) type and style to be implementation type and
			// style. Then handle HEAD content for the case of HTML Browser.
			String value = element.getAttribute(ATT_STYLE);
			if (value!=null) {
				IntroModelRoot root = getModelRoot();
				ArrayList<String> list = new ArrayList<>();
				StringTokenizer stok = new StringTokenizer(value, ","); //$NON-NLS-1$
				for (;stok.hasMoreTokens();) {
					String oneStyle = stok.nextToken().trim();
					if (root!=null)
						oneStyle = root.resolveVariables(oneStyle);
					list.add(oneStyle);
				}
				implementationStyles = list.toArray(new String[list.size()]);
			}
			implementationKind = element.getAttribute(ATT_KIND);
			// get Head contribution, regardless of implementation class.
			// Implementation class is created lazily by UI.
			head = getHead(element);
			// Resolve.
			if (implementationStyles!=null) {
				for (int i=0; i<implementationStyles.length; i++) {
					implementationStyles[i] = ModelUtil.resolveURL(implementationStyles[i], element);
				}
			}
		}
	}

	/**
	 * Returns the styles associated with the Presentation. May be null if no shared presentation
	 * style is needed, or in the case of static HTML OOBE.
	 *
	 * @return Returns the array of styles or null if not defined.
	 */
	public String[] getImplementationStyles() {
		return implementationStyles;
	}

	/**
	 * Returns the type attribute of the implementation picked by this presentation.
	 *
	 * @return Returns the implementationKind.
	 */
	public String getImplementationKind() {
		return implementationKind;
	}

	public AbstractIntroPartImplementation getIntroPartImplementation() {
		return implementation;
	}


	/**
	 * Returns the model class for the Head element under an implementation. Returns null if there
	 * is no head contribution.
	 *
	 * @param element
	 * @return
	 */
	private IntroHead getHead(IConfigurationElement element) {
		try {
			// There should only be one head element. Since elements where
			// obtained by name, no point validating name.
			IConfigurationElement[] headElements = element.getChildren(IntroHead.TAG_HEAD);
			if (headElements.length == 0)
				// no contributions. done.
				return null;
			IntroHead head = new IntroHead(headElements[0]);
			head.setParent(this);
			return head;
		} catch (Exception e) {
			Log.error(e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Returns the launch bar element if defined in this presentation, or <code>null</code>
	 * otherwise.
	 *
	 * @since 3.1
	 * @return
	 */

	public IntroLaunchBarElement getLaunchBarElement() {
		if (launchBar != null)
			return launchBar;
		IConfigurationElement[] children = getCfgElement().getChildren("launchBar"); //$NON-NLS-1$
		if (children.length > 0) {
			launchBar = new IntroLaunchBarElement(children[0]);
			launchBar.setParent(this);
			if (children.length > 1)
				Log
						.warning("Mutiple Intro Launch bars defined when only one is allowed. Only first one was loaded. "); //$NON-NLS-1$
		}
		return launchBar;
	}

	/**
	 * @param introPart
	 */
	public void init(IIntroPart introPart, IMemento memento) {
		// REVISIT: Called when the actual UI needs to be created. Incomplete
		// separation of model / UI. Will change later. should not get here if
		// there is no valid implementation.
		this.introPart = introPart;
		this.memento = memento;
	}

	/**
	 * Creates the UI based on the implementation class.
	 *
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPartControl(Composite parent) {
		Vector validImplementations = getValidImplementationElements(getCfgElement());
		IConfigurationElement implementationElement = null;
		for (int i = 0; i < validImplementations.size(); i++) {
			implementationElement = (IConfigurationElement) validImplementations.elementAt(i);
			// you want to pass primed model.
			updatePresentationAttributes(implementationElement);
			try {
				implementation = createIntroPartImplementation(getImplementationKind());
				if (implementation == null)
					// failed to create executable.
					continue;

				implementation.init(introPart, memento);
				implementation.createPartControl(parent);
				IntroModelRoot model = getModelRoot();
				if (model != null && model.getConfigurer() != null) {
					IntroTheme theme = model.getTheme();
					Map<String, String> properties = theme != null ? theme.getProperties() : null;
					model.getConfigurer().init(introPart.getIntroSite(), properties);
				}
				if (Log.logInfo)
					Log.info("Loading Intro UI implementation from: " //$NON-NLS-1$
							+ ModelLoaderUtil.getLogString(implementationElement, "kind")); //$NON-NLS-1$
				break;
			} catch (SWTError e) {
				Log.warning("Failed to create Intro UI implementation from: " //$NON-NLS-1$
						+ ModelLoaderUtil.getLogString(implementationElement, "kind") + e.getMessage()); //$NON-NLS-1$
				implementation = null;
				implementationElement = null;
			} catch (Exception e) {
				Log.error("Failed to create Intro UI implementation from: " //$NON-NLS-1$
						+ ModelLoaderUtil.getLogString(implementationElement, "kind"), e); //$NON-NLS-1$
				implementation = null;
				implementationElement = null;
			}
		}

		if (implementationElement == null) {
			// worst case scenario. We failed in all cases.
			implementation = new FormIntroPartImplementation();
			try {
				implementation.init(introPart, memento);
				// simply set the presentation kind since all other attributes
				// will be null.
				implementationKind = FORMS_IMPL_KIND;
			} catch (Exception e) {
				// should never be here.
				Log.error(e.getMessage(), e);
				return;
			}
			implementation.createPartControl(parent);
			Log.warning("Loaded UI Forms implementation as a default UI implementation."); //$NON-NLS-1$
		}
	}

	/**
	 * Retruns a list of valid implementation elements of the config. Choose correct implementation
	 * element based on os atrributes. Rules: get current OS, choose first contributrion, with os
	 * that matches OS. Otherwise, choose first contribution with no os. Returns null if no valid
	 * implementation is found.
	 */
	private Vector getValidImplementationElements(IConfigurationElement configElement) {

		Vector<IConfigurationElement> validList = new Vector<>();

		// There can be more than one implementation contribution. Add each
		// valid one. First start with OS, then WS then no OS.
		IConfigurationElement[] implementationElements = configElement.getChildren(TAG_IMPLEMENTATION);
		// IConfigurationElement implementationElement = null;

		if (implementationElements.length == 0)
			// no contributions. done.
			return validList;

		String currentOS = Platform.getOS();
		String currentWS = Platform.getWS();

		// first loop through all to find one with matching OS, with or
		// without WS.
		for (int i = 0; i < implementationElements.length; i++) {
			String os = implementationElements[i].getAttribute(ATT_OS);
			if (os == null)
				// no os, no match.
				continue;

			if (listValueHasValue(os, currentOS)) {
				// found implementation with correct OS. Now try if WS
				// matches.
				String ws = implementationElements[i].getAttribute(ATT_WS);
				if (ws == null) {
					// good OS, and they do not care about WS. we have a
					// match.
					validList.add(implementationElements[i]);
				} else {
					// good OS, and we have WS.
					if (listValueHasValue(ws, currentWS))
						validList.add(implementationElements[i]);
				}
			}
		}

		// now loop through all to find one with no OS defined, but with a
		// matching WS.
		for (int i = 0; i < implementationElements.length; i++) {
			String os = implementationElements[i].getAttribute(ATT_OS);
			if (os == null) {
				// found implementation with no OS. Now try if WS
				// matches.
				String ws = implementationElements[i].getAttribute(ATT_WS);
				if (ws == null) {
					// no OS, and they do not care about WS. we have a
					// match.
					validList.add(implementationElements[i]);
				} else {
					// no OS, and we have WS.
					if (listValueHasValue(ws, currentWS))
						validList.add(implementationElements[i]);
				}

			}
		}

		return validList;

	}

	/**
	 * Util method that searches for the given value in a comma separated list of values. The list
	 * is retrieved as an attribute value of OS, WS.
	 *
	 */
	private boolean listValueHasValue(String stringValue, String value) {
		String[] attributeValues = stringValue.split(","); //$NON-NLS-1$
		for (int i = 0; i < attributeValues.length; i++) {
			if (attributeValues[i].equalsIgnoreCase(value))
				return true;
		}
		return false;
	}

	/**
	 * Util method to load shared style from given kind.
	 */
	public String getSharedStyle(String kind) {
		// There can be more than one implementation contribution.
		IConfigurationElement[] implementationElements = getCfgElement().getChildren(TAG_IMPLEMENTATION);
		// IConfigurationElement implementationElement = null;

		if (implementationElements.length == 0)
			// no implementations. done.
			return null;

		// loop through all to find one with matching kind.
		for (int i = 0; i < implementationElements.length; i++) {
			String aKind = implementationElements[i].getAttribute(ATT_KIND);
			if (aKind.equals(kind)) {
				// found implementation with matching kind.
				String style = implementationElements[i].getAttribute(ATT_STYLE);
				return ModelUtil.resolveURL(style, getCfgElement());
			}
		}
		return null;
	}


	/**
	 * Creates the actual implementation class. Returns null on failure.
	 *
	 */
	private AbstractIntroPartImplementation createIntroPartImplementation(String implementationType) {
		// quick exits
		if (implementationType == null)
			return null;
		if (!implementationType.equals(BROWSER_IMPL_KIND) && !implementationType.equals(FORMS_IMPL_KIND)
				&& !implementationType.equals(TEXT_IMPL_KIND))
			return null;
		if (implementationType.equals(BROWSER_IMPL_KIND) && IntroPlugin.DEBUG_NO_BROWSER)
			return null;

		AbstractIntroPartImplementation implementation = null;
		try {
			switch (implementationType) {
			case BROWSER_IMPL_KIND:
				implementation = //null;
				new BrowserIntroPartImplementation();
				break;
			case FORMS_IMPL_KIND:
				implementation = new FormIntroPartImplementation();
				break;
			default:
				implementation = new TextIntroPartImplementation();
				break;
			}
		} catch (Exception e) {
			Log.error("Could not instantiate implementation " //$NON-NLS-1$
					+ implementationType, e);
		}
		return implementation;
	}

	/**
	 * Returns the the Customizable Intro Part. may return null if init() has not been called yet on
	 * the presentation.
	 *
	 * @return Returns the introPart.
	 */
	public IIntroPart getIntroPart() {
		return introPart;
	}

	/**
	 * Save the current state of the intro. Delegate to the implementation to do the work, as
	 * different implementations may have different requirements.
	 *
	 * @param memento
	 *            the memento in which to store state information
	 */
	public void saveState(IMemento memento) {
		if (implementation != null)
			implementation.saveState(memento);
	}


	public void setFocus() {
		if (implementation != null)
			implementation.setFocus();
	}

	public void standbyStateChanged(boolean standby, boolean isStandbyPartNeeded) {
		if (implementation != null)
			implementation.standbyStateChanged(standby, isStandbyPartNeeded);
	}

	public void updateHistory(AbstractIntroPage page) {
		if (implementation != null)
			implementation.updateHistory(page);
	}



	public boolean navigateForward() {
		if (implementation != null)
			return implementation.navigateForward();
		return false;
	}

	public boolean navigateBackward() {
		if (implementation != null)
			return implementation.navigateBackward();
		return false;
	}

	public boolean navigateHome() {
		if (implementation != null)
			return implementation.navigateHome();
		return false;
	}


	/**
	 * Called when the IntroPart is disposed. Forwards the call to the implementation class.
	 */
	public void dispose() {
		if (implementation != null)
			implementation.dispose();
	}

	/**
	 * Support dynamic awarness. Clear cached models first, then update UI by delegating to
	 * implementation.
	 *
	 * @see org.eclipse.core.runtime.IRegistryChangeListener#registryChanged(org.eclipse.core.runtime.IRegistryChangeEvent)
	 */
	public void registryChanged(IRegistryChangeEvent event) {
		if (implementation != null)
			implementation.registryChanged(event);
	}

	/**
	 * @return Returns the homePageId.
	 */
	public String getHomePageId() {
		return homePageId;
	}

	/**
	 * @return Returns the homePageId.
	 */
	public String getStandbyPageId() {
		return standbyPageId;
	}

	@Override
	public int getType() {
		return AbstractIntroElement.PRESENTATION;
	}

	/**
	 * @return Returns the HTML head conttent to be added to each dynamic html page in this
	 *         presentation..
	 */
	public IntroHead getHead() {
		return head;
	}




}
