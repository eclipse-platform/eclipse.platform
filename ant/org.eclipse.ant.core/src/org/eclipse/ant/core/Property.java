/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.VariablesPlugin;

/**
 * Represents a Ant property.
 * Clients may instantiate this class; it is not intended to be subclassed.
 * @since 2.1
 */
public class Property {

	private String name;
	private String value;
	private String className;
	private IAntPropertyValueProvider valueProvider;
	private String pluginLabel;
	private ClassLoader loader;
	private boolean eclipseRuntime= true;

	public Property(String name, String value) {
		this.name= name;
		this.value= value;
	}

	public Property() {
	}
	
	/**
	 * Gets the name
	 * @return Returns a String
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name
	 * @param name The name to set
	 */
	public void setName(String name) {
		this.name= name;
	}
	
	/*
	 * @see Object#equals()
	 */	
	public boolean equals(Object other) {
		if (other.getClass().equals(getClass())) {
			Property elem= (Property)other;
			return name.equals(elem.getName());
		}
		return false;
	}
	
	/*
	 * @see Object#hashCode()
	 */
	public int hashCode() {
		return name.hashCode();
	}
	
	/**
	 * Returns the value.
	 * Equivalent to calling #getValue(true);
	 * @return String
	 */
	public String getValue() {
		return getValue(true);
	}
	
	/**
	 * Returns the value.
	 * 
	 * @param substituteVariables whether the value has any variables resolved.
	 * @return String
	 * @since 3.0
	 */
	public String getValue(boolean substituteVariables) {
		if (className != null) {
			Class cls = null;
			try {
				cls = loader.loadClass(className);
			} catch (ClassNotFoundException e) {
				AntCorePlugin.log(e);
				return null;
			}
			try {
				valueProvider = (IAntPropertyValueProvider)cls.newInstance();
			} catch (InstantiationException e) {
				AntCorePlugin.log(e);
				return null;
			} catch (IllegalAccessException ex) {
				AntCorePlugin.log(ex);
				return null;
			}
			loader= null;
			className= null;
		}
		
		if (valueProvider != null) {
			return valueProvider.getAntPropertyValue(name);
		} 
		if (substituteVariables) {
			try {
				String expanded = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(value);
				return expanded;
			} catch (CoreException e) {
			}
		} else {
			return value;
		}
		return value;
	}

	/**
	 * Sets the value.
	 * @param value The value to set
	 */
	public void setValue(String value) {
		this.value = value;
	}
	
	/**
	 * Returns whether this Ant property has been created because of an extension
	 * point definition.
	 * 
	 * @return boolean
	 * @since 3.0
	 */
	public boolean isDefault() {
		return pluginLabel != null;
	}

	/**
	 * Sets the label of the plugin that contributed this Ant property via an extension
	 * point.
	 * 
	 * @param pluginLabel The label of the plugin
	 * @since 3.0
	 */
	public void setPluginLabel(String pluginLabel) {
		this.pluginLabel = pluginLabel;
	}
	
	/**
	 * Returns the label of the plugin that contributed this Ant property via an extension
	 * point.
	 * 
	 * @return pluginLabel The label of the plugin
	 * @since 3.0
	 */
	public String getPluginLabel() {
		return this.pluginLabel;
	}
	
	/**
	 * Sets the name of the class that is an <code>IAntPropertyValueProvider</code> to be used to dynamically provide a 
	 * value for this property.
	 * Sets the class loader to load the <code>IAntPropertyValueProvider</code> to be used to dynamically provide a 
	 * value for this property.
	 * 
	 * @param className The name of the value provider class to use to resolve the value of this property
	 * @param loader The class loader to use to load the value provider class to use to resolve the value of this property
	 * @since 3.0
	 */
	public void setValueProvider(String className, ClassLoader loader) {
		this.className= className;
		this.loader= loader;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buff= new StringBuffer("\""); //$NON-NLS-1$
		buff.append(getName());
		buff.append("\"= \""); //$NON-NLS-1$
		buff.append(getValue(false));
		buff.append("\""); //$NON-NLS-1$
		return buff.toString();
	}
	
	/**
	 * Returns whether this property requires the Eclipse runtime to be 
	 * relevant. Defaults value is <code>true</code>
	 * 
	 * @return whether this property requires the Eclipse runtime
     * @since 3.0
	 */
	public boolean isEclipseRuntimeRequired() {
		return eclipseRuntime;
	}
	
	public void setEclipseRuntimeRequired(boolean eclipseRuntime) {
		this.eclipseRuntime= eclipseRuntime;
	}
}
