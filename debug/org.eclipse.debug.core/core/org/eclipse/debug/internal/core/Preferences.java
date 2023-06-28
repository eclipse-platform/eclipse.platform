/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
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
package org.eclipse.debug.internal.core;

import org.osgi.service.prefs.BackingStoreException;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.debug.core.DebugPlugin;

/**
 * Convenience class to facilitate using the new {@link IEclipsePreferences} story
 *
 * @since 3.6
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class Preferences {

	static final IScopeContext[] contexts= new IScopeContext[] { DefaultScope.INSTANCE, InstanceScope.INSTANCE };

	static final int DEFAULT_CONTEXT = 0;
	static final int INSTANCE_CONTEXT = 1;

	/**
	 * Constructor
	 */
	private Preferences() {
		// no direct instantiation
	}

	/**
	 * Sets a string preference in the {@link InstanceScope} or the given {@link IScopeContext} if it
	 * is not <code>null</code>. Preferences set in a given context are flushed as they are set.
	 * @param qualifier the preference qualifier
	 * @param key the key
	 * @param value the value
	 * @param context the context to set the value in
	 */
	public static synchronized void setString(String qualifier, String key, String value, IScopeContext context) {
		if(context != null) {
			try {
				IEclipsePreferences node = context.getNode(qualifier);
				node.put(key, value);
				node.flush();
			}
			catch(BackingStoreException bse) {
				DebugPlugin.log(bse);
			}
		}
		else {
			contexts[INSTANCE_CONTEXT].getNode(qualifier).put(key, value);
		}
	}

	/**
	 * Sets a boolean preference in the {@link InstanceScope} or the given {@link IScopeContext} if it
	 * is not <code>null</code>. Preferences set in a given context are flushed as they are set.
	 * @param qualifier the preference qualifier
	 * @param key the key
	 * @param value the value
	 * @param context the context to set the value in
	 */
	public static synchronized void setBoolean(String qualifier, String key, boolean value, IScopeContext context) {
		if(context != null) {
			try {
				IEclipsePreferences node = context.getNode(qualifier);
				node.putBoolean(key, value);
				node.flush();
			}
			catch(BackingStoreException bse) {
				DebugPlugin.log(bse);
			}
		}
		else {
			contexts[INSTANCE_CONTEXT].getNode(qualifier).putBoolean(key, value);
		}
	}

	/**
	 * Sets a integer preference in the {@link InstanceScope} or the given {@link IScopeContext} if it
	 * is not <code>null</code>. Preferences set in a given context are flushed as they are set.
	 * @param qualifier the preference qualifier
	 * @param key the key
	 * @param value the value
	 * @param context the context to set the value in
	 */
	public static synchronized void setInt(String qualifier, String key, int value, IScopeContext context) {
		if(context != null) {
			try {
				IEclipsePreferences node = context.getNode(qualifier);
				node.putInt(key, value);
				node.flush();
			}
			catch(BackingStoreException bse) {
				DebugPlugin.log(bse);
			}
		}
		else {
			contexts[INSTANCE_CONTEXT].getNode(qualifier).putInt(key, value);
		}
	}

	/**
	 * Sets a long preference in the {@link InstanceScope} or the given {@link IScopeContext} if it
	 * is not <code>null</code>. Preferences set in a given context are flushed as they are set.
	 * @param qualifier the preference qualifier
	 * @param key the key
	 * @param value the value
	 * @param context the context to set the value in
	 */
	public static synchronized void setLong(String qualifier, String key, long value, IScopeContext context) {
		if(context != null) {
			try {
				IEclipsePreferences node = context.getNode(qualifier);
				node.putLong(key, value);
				node.flush();
			}
			catch(BackingStoreException bse) {
				DebugPlugin.log(bse);
			}
		}
		else {
			contexts[INSTANCE_CONTEXT].getNode(qualifier).putLong(key, value);
		}
	}

	/**
	 * Sets a byte array preference in the {@link InstanceScope} or the given {@link IScopeContext} if it
	 * is not <code>null</code>. Preferences set in a given context are flushed as they are set.
	 * @param qualifier the preference qualifier
	 * @param key the key
	 * @param value the value
	 * @param context the context to set the value in
	 */
	public static synchronized void setByteArray(String qualifier, String key, byte[] value, IScopeContext context) {
		if(context != null) {
			try {
				IEclipsePreferences node = context.getNode(qualifier);
				node.putByteArray(key, value);
				node.flush();
			}
			catch(BackingStoreException bse) {
				DebugPlugin.log(bse);
			}
		}
		else {
			contexts[INSTANCE_CONTEXT].getNode(qualifier).putByteArray(key, value);
		}
	}

	/**
	 * Sets a double preference in the {@link InstanceScope} or the given {@link IScopeContext} if it
	 * is not <code>null</code>. Preferences set in a given context are flushed as they are set.
	 * @param qualifier the preference qualifier
	 * @param key the key
	 * @param value the value
	 * @param context the context to set the value in
	 */
	public static synchronized void setDouble(String qualifier, String key, double value, IScopeContext context) {
		if(context != null) {
			try {
				IEclipsePreferences node = context.getNode(qualifier);
				node.putDouble(key, value);
				node.flush();
			}
			catch(BackingStoreException bse) {
				DebugPlugin.log(bse);
			}
		}
		else {
			contexts[INSTANCE_CONTEXT].getNode(qualifier).putDouble(key, value);
		}
	}

	/**
	 * Sets a float preference in the {@link InstanceScope} or the given {@link IScopeContext} if it
	 * is not <code>null</code>. Preferences set in a given context are flushed as they are set.
	 * @param qualifier the preference qualifier
	 * @param key the key
	 * @param value the value
	 * @param context the context to setthe value in
	 */
	public static synchronized void setFloat(String qualifier, String key, float value, IScopeContext context) {
		if(context != null) {
			try {
				IEclipsePreferences node = context.getNode(qualifier);
				node.putFloat(key, value);
				node.flush();
			}
			catch(BackingStoreException bse) {
				DebugPlugin.log(bse);
			}
		}
		else {
			contexts[INSTANCE_CONTEXT].getNode(qualifier).putFloat(key, value);
		}
	}

	/**
	 * Sets a string in the {@link DefaultScope}
	 * @param qualifier the preference qualifier
	 * @param key the key
	 * @param value the new value
	 */
	public static synchronized void setDefaultString(String qualifier, String key, String value) {
		contexts[DEFAULT_CONTEXT].getNode(qualifier).put(key, value);
	}

	/**
	 * Sets a boolean in the {@link DefaultScope}
	 * @param qualifier the preference qualifier
	 * @param key the key
	 * @param value the new value
	 */
	public static synchronized void setDefaultBoolean(String qualifier, String key, boolean value) {
		contexts[DEFAULT_CONTEXT].getNode(qualifier).putBoolean(key, value);
	}

	/**
	 * Sets a byte array in the {@link DefaultScope}
	 * @param qualifier the preference qualifier
	 * @param key the key
	 * @param value the new value
	 */
	public static synchronized void setDefaultByteArray(String qualifier, String key, byte[] value) {
		contexts[DEFAULT_CONTEXT].getNode(qualifier).putByteArray(key, value);
	}

	/**
	 * Sets a double in the {@link DefaultScope}
	 * @param qualifier the preference qualifier
	 * @param key the key
	 * @param value the new value
	 */
	public static synchronized void setDefaultDouble(String qualifier, String key, double value) {
		contexts[DEFAULT_CONTEXT].getNode(qualifier).putDouble(key, value);
	}

	/**
	 * Sets a float in the {@link DefaultScope}
	 * @param qualifier the preference qualifier
	 * @param key the key
	 * @param value the new value
	 */
	public static synchronized void setDefaultFloat(String qualifier, String key, float value) {
		contexts[DEFAULT_CONTEXT].getNode(qualifier).putFloat(key, value);
	}

	/**
	 * Sets a integer in the {@link DefaultScope}
	 * @param qualifier the preference qualifier
	 * @param key the key
	 * @param value the new value
	 */
	public static synchronized void setDefaultInt(String qualifier, String key, int value) {
		contexts[DEFAULT_CONTEXT].getNode(qualifier).putInt(key, value);
	}

	/**
	 * Sets a long in the {@link DefaultScope}
	 * @param qualifier the preference qualifier
	 * @param key the key
	 * @param value the new value
	 */
	public static synchronized void setDefaultLong(String qualifier, String key, long value) {
		contexts[DEFAULT_CONTEXT].getNode(qualifier).putLong(key, value);
	}

	/**
	 * Sets the given preference to its default value. This is done by removing any set value
	 * from the {@link InstanceScope}. Has no effect if the given key is <code>null</code>.
	 * @param qualifier the preference qualifier
	 * @param key the key for the preference
	 */
	public static synchronized void setToDefault(String qualifier, String key) {
		if(key != null) {
			contexts[INSTANCE_CONTEXT].getNode(qualifier).remove(key);
		}
	}

	/**
	 * Returns the default boolean value stored in the {@link DefaultScope} for the given key
	 * or the specified default value if the key does not appear in the {@link DefaultScope}
	 * @param qualifier the preference qualifier
	 * @param key the preference key
	 * @param defaultvalue the default value
	 *
	 * @return the boolean value set in the {@link DefaultScope} for the given key, or the specified default value.
	 */
	public static synchronized boolean getDefaultBoolean(String qualifier, String key, boolean defaultvalue) {
		return contexts[DEFAULT_CONTEXT].getNode(qualifier).getBoolean(key, defaultvalue);
	}

	/**
	 * Returns the default string value stored in the {@link DefaultScope} for the given key
	 * or the specified default value if the key does not appear in the {@link DefaultScope}
	 * @param qualifier the preference qualifier
	 * @param key the preference key
	 * @param defaultvalue the default value
	 *
	 * @return the string value set in the {@link DefaultScope} for the given key, or the specified default value.
	 */
	public static synchronized String getDefaultString(String qualifier, String key, String defaultvalue) {
		return contexts[DEFAULT_CONTEXT].getNode(qualifier).get(key, defaultvalue);
	}

	/**
	 * Returns the default byte array value stored in the {@link DefaultScope} for the given key
	 * or the specified default value if the key does not appear in the {@link DefaultScope}
	 * @param qualifier the preference qualifier
	 * @param key the preference key
	 * @param defaultvalue the default value
	 *
	 * @return the byte array value set in the {@link DefaultScope} for the given key, or the specified default value.
	 */
	public static synchronized byte[] getDefaultByteArray(String qualifier, String key, byte[] defaultvalue) {
		return contexts[DEFAULT_CONTEXT].getNode(qualifier).getByteArray(key, defaultvalue);
	}

	/**
	 * Returns the default integer value stored in the {@link DefaultScope} for the given key
	 * or the specified default value if the key does not appear in the {@link DefaultScope}
	 * @param qualifier the preference qualifier
	 * @param key the preference key
	 * @param defaultvalue the default value
	 *
	 * @return the integer value set in the {@link DefaultScope} for the given key, or the specified default value.
	 */
	public static synchronized int getDefaultInt(String qualifier, String key, int defaultvalue) {
		return contexts[DEFAULT_CONTEXT].getNode(qualifier).getInt(key, defaultvalue);
	}

	/**
	 * Returns the default long value stored in the {@link DefaultScope} for the given key
	 * or the specified default value if the key does not appear in the {@link DefaultScope}
	 * @param qualifier the preference qualifier
	 * @param key the preference key
	 * @param defaultvalue the default value
	 *
	 * @return the long value set in the {@link DefaultScope} for the given key, or the specified default value.
	 */
	public static synchronized long getDefaultLong(String qualifier, String key, long defaultvalue) {
		return contexts[DEFAULT_CONTEXT].getNode(qualifier).getLong(key, defaultvalue);
	}

	/**
	 * Returns the default double value stored in the {@link DefaultScope} for the given key
	 * or the specified default value if the key does not appear in the {@link DefaultScope}
	 * @param qualifier the preference qualifier
	 * @param key the preference key
	 * @param defaultvalue the default value
	 *
	 * @return the double value set in the {@link DefaultScope} for the given key, or the specified default value.
	 */
	public static synchronized double getDefaultDouble(String qualifier, String key, double defaultvalue) {
		return contexts[DEFAULT_CONTEXT].getNode(qualifier).getDouble(key, defaultvalue);
	}

	/**
	 * Returns the default float value stored in the {@link DefaultScope} for the given key
	 * or the specified default value if the key does not appear in the {@link DefaultScope}
	 * @param qualifier the preference qualifier
	 * @param key the preference key
	 * @param defaultvalue the default value
	 *
	 * @return the float value set in the {@link DefaultScope} for the given key, or the specified default value.
	 */
	public static synchronized float getDefaultFloat(String qualifier, String key, float defaultvalue) {
		return contexts[DEFAULT_CONTEXT].getNode(qualifier).getFloat(key, defaultvalue);
	}

	/**
	 * Save the preferences for the given plug-in identifier.
	 * It should be noted that all pending preference changes will be flushed with this method.
	 * @param qualifier the preference qualifier
	 */
	public static synchronized void savePreferences(String qualifier) {
		try {
			contexts[DEFAULT_CONTEXT].getNode(qualifier).flush();
			contexts[INSTANCE_CONTEXT].getNode(qualifier).flush();
		}
		catch(BackingStoreException bse) {
			DebugPlugin.log(bse);
		}
	}

	/**
	 * Adds the given preference listener to the {@link DefaultScope} and the {@link InstanceScope}
	 * @param qualifier the preference qualifier
	 * @param listener the listener to register
	 */
	public static void addPreferenceListener(String qualifier, IEclipsePreferences.IPreferenceChangeListener listener) {
		contexts[DEFAULT_CONTEXT].getNode(qualifier).addPreferenceChangeListener(listener);
		contexts[INSTANCE_CONTEXT].getNode(qualifier).addPreferenceChangeListener(listener);
	}

	/**
	 * Removes the given preference listener from the {@link DefaultScope} and the {@link InstanceScope}
	 * @param qualifier the preference qualifier
	 * @param listener the listener to register
	 */
	public static void removePreferenceListener(String qualifier, IEclipsePreferences.IPreferenceChangeListener listener) {
		contexts[DEFAULT_CONTEXT].getNode(qualifier).removePreferenceChangeListener(listener);
		contexts[INSTANCE_CONTEXT].getNode(qualifier).removePreferenceChangeListener(listener);
	}
}
