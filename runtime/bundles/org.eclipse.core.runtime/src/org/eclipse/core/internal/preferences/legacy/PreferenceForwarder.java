/*******************************************************************************
 *  Copyright (c) 2004, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.preferences.legacy;

import java.io.*;
import java.util.Properties;
import org.eclipse.core.internal.preferences.*;
import org.eclipse.core.internal.runtime.RuntimeLog;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.preferences.*;
import org.osgi.service.prefs.BackingStoreException;

/**
 * This class represents a convenience layer between the Eclipse 3.0
 * preferences and pre-3.0 preferences. It acts as a bridge between the
 * org.eclipse.core.runtime.Preferences object associated with a particular plug-in
 * object, and its corresponding preference node in the 3.0 preference node
 * hierarchy.
 *
 * @since 3.0
 * @deprecated to avoid warnings for interacting with deprecated Preferences class.
 */
@Deprecated
public class PreferenceForwarder extends Preferences implements IEclipsePreferences.IPreferenceChangeListener, IEclipsePreferences.INodeChangeListener {

	private static final byte[] BYTE_ARRAY_DEFAULT_DEFAULT = new byte[0];

	private IEclipsePreferences pluginRoot = (IEclipsePreferences) PreferencesService.getDefault().getRootNode().node(InstanceScope.SCOPE);
	private DefaultPreferences defaultsRoot = (DefaultPreferences) PreferencesService.getDefault().getRootNode().node(DefaultScope.SCOPE);
	private String pluginID;
	private Object plugin;
	// boolean to check to see if we should re-wrap and forward change
	// events coming from the new runtime APIs.
	private boolean notify = true;

	/*
	 * Used for test suites only.
	 */
	public PreferenceForwarder(String pluginID) {
		this(null, pluginID);
	}

	public PreferenceForwarder(Object plugin, String pluginID) {
		super();
		this.plugin = plugin;
		this.pluginID = pluginID;
	}

	/*
	 * @see org.eclipse.core.runtime.preferences.IEclipsePreferences.INodeChangeListener#added(org.eclipse.core.runtime.preferences.IEclipsePreferences.NodeChangeEvent)
	 */
	@Override
	public synchronized void added(IEclipsePreferences.NodeChangeEvent event) {
		if (listeners.size() > 0 && pluginID.equals(event.getChild().name())) {
			try {
				EclipsePreferences prefs = (EclipsePreferences) event.getChild();
				prefs.addPreferenceChangeListener(this);
			} catch (ClassCastException e) {
				throw new RuntimeException("Plug-in preferences must be instances of EclipsePreferences: " + e.getMessage()); //$NON-NLS-1$
			}
		}
	}

	/*
	 * @see org.eclipse.core.runtime.preferences.IEclipsePreferences.INodeChangeListener#removed(org.eclipse.core.runtime.preferences.IEclipsePreferences.NodeChangeEvent)
	 */
	@Override
	public synchronized void removed(IEclipsePreferences.NodeChangeEvent event) {
		// Do nothing. We can't remove ourselves from the node's list of preference change
		// listeners because the node has already been removed.
	}

	/**
	 * Adds a property change listener to this preference object.
	 * Has no effect if the identical listener is already registered.
	 *
	 * @param listener a property change listener
	 */
	@Override
	public synchronized void addPropertyChangeListener(IPropertyChangeListener listener) {
		if (listeners.size() == 0) {
			EclipsePreferences prefs = getPluginPreferences(false);
			if (prefs != null) {
				prefs.addPreferenceChangeListener(this);
			}
			pluginRoot.addNodeChangeListener(this);
		}
		listeners.add(listener);
	}


	/*
	 * @see org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener#preferenceChange(org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent)
	 */
	@Override
	public void preferenceChange(IEclipsePreferences.PreferenceChangeEvent event) {
		// if we are the ones making this change, then don't broadcast
		if (!notify)
			return;
		Object oldValue = event.getOldValue();
		Object newValue = event.getNewValue();
		String key = event.getKey();
		if (newValue == null)
			newValue = getDefault(key, oldValue);
		else if (oldValue == null)
			oldValue = getDefault(key, newValue);
		firePropertyChangeEvent(key, oldValue, newValue);
	}

	private EclipsePreferences getPluginPreferences(boolean create) {
		try {
			if (!create && !pluginRoot.nodeExists(pluginID))
				return null;
		} catch (BackingStoreException e) {
			return null;
		}
		try {
			return (EclipsePreferences) pluginRoot.node(pluginID);
		} catch (ClassCastException e) {
			throw new RuntimeException("Plug-in preferences must be instances of EclipsePreferences: " + e.getMessage()); //$NON-NLS-1$
		}
	}

	private IEclipsePreferences getDefaultPreferences() {
		return defaultsRoot.node(pluginID, plugin);
	}

	/**
	 * Removes the given listener from this preference object.
	 * Has no effect if the listener is not registered.
	 *
	 * @param listener a property change listener
	 */
	@Override
	public synchronized void removePropertyChangeListener(IPropertyChangeListener listener) {
		listeners.remove(listener);
		if (listeners.size() == 0) {
			EclipsePreferences prefs = getPluginPreferences(false);
			if (prefs != null) {
				prefs.removePreferenceChangeListener(this);
			}
			pluginRoot.removeNodeChangeListener(this);
		}
	}


	/**
	 * Does its best at determining the default value for the given key. Checks the
	 * given object's type and then looks in the list of defaults to see if a value
	 * exists. If not or if there is a problem converting the value, the default default
	 * value for that type is returned.
	 */
	private Object getDefault(String key, Object obj) {
		IEclipsePreferences defaults = getDefaultPreferences();
		if (obj instanceof String)
			return defaults.get(key, STRING_DEFAULT_DEFAULT);
		else if (obj instanceof Integer)
			return Integer.valueOf(defaults.getInt(key, INT_DEFAULT_DEFAULT));
		else if (obj instanceof Double)
			return Double.valueOf(defaults.getDouble(key, DOUBLE_DEFAULT_DEFAULT));
		else if (obj instanceof Float)
			return Float.valueOf(defaults.getFloat(key, FLOAT_DEFAULT_DEFAULT));
		else if (obj instanceof Long)
			return Long.valueOf(defaults.getLong(key, LONG_DEFAULT_DEFAULT));
		else if (obj instanceof byte[])
			return defaults.getByteArray(key, BYTE_ARRAY_DEFAULT_DEFAULT);
		else if (obj instanceof Boolean)
			return defaults.getBoolean(key, BOOLEAN_DEFAULT_DEFAULT) ? Boolean.TRUE : Boolean.FALSE;
		else
			return null;
	}

	/**
	 * Returns whether the given property is known to this preference object,
	 * either by having an explicit setting or by having a default
	 * setting.
	 *
	 * @param name the name of the property
	 * @return <code>true</code> if either a current value or a default
	 *  value is known for the named property, and <code>false</code>otherwise
	 */
	@Override
	public boolean contains(String name) {
		if (name == null)
			return false;
		String value = getPluginPreferences(true).get(name, null);
		if (value != null)
			return true;
		return getDefaultPreferences().get(name, null) != null;
	}

	/**
	 * Returns the current value of the boolean-valued property with the
	 * given name.
	 * Returns the default-default value (<code>false</code>) if there
	 * is no property with the given name, or if the current value
	 * cannot be treated as a boolean.
	 *
	 * @param name the name of the property
	 * @return the boolean-valued property
	 */
	@Override
	public boolean getBoolean(String name) {
		return getPluginPreferences(true).getBoolean(name, getDefaultPreferences().getBoolean(name, BOOLEAN_DEFAULT_DEFAULT));
	}

	/**
	 * Sets the current value of the boolean-valued property with the
	 * given name.
	 * <p>
	 * A property change event is reported if the current value of the
	 * property actually changes from its previous value. In the event
	 * object, the property name is the name of the property, and the
	 * old and new values are wrapped as objects.
	 * </p>
	 * <p>
	 * If the given value is the same as the corresponding default value
	 * for the given property, the explicit setting is deleted.
	 * Note that the recommended way of re-initializing a property to its
	 * default value is to call <code>setToDefault</code>.
	 * </p>
	 *
	 * @param name the name of the property
	 * @param value the new current value of the property
	 */
	@Override
	public void setValue(String name, boolean value) {
		Boolean oldValue = getBoolean(name) ? Boolean.TRUE : Boolean.FALSE;
		Boolean newValue = value ? Boolean.TRUE : Boolean.FALSE;
		if (newValue == oldValue)
			return;
		try {
			notify = false;
			if (getDefaultBoolean(name) == value)
				getPluginPreferences(true).remove(name);
			else
				getPluginPreferences(true).putBoolean(name, value);
			firePropertyChangeEvent(name, oldValue, newValue);
		} finally {
			notify = true;
		}
	}

	/**
	 * Returns the default value for the boolean-valued property
	 * with the given name.
	 * Returns the default-default value (<code>false</code>) if there
	 * is no default property with the given name, or if the default
	 * value cannot be treated as a boolean.
	 *
	 * @param name the name of the property
	 * @return the default value of the named property
	 */
	@Override
	public boolean getDefaultBoolean(String name) {
		return getDefaultPreferences().getBoolean(name, BOOLEAN_DEFAULT_DEFAULT);
	}

	/**
	 * Sets the default value for the boolean-valued property with the
	 * given name.
	 * <p>
	 * Note that the current value of the property is affected if
	 * the property's current value was its old default value, in which
	 * case it changes to the new default value. If the property's current
	 * is different from its old default value, its current value is
	 * unaffected. No property change events are reported by changing default
	 * values.
	 * </p>
	 *
	 * @param name the name of the property
	 * @param value the new default value for the property
	 */
	@Override
	public void setDefault(String name, boolean value) {
		getDefaultPreferences().putBoolean(name, value);
	}

	/**
	 * Returns the current value of the double-valued property with the
	 * given name.
	 * Returns the default-default value (<code>0.0</code>) if there
	 * is no property with the given name, or if the current value
	 * cannot be treated as a double.
	 *
	 * @param name the name of the property
	 * @return the double-valued property
	 */
	@Override
	public double getDouble(String name) {
		return getPluginPreferences(true).getDouble(name, getDefaultPreferences().getDouble(name, DOUBLE_DEFAULT_DEFAULT));
	}

	/**
	 * Sets the current value of the double-valued property with the
	 * given name.
	 * <p>
	 * A property change event is reported if the current value of the
	 * property actually changes from its previous value. In the event
	 * object, the property name is the name of the property, and the
	 * old and new values are wrapped as objects.
	 * </p>
	 * <p>
	 * If the given value is the same as the corresponding default value
	 * for the given property, the explicit setting is deleted.
	 * Note that the recommended way of re-initializing a property to its
	 * default value is to call <code>setToDefault</code>.
	 * </p>
	 *
	 * @param name the name of the property
	 * @param value the new current value of the property; must be
	 *   a number (not a NaN)
	 */
	@Override
	public void setValue(String name, double value) {
		if (Double.isNaN(value))
			throw new IllegalArgumentException();
		final double doubleValue = getDouble(name);
		if (value == doubleValue)
			return;
		Double oldValue = Double.valueOf(doubleValue);
		Double newValue = Double.valueOf(value);
		try {
			notify = false;
			if (getDefaultDouble(name) == value)
				getPluginPreferences(true).remove(name);
			else
				getPluginPreferences(true).putDouble(name, value);
			firePropertyChangeEvent(name, oldValue, newValue);
		} finally {
			notify = true;
		}
	}

	/**
	 * Returns the default value for the double-valued property
	 * with the given name.
	 * Returns the default-default value (<code>0.0</code>) if there
	 * is no default property with the given name, or if the default
	 * value cannot be treated as a double.
	 *
	 * @param name the name of the property
	 * @return the default value of the named property
	 */
	@Override
	public double getDefaultDouble(String name) {
		return getDefaultPreferences().getDouble(name, DOUBLE_DEFAULT_DEFAULT);
	}

	/**
	 * Sets the default value for the double-valued property with the
	 * given name.
	 * <p>
	 * Note that the current value of the property is affected if
	 * the property's current value was its old default value, in which
	 * case it changes to the new default value. If the property's current
	 * is different from its old default value, its current value is
	 * unaffected. No property change events are reported by changing default
	 * values.
	 * </p>
	 *
	 * @param name the name of the property
	 * @param value the new default value for the property; must be
	 *   a number (not a NaN)
	 */
	@Override
	public void setDefault(String name, double value) {
		if (Double.isNaN(value))
			throw new IllegalArgumentException();
		getDefaultPreferences().putDouble(name, value);
	}

	/**
	 * Returns the current value of the float-valued property with the
	 * given name.
	 * Returns the default-default value (<code>0.0f</code>) if there
	 * is no property with the given name, or if the current value
	 * cannot be treated as a float.
	 *
	 * @param name the name of the property
	 * @return the float-valued property
	 */
	@Override
	public float getFloat(String name) {
		return getPluginPreferences(true).getFloat(name, getDefaultPreferences().getFloat(name, FLOAT_DEFAULT_DEFAULT));
	}

	/**
	 * Sets the current value of the float-valued property with the
	 * given name.
	 * <p>
	 * A property change event is reported if the current value of the
	 * property actually changes from its previous value. In the event
	 * object, the property name is the name of the property, and the
	 * old and new values are wrapped as objects.
	 * </p>
	 * <p>
	 * If the given value is the same as the corresponding default value
	 * for the given property, the explicit setting is deleted.
	 * Note that the recommended way of re-initializing a property to its
	 * default value is to call <code>setToDefault</code>.
	 * </p>
	 *
	 * @param name the name of the property
	 * @param value the new current value of the property; must be
	 *   a number (not a NaN)
	 */
	@Override
	public void setValue(String name, float value) {
		if (Float.isNaN(value))
			throw new IllegalArgumentException();
		final float floatValue = getFloat(name);
		if (value == floatValue)
			return;
		Float oldValue = Float.valueOf(floatValue);
		Float newValue = Float.valueOf(value);
		try {
			notify = false;
			if (getDefaultFloat(name) == value)
				getPluginPreferences(true).remove(name);
			else
				getPluginPreferences(true).putFloat(name, value);
			firePropertyChangeEvent(name, oldValue, newValue);
		} finally {
			notify = true;
		}
	}

	/**
	 * Returns the default value for the float-valued property
	 * with the given name.
	 * Returns the default-default value (<code>0.0f</code>) if there
	 * is no default property with the given name, or if the default
	 * value cannot be treated as a float.
	 *
	 * @param name the name of the property
	 * @return the default value of the named property
	 */
	@Override
	public float getDefaultFloat(String name) {
		return getDefaultPreferences().getFloat(name, FLOAT_DEFAULT_DEFAULT);
	}

	/**
	 * Sets the default value for the float-valued property with the
	 * given name.
	 * <p>
	 * Note that the current value of the property is affected if
	 * the property's current value was its old default value, in which
	 * case it changes to the new default value. If the property's current
	 * is different from its old default value, its current value is
	 * unaffected. No property change events are reported by changing default
	 * values.
	 * </p>
	 *
	 * @param name the name of the property
	 * @param value the new default value for the property; must be
	 *   a number (not a NaN)
	 */
	@Override
	public void setDefault(String name, float value) {
		if (Float.isNaN(value))
			throw new IllegalArgumentException();
		getDefaultPreferences().putFloat(name, value);
	}

	/**
	 * Returns the current value of the integer-valued property with the
	 * given name.
	 * Returns the default-default value (<code>0</code>) if there
	 * is no property with the given name, or if the current value
	 * cannot be treated as an integter.
	 *
	 * @param name the name of the property
	 * @return the int-valued property
	 */
	@Override
	public int getInt(String name) {
		return getPluginPreferences(true).getInt(name, getDefaultPreferences().getInt(name, INT_DEFAULT_DEFAULT));
	}

	/**
	 * Sets the current value of the integer-valued property with the
	 * given name.
	 * <p>
	 * A property change event is reported if the current value of the
	 * property actually changes from its previous value. In the event
	 * object, the property name is the name of the property, and the
	 * old and new values are wrapped as objects.
	 * </p>
	 * <p>
	 * If the given value is the same as the corresponding default value
	 * for the given property, the explicit setting is deleted.
	 * Note that the recommended way of re-initializing a property to its
	 * default value is to call <code>setToDefault</code>.
	 * </p>
	 *
	 * @param name the name of the property
	 * @param value the new current value of the property
	 */
	@Override
	public void setValue(String name, int value) {
		final int intValue = getInt(name);
		if (value == intValue)
			return;
		Integer oldValue = Integer.valueOf(intValue);
		Integer newValue = Integer.valueOf(value);
		try {
			notify = false;
			if (getDefaultInt(name) == value)
				getPluginPreferences(true).remove(name);
			else
				getPluginPreferences(true).putInt(name, value);
			firePropertyChangeEvent(name, oldValue, newValue);
		} finally {
			notify = true;
		}
	}

	/**
	 * Returns the default value for the integer-valued property
	 * with the given name.
	 * Returns the default-default value (<code>0</code>) if there
	 * is no default property with the given name, or if the default
	 * value cannot be treated as an integer.
	 *
	 * @param name the name of the property
	 * @return the default value of the named property
	 */
	@Override
	public int getDefaultInt(String name) {
		return getDefaultPreferences().getInt(name, INT_DEFAULT_DEFAULT);
	}

	/**
	 * Sets the default value for the integer-valued property with the
	 * given name.
	 * <p>
	 * Note that the current value of the property is affected if
	 * the property's current value was its old default value, in which
	 * case it changes to the new default value. If the property's current
	 * is different from its old default value, its current value is
	 * unaffected. No property change events are reported by changing default
	 * values.
	 * </p>
	 *
	 * @param name the name of the property
	 * @param value the new default value for the property
	 */
	@Override
	public void setDefault(String name, int value) {
		getDefaultPreferences().putInt(name, value);
	}

	/**
	 * Returns the current value of the long-valued property with the
	 * given name.
	 * Returns the default-default value (<code>0L</code>) if there
	 * is no property with the given name, or if the current value
	 * cannot be treated as a long.
	 *
	 * @param name the name of the property
	 * @return the long-valued property
	 */
	@Override
	public long getLong(String name) {
		return getPluginPreferences(true).getLong(name, getDefaultPreferences().getLong(name, LONG_DEFAULT_DEFAULT));
	}

	/**
	 * Sets the current value of the long-valued property with the
	 * given name.
	 * <p>
	 * A property change event is reported if the current value of the
	 * property actually changes from its previous value. In the event
	 * object, the property name is the name of the property, and the
	 * old and new values are wrapped as objects.
	 * </p>
	 * <p>
	 * If the given value is the same as the corresponding default value
	 * for the given property, the explicit setting is deleted.
	 * Note that the recommended way of re-initializing a property to its
	 * default value is to call <code>setToDefault</code>.
	 * </p>
	 *
	 * @param name the name of the property
	 * @param value the new current value of the property
	 */
	@Override
	public void setValue(String name, long value) {
		final long longValue = getLong(name);
		if (value == longValue)
			return;
		Long oldValue = Long.valueOf(longValue);
		Long newValue = Long.valueOf(value);
		try {
			notify = false;
			if (getDefaultLong(name) == value)
				getPluginPreferences(true).remove(name);
			else
				getPluginPreferences(true).putLong(name, value);
			firePropertyChangeEvent(name, oldValue, newValue);
		} finally {
			notify = true;
		}
	}

	/**
	 * Returns the default value for the long-valued property
	 * with the given name.
	 * Returns the default-default value (<code>0L</code>) if there
	 * is no default property with the given name, or if the default
	 * value cannot be treated as a long.
	 *
	 * @param name the name of the property
	 * @return the default value of the named property
	 */
	@Override
	public long getDefaultLong(String name) {
		return getDefaultPreferences().getLong(name, LONG_DEFAULT_DEFAULT);
	}

	/**
	 * Sets the default value for the long-valued property with the
	 * given name.
	 * <p>
	 * Note that the current value of the property is affected if
	 * the property's current value was its old default value, in which
	 * case it changes to the new default value. If the property's current
	 * is different from its old default value, its current value is
	 * unaffected. No property change events are reported by changing default
	 * values.
	 * </p>
	 *
	 * @param name the name of the property
	 * @param value the new default value for the property
	 */
	@Override
	public void setDefault(String name, long value) {
		getDefaultPreferences().putLong(name, value);
	}

	/**
	 * Returns the current value of the string-valued property with the
	 * given name.
	 * Returns the default-default value (the empty string <code>""</code>)
	 * if there is no property with the given name.
	 *
	 * @param name the name of the property
	 * @return the string-valued property
	 */
	@Override
	public String getString(String name) {
		return getPluginPreferences(true).get(name, getDefaultPreferences().get(name, STRING_DEFAULT_DEFAULT));
	}

	/**
	 * Sets the current value of the string-valued property with the
	 * given name.
	 * <p>
	 * A property change event is reported if the current value of the
	 * property actually changes from its previous value. In the event
	 * object, the property name is the name of the property, and the
	 * old and new values are wrapped as objects.
	 * </p>
	 * <p>
	 * If the given value is the same as the corresponding default value
	 * for the given property, the explicit setting is deleted.
	 * Note that the recommended way of re-initializing a property to its
	 * default value is to call <code>setToDefault</code>.
	 * </p>
	 *
	 * @param name the name of the property
	 * @param value the new current value of the property
	 */
	@Override
	public void setValue(String name, String value) {
		if (value == null)
			throw new IllegalArgumentException();
		String oldValue = getString(name);
		if (value.equals(oldValue))
			return;
		try {
			notify = false;
			if (getDefaultString(name).equals(value))
				getPluginPreferences(true).remove(name);
			else
				getPluginPreferences(true).put(name, value);
			firePropertyChangeEvent(name, oldValue, value);
		} finally {
			notify = true;
		}
	}

	/**
	 * Returns the default value for the string-valued property
	 * with the given name.
	 * Returns the default-default value (the empty string <code>""</code>)
	 * is no default property with the given name, or if the default
	 * value cannot be treated as a string.
	 *
	 * @param name the name of the property
	 * @return the default value of the named property
	 */
	@Override
	public String getDefaultString(String name) {
		return getDefaultPreferences().get(name, STRING_DEFAULT_DEFAULT);
	}

	/**
	 * Sets the default value for the string-valued property with the
	 * given name.
	 * <p>
	 * Note that the current value of the property is affected if
	 * the property's current value was its old default value, in which
	 * case it changes to the new default value. If the property's current
	 * is different from its old default value, its current value is
	 * unaffected. No property change events are reported by changing default
	 * values.
	 * </p>
	 *
	 * @param name the name of the property
	 * @param value the new default value for the property
	 */
	@Override
	public void setDefault(String name, String value) {
		if (value == null)
			throw new IllegalArgumentException();
		getDefaultPreferences().put(name, value);
	}

	/**
	 * Returns whether the property with the given name has the default value in
	 * virtue of having no explicitly set value.
	 *
	 * @param name the name of the property
	 * @return <code>true</code> if the property has no explicitly set value,
	 * and <code>false</code> otherwise (including the case where the property
	 * is unknown to this object)
	 */
	@Override
	public boolean isDefault(String name) {
		if (name == null)
			return false;
		return getPluginPreferences(true).get(name, null) == null;
	}

	/**
	 * Sets the current value of the property with the given name back
	 * to its default value. Has no effect if the property does not have
	 * its own current value.
	 * <p>
	 * Note that the recommended way of re-initializing a property to the
	 * appropriate default value is to call <code>setToDefault</code>.
	 * This is implemented by removing the named value from the object,
	 * thereby exposing the default value.
	 * </p>
	 * <p>
	 * A property change event is always reported. In the event
	 * object, the property name is the name of the property, and the
	 * old and new values are either strings, or <code>null</code>
	 * indicating the default-default value.
	 * </p>
	 *
	 * @param name the name of the property
	 */
	@Override
	public void setToDefault(String name) {
		IEclipsePreferences preferences = getPluginPreferences(true);
		Object oldValue = preferences.get(name, null);
		if (oldValue != null)
			preferences.remove(name);
	}

	/**
	 * Returns a list of all properties known to this preference object which
	 * have current values other than their default value.
	 *
	 * @return an array of property names
	 */
	@Override
	public String[] propertyNames() {
		return getPluginPreferences(true).keys();
	}

	/**
	 * Returns a list of all properties known to this preference object which
	 * have default values other than their default-default value.
	 *
	 * @return an array of property names
	 */
	@Override
	public String[] defaultPropertyNames() {
		try {
			return getDefaultPreferences().keys();
		} catch (BackingStoreException e) {
			logError(e.getMessage(), e);
			return new String[0];
		}
	}

	/**
	 * Returns whether the current values in this preference object
	 * require saving.
	 *
	 * @return <code>true</code> if at least one of the properties
	 *  known to this preference object has a current value different from its
	 *  default value, and <code>false</code> otherwise
	 */
	@Override
	public boolean needsSaving() {
		return getPluginPreferences(true).isDirty();
	}

	/**
	 * Flush the values of these plug-in preferences to disk.
	 *
	 * @throws BackingStoreException
	 */
	public void flush() throws BackingStoreException {
		IEclipsePreferences node = getPluginPreferences(false);
		if (node != null)
			node.flush();
	}

	/*
	 * Something bad happened so log it.
	 */
	private void logError(String message, Exception e) {
		IStatus status = new Status(IStatus.ERROR, PrefsMessages.OWNER_NAME, IStatus.ERROR, message, e);
		RuntimeLog.log(status);
	}

	/*
	 * @see org.eclipse.core.runtime.Preferences#load(java.io.InputStream)
	 */
	@Override
	public void load(InputStream in) throws IOException {
		Properties result = new Properties();
		result.load(in);
		convertFromProperties(result);
		// We loaded the prefs from a non-default location so now
		// store them to disk. This also clears the dirty flag
		// and therefore sets the #needsSaving() state correctly.
		try {
			flush();
		} catch (BackingStoreException e) {
			throw new IOException(e.getMessage());
		}
	}

	/*
	 * @see org.eclipse.core.runtime.Preferences#store(java.io.OutputStream, java.lang.String)
	 */
	@Override
	public void store(OutputStream out, String header) throws IOException {
		Properties result = convertToProperties();
		result.store(out, header);
		// We stored the prefs to a non-default location but the spec
		// says that the dirty state is cleared so we want to store
		// them to disk at the default location as well.
		try {
			flush();
		} catch (BackingStoreException e) {
			throw new IOException(e.getMessage());
		}
	}

	private void convertFromProperties(Properties props) {
		IEclipsePreferences preferences = getPluginPreferences(true);
		for (Object object : props.keySet()) {
			String key = (String) object;
			String value = props.getProperty(key);
			if (value != null)
				preferences.put(key, value);
		}
	}

	@Override
	public String toString() {
		return "PreferenceForwarder(" + pluginID + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/*
	 * Convert the preferences in this node to a properties file
	 * suitable for persistence.
	 */
	private Properties convertToProperties() {
		Properties result = new Properties();
		String[] keys = propertyNames();
		for (String key : keys) {
			String value = getString(key);
			if (!Preferences.STRING_DEFAULT_DEFAULT.equals(value))
				result.put(key, value);
		}
		return result;
	}
}
