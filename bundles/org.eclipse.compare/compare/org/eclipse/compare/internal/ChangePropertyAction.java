/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alex Blewitt <alex.blewitt@gmail.com> - replace new Boolean with Boolean.valueOf - https://bugs.eclipse.org/470344
 *******************************************************************************/
package org.eclipse.compare.internal;

import java.util.ResourceBundle;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.compare.CompareConfiguration;

/**
 * Toggles a boolean property of an <code>CompareConfiguration</code>.
 */
public class ChangePropertyAction extends Action implements IPropertyChangeListener, DisposeListener {

	private CompareConfiguration fCompareConfiguration;
	private String fPropertyKey;
	private ResourceBundle fBundle;
	private String fPrefix;

	public static ChangePropertyAction createIgnoreWhiteSpaceAction(ResourceBundle bundle, CompareConfiguration compareConfiguration) {
		return new ChangePropertyAction(bundle, compareConfiguration, "action.IgnoreWhiteSpace.", CompareConfiguration.IGNORE_WHITESPACE); //$NON-NLS-1$
	}
	public static ChangePropertyAction createShowPseudoConflictsAction(ResourceBundle bundle, CompareConfiguration compareConfiguration) {
		return new ChangePropertyAction(bundle, compareConfiguration, "action.ShowPseudoConflicts.", CompareConfiguration.SHOW_PSEUDO_CONFLICTS); //$NON-NLS-1$
	}

	public ChangePropertyAction(ResourceBundle bundle, CompareConfiguration cc, String rkey, String pkey) {
		fPropertyKey= pkey;
		fBundle= bundle;
		fPrefix= rkey;
		Utilities.initAction(this, fBundle, fPrefix);
		setCompareConfiguration(cc);
	}

	public void run() {
		boolean b= !Utilities.getBoolean(fCompareConfiguration, fPropertyKey, false);
		setChecked(b);
		if (fCompareConfiguration != null)
			fCompareConfiguration.setProperty(fPropertyKey, Boolean.valueOf(b));
	}

	public void setChecked(boolean state) {
		super.setChecked(state);
		Utilities.initToggleAction(this, fBundle, fPrefix, state);
	}
	
	public void setCompareConfiguration(CompareConfiguration cc) {
		if (fCompareConfiguration != null)
			fCompareConfiguration.removePropertyChangeListener(this);
		fCompareConfiguration= cc;
		if (fCompareConfiguration != null)
			fCompareConfiguration.addPropertyChangeListener(this);
		setChecked(Utilities.getBoolean(fCompareConfiguration, fPropertyKey, false));
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(fPropertyKey)) {
			setChecked(Utilities.getBoolean(fCompareConfiguration, fPropertyKey, false));
		}
	}
	
	public void dispose(){
		if (fCompareConfiguration != null)
			fCompareConfiguration.removePropertyChangeListener(this);
	}
	
	public void widgetDisposed(DisposeEvent e) {
		dispose();
	}
}
