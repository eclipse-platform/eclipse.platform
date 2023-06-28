/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.core.internal.variables;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.variables.IValueVariable;
import org.eclipse.core.variables.IValueVariableInitializer;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.osgi.util.NLS;

/**
 * Implementation of a value variable.
 */
public class ContributedValueVariable extends StringVariable implements IValueVariable {

	/**
	 * Variable value or <code>null</code> if none
	 */
	private String fValue;

	/**
	 * Whether this variable's value has been initialized
	 */
	private boolean fInitialized = false;

	/**
	 * Whether this variable is read only.  If true, users cannot change the value.
	 */
	private boolean fReadOnly;

	/**
	 * Constructs a new value variable with the given name, description, read only
	 * property and associated configuration element.  The value will be initialized
	 * from the configuration element the first time getValue() is called.
	 *
	 * @param name variable name
	 * @param description variable description or <code>null</code>
	 * @param readOnly whether the variable should be a read only variable
	 * @param configurationElement configuration element
	 */
	public ContributedValueVariable(String name, String description, boolean readOnly, IConfigurationElement configurationElement) {
		super(name, description, configurationElement);
		fReadOnly = readOnly;
	}

	@Override
	public void setValue(String value) {
		if (!isReadOnly() || !isInitialized()){
			fValue = value;
			setInitialized(true);
			StringVariableManager.getDefault().notifyChanged(this);
		}
	}

	@Override
	public String getValue() {
		if (!isInitialized()) {
			initialize();
		}
		return fValue;
	}

	/**
	 * Initialize this variable's value from the configuration element.
	 */
	private void initialize() {
		if (getConfigurationElement() != null) {
			// check for a explicit value specified in plug-in XML
			String value = getConfigurationElement().getAttribute("initialValue"); //$NON-NLS-1$
			if (value == null) {
				// check for initializer
				String className = getConfigurationElement().getAttribute("initializerClass"); //$NON-NLS-1$
				if (className != null) {
					try {
						Object object = getConfigurationElement().createExecutableExtension("initializerClass"); //$NON-NLS-1$
						if (object instanceof IValueVariableInitializer) {
							IValueVariableInitializer initializer = (IValueVariableInitializer)object;
							initializer.initialize(this);
						} else {
							VariablesPlugin.logMessage(NLS.bind("Unable to initialize variable {0} - initializer must be an instance of IValueVariableInitializer.", new String[]{getName()}), null); //$NON-NLS-1$
						}
					} catch (CoreException e) {
						VariablesPlugin.logMessage(NLS.bind("Unable to initialize variable {0}",new String[]{getName()}), e); //$NON-NLS-1$
					}
				}
			} else {
				setValue(value);
			}
		}
		setInitialized(true);
	}

	/**
	 * Returns whether this variable has been initialized with a value by one of:
	 * <ul>
	 * <li><code>setValue(String)</code></li>
	 * <li>its configuration element's <code>initialValue</code> attribute</li>
	 * <li>its configuration element's initializer</li>
	 * </ul>
	 * @return whether this variable has been initialized with a value
	 */
	protected boolean isInitialized() {
		return fInitialized;
	}

	/**
	 * Sets whether this variable has been initialized with a value.
	 *
	 * @param initialized whether this variable has been initialized
	 */
	protected void setInitialized(boolean initialized) {
		fInitialized = initialized;
	}

	@Override
	public boolean isReadOnly() {
		return fReadOnly;
	}

	@Override
	public boolean isContributed() {
		return getConfigurationElement() != null;
	}

}
