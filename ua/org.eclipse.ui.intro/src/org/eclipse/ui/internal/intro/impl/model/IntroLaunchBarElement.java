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
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.intro.impl.util.ImageUtil;
import org.eclipse.ui.intro.config.IntroConfigurer;
import org.eclipse.ui.intro.config.IntroElement;

/**
 * An Intro Config component captures launch bar information. It can have
 * shortcuts and one handle. <br>
 * ps: Handles are not modeled in a dedicated class, but are handled here.
 *
 * @since 3.1
 */
public class IntroLaunchBarElement extends AbstractIntroElement {

	private List<IntroLaunchBarShortcut> shortcuts;

	IntroLaunchBarElement(IConfigurationElement element) {
		super(element);
	}


	/**
	 * Returns LAUNCH_BAR.
	 */
	@Override
	public int getType() {
		return AbstractIntroElement.LAUNCH_BAR;
	}

	/**
	 * Returns the desired launch bar orientation that results from the desired
	 * location. Valid values are <code>SWT.VERTICAL</code> and
	 * <code>SWT.HORIZONTAL</code>.
	 *
	 * @return
	 */
	public int getOrientation() {
		int location = getLocation();
		return (location == SWT.RIGHT || location == SWT.LEFT) ? SWT.VERTICAL
				: SWT.HORIZONTAL;
	}

	/**
	 * Returns the location of the launch bar in the workbench window. Valid
	 * values are <code>SWT.RIGHT</code>,<code>SWT.LEFT</code> and
	 * <code>SWT.BOTTOM</code>.
	 *
	 * @return
	 */
	public int getLocation() {
		String location = getCfgElement().getAttribute("location"); //$NON-NLS-1$

		if (location != null) {
			if (location.equals("left")) //$NON-NLS-1$
				return SWT.LEFT;
			if (location.equals("bottom")) //$NON-NLS-1$
				return SWT.BOTTOM;
			if (location.equals("right")) //$NON-NLS-1$
				return SWT.RIGHT;
		}
		// default to the initial fast view location
		String fastviewLocation = PlatformUI.getPreferenceStore().getString(
			IWorkbenchPreferenceConstants.INITIAL_FAST_VIEW_BAR_LOCATION);
		if (fastviewLocation.equals(IWorkbenchPreferenceConstants.LEFT))
			return SWT.LEFT;
		if (fastviewLocation.equals(IWorkbenchPreferenceConstants.RIGHT))
			return SWT.RIGHT;
		if (fastviewLocation.equals(IWorkbenchPreferenceConstants.BOTTOM))
			return SWT.BOTTOM;
		// just in case
		return SWT.RIGHT;
	}

	public String getBackground() {
		return getCfgElement().getAttribute("bg"); //$NON-NLS-1$
	}

	public String getForeground() {
		return getCfgElement().getAttribute("fg"); //$NON-NLS-1$
	}

	public boolean getCreateHandle() {
		return getHandleElement() != null;
	}

	public boolean getClose() {
		IConfigurationElement handle = getHandleElement();
		if (handle != null) {
			String value = handle.getAttribute("close"); //$NON-NLS-1$
			return value == null || value.equals("true"); //$NON-NLS-1$
		}
		return true;
	}

	/**
	 * Returns the relative icon path of the handle image.
	 *
	 * @return
	 */
	private String getHandleImage() {
		IConfigurationElement handle = getHandleElement();
		if (handle == null)
			return null;
		return handle.getAttribute("image"); //$NON-NLS-1$
	}

	/**
	 * Returns the icon image of the handle, or <code>null</code> if not
	 * defined or found.
	 *
	 * @return
	 */
	public ImageDescriptor getHandleImageDescriptor() {
		String path = getHandleImage();
		if (path == null)
			return null;
		return ImageUtil.createImageDescriptor(getBundle(), path);
	}

	private IConfigurationElement getHandleElement() {
		IConfigurationElement[] children = getCfgElement().getChildren(
			"handle"); //$NON-NLS-1$
		if (children.length > 0)
			return children[0];
		return null;
	}

	/**
	 * Returns an array of shorcut elements.
	 *
	 * @return
	 */
	public IntroLaunchBarShortcut[] getShortcuts() {
		if (shortcuts == null) {
			createShortcuts();
		}
		return shortcuts.toArray(new IntroLaunchBarShortcut[shortcuts.size()]);
	}

	/**
	 * Creates an array of shortcut elements
	 *
	 */
	private void createShortcuts() {
		shortcuts = new ArrayList<>();
		IntroModelRoot model = getModelRoot();
		IntroConfigurer configurer = model!=null?model.getConfigurer():null;

		String cvalue = getCfgElement().getAttribute("computed"); //$NON-NLS-1$
		boolean computed = cvalue!=null && cvalue.equalsIgnoreCase("true"); //$NON-NLS-1$

		if (computed && configurer!=null) {
			IntroElement [] children = configurer.getLaunchBarShortcuts();
			for (int i=0; i<children.length; i++) {
				IntroLaunchBarShortcut shortcut = new IntroLaunchBarShortcut(getCfgElement(), children[i]);
				shortcuts.add(shortcut);
			}
		}
		else {
			IConfigurationElement[] children = getCfgElement().getChildren(
					IntroLaunchBarShortcut.TAG_SHORTCUT);
			for (int i = 0; i < children.length; i++) {
				IConfigurationElement child = children[i];
				IntroLaunchBarShortcut shortcut = new IntroLaunchBarShortcut(child);
				shortcuts.add(shortcut);
			}
		}
	}
}
