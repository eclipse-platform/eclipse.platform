/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 476403, 478769, 490586
 *     Christoph Läubrich - remove reference to InternalPlatform.getDefault().log
 *******************************************************************************/
package org.eclipse.core.runtime;

import java.io.*;
import java.net.URL;
import java.util.Map;
import org.eclipse.core.internal.runtime.*;
import org.eclipse.core.runtime.preferences.*;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The abstract superclass of all plug-in runtime class implementations. A
 * plug-in subclasses this class and overrides the appropriate life cycle
 * methods in order to react to the life cycle requests automatically issued by
 * the platform. For compatibility reasons, the methods called for those life
 * cycle events vary, please see the "Constructors and life cycle methods"
 * section below.
 *
 * <p>
 * Conceptually, the plug-in runtime class represents the entire plug-in rather
 * than an implementation of any one particular extension the plug-in declares.
 * A plug-in is not required to explicitly specify a plug-in runtime class; if
 * none is specified, the plug-in will be given a default plug-in runtime object
 * that ignores all life cycle requests (it still provides access to the
 * corresponding plug-in descriptor).
 * </p>
 * <p>
 * In the case of more complex plug-ins, it may be desirable to define a
 * concrete subclass of <code>Plugin</code>. However, just subclassing
 * <code>Plugin</code> is not sufficient. The name of the class must be
 * explicitly configured in the plug-in's manifest (<code>plugin.xml</code>)
 * file with the class attribute of the <code>&lt;plugin&gt;</code> element
 * markup.
 * </p>
 * <p>
 * Instances of plug-in runtime classes are automatically created by the
 * platform in the course of plug-in activation. For compatibility reasons, the
 * constructor used to create plug-in instances varies, please see the
 * "Constructors and life cycle methods" section below.
 * </p>
 * <p>
 * The concept of bundles underlies plug-ins. However it is safe to regard
 * plug-ins and bundles as synonyms.
 * </p>
 * <p>
 * <b>Clients must never explicitly instantiate a plug-in runtime class</b>.
 * </p>
 * <p>
 * A typical implementation pattern for plug-in runtime classes is to provide a
 * static convenience method to gain access to a plug-in's runtime object. This
 * way, code in other parts of the plug-in implementation without direct access
 * to the plug-in runtime object can easily obtain a reference to it, and thence
 * to any plug-in-wide resources recorded on it. An example for Eclipse 3.0
 * follows:
 * </p>
 *
 * <pre>
 * package myplugin;
 *
 * public class MyPluginClass extends Plugin {
 * 	private static MyPluginClass instance;
 *
 * 	public static MyPluginClass getInstance() {
 * 		return instance;
 * 	}
 *
 * 	public void MyPluginClass() {
 * 		super();
 * 		instance = this;
 * 		// ... other initialization
 * 	}
 * 	// ... other methods
 * }
 * </pre>
 * <p>
 * In the above example, a call to <code>MyPluginClass.getInstance()</code> will
 * always return an initialized instance of <code>MyPluginClass</code>.
 * </p>
 * <p>
 * <b>Constructors and life cycle methods</b>
 * </p>
 * <p>
 * If the plugin.xml of a plug-in indicates &lt;?eclipse version="3.0"?&gt; and
 * its prerequisite list includes <code>org.eclipse.core.runtime</code>, the
 * default constructor of the plug-in class is used and
 * {@link #start(BundleContext)} and {@link #stop(BundleContext)} are called as
 * life cycle methods.
 * </p>
 * <p>
 * Since Eclipse 3.0 APIs of the Plugin class can be called only when the Plugin
 * is in an active state, i.e., after it was started up and before it is
 * shutdown. In particular, it means that Plugin APIs should not be called from
 * overrides of {@link #Plugin()}.
 * </p>
 */
public abstract class Plugin implements BundleActivator {

	/**
	 * String constant used for the default scope name for legacy
	 * Eclipse plug-in preferences. The value of <code>PLUGIN_PREFERENCE_SCOPE</code> should
	 * match the InstanceScope's variable SCOPE from org.eclipse.core.runtime.preferences.
	 * The value is copied in this file to prevent unnecessary activation of
	 * the Preferences plugin on startup.
	 *
	 * @since 3.0
	 */
	public static final String PLUGIN_PREFERENCE_SCOPE = "instance"; //$NON-NLS-1$

	/**
	 * The bundle associated this plug-in
	 */
	private Bundle bundle;

	private volatile IPath stateLocation;

	/**
	 * The debug flag for this plug-in.  The flag is false by default.
	 * This flag is only used when the DebugOptions service is not available.
	 */
	private boolean debug = false;

	/**
	 * DebugOptions service tracker
	 */
	private ServiceTracker<DebugOptions,DebugOptions> debugTracker = null;

	/**
	 * The base name (value <code>"preferences"</code>) for the file which is used for
	 * overriding default preference values.
	 *
	 * @since 2.0
	 * @see #PREFERENCES_DEFAULT_OVERRIDE_FILE_NAME
	 */
	public static final String PREFERENCES_DEFAULT_OVERRIDE_BASE_NAME = "preferences"; //$NON-NLS-1$

	/**
	 * The name of the file (value <code>"preferences.ini"</code>) in a
	 * plug-in's (read-only) directory that, when present, contains values that
	 * override the normal default values for this plug-in's preferences.
	 * <p>
	 * The format of the file is as per <code>java.io.Properties</code> where
	 * the keys are property names and values are strings.
	 * </p>
	 *
	 * @since 2.0
	 */
	public static final String PREFERENCES_DEFAULT_OVERRIDE_FILE_NAME = PREFERENCES_DEFAULT_OVERRIDE_BASE_NAME + ".ini"; //$NON-NLS-1$

	/**
	 * The preference object for this plug-in; initially <code>null</code>
	 * meaning not yet created and initialized.
	 *
	 * @since 2.0
	 * @deprecated
	 */
	@Deprecated
	private Preferences preferences = null;

	/**
	 * Creates a new plug-in runtime object.  This method is called by the platform
	 * if this class is used as a <code>BundleActivator</code>.  This method is not
	 * needed/used if this plug-in requires the org.eclipse.core.runtime.compatibility plug-in.
	 * Subclasses of <code>Plugin</code>
	 * must call this method first in their constructors.
	 *
	 * The resultant instance is not managed by the runtime and
	 * so should be remembered by the client (typically using a Singleton pattern).
	 * <b>Clients must never explicitly call this method.</b>
	 * <p>
	 * Note: The class loader typically has monitors acquired during invocation of this method.  It is
	 * strongly recommended that this method avoid synchronized blocks or other thread locking mechanisms,
	 * as this would lead to deadlock vulnerability.
	 * </p>
	 *
	 * @since 3.0
	 */
	public Plugin() {
		super();
	}


	/**
	 * Returns a URL for the given path.  Returns <code>null</code> if the URL
	 * could not be computed or created.
	 *
	 * @param path path relative to plug-in installation location
	 * @return a URL for the given path or <code>null</code>
	 * @deprecated use {@link FileLocator#find(Bundle, IPath, Map)}
	 */
	@Deprecated
	public final URL find(IPath path) {
		return FileLocator.find(getBundle(), path, null);
	}

	/**
	 * Returns a URL for the given path.  Returns <code>null</code> if the URL
	 * could not be computed or created.
	 *
	 * @param path file path relative to plug-in installation location
	 * @param override map of override substitution arguments to be used for
	 * any $arg$ path elements. The map keys correspond to the substitution
	 * arguments (eg. "$nl$" or "$os$"). The resulting
	 * values must be of type java.lang.String. If the map is <code>null</code>,
	 * or does not contain the required substitution argument, the default
	 * is used.
	 * @return a URL for the given path or <code>null</code>
	 * @deprecated use {@link FileLocator#find(Bundle, IPath, Map)}
	 */
	@Deprecated
	public final URL find(IPath path, Map<String,String> override) {
		return FileLocator.find(getBundle(), path, override);
	}


	/**
	 * Returns the log for this plug-in.  If no such log exists, one is created.
	 *
	 * @return the log for this plug-in
	 * XXX change this into a LogMgr service that would keep track of the map. See if it can be a service factory.
	 */
	public final ILog getLog() {
		return InternalPlatform.getDefault().getLog(getBundle());
	}

	/**
	 * Returns the location in the local file system of the
	 * plug-in state area for this plug-in.
	 * If the plug-in state area did not exist prior to this call,
	 * it is created.
	 * <p>
	 * The plug-in state area is a file directory within the
	 * platform's metadata area where a plug-in is free to create files.
	 * The content and structure of this area is defined by the plug-in,
	 * and the particular plug-in is solely responsible for any files
	 * it puts there. It is recommended for plug-in preference settings and
	 * other configuration parameters.
	 * </p>
	 * @throws IllegalStateException when the system is running with no data area (-data @none),
	 * or when a data area has not been set yet.
	 * @return a local file system path
	 *  XXX Investigate the usage of a service factory (see also platform.getStateLocation)
	 */
	public final IPath getStateLocation() throws IllegalStateException {
		if (stateLocation == null) {
			// cache the value to avoid repeated java.io.File.mkdirs()
			// does not matter if the value is computed twice in parallel
			stateLocation = InternalPlatform.getDefault().getStateLocation(getBundle(), true);
		}
		return stateLocation;
	}

	/**
	 * Returns the preference store for this plug-in.
	 * <p>
	 * Note that if an error occurs reading the preference store from disk, an empty
	 * preference store is quietly created, initialized with defaults, and returned.
	 * </p>
	 * <p>
	 * Calling this method may cause the preference store to be created and
	 * initialized. Subclasses which reimplement the
	 * <code>initializeDefaultPluginPreferences</code> method have this opportunity
	 * to initialize preference default values, just prior to processing override
	 * default values imposed externally to this plug-in (specified for the product,
	 * or at platform start up).
	 * </p>
	 * <p>
	 * After settings in the preference store are changed (for example, with
	 * <code>Preferences.setValue</code> or <code>setToDefault</code>),
	 * <code>savePluginPreferences</code> should be called to store the changed
	 * values back to disk. Otherwise the changes will be lost on plug-in shutdown.
	 * </p>
	 *
	 * @return the preference store
	 * @see #savePluginPreferences()
	 * @see Preferences#setValue(String, String)
	 * @see Preferences#setToDefault(String)
	 * @since 2.0
	 * @deprecated Replaced by {@link IEclipsePreferences}. Preferences are now
	 *             stored according to scopes in the {@link IPreferencesService}.
	 *             The return value of this method corresponds to a combination of
	 *             the {@link InstanceScope} and the {@link DefaultScope}. To set
	 *             preferences for your plug-in, use
	 *             <code>InstanceScope.INSTANCE.getNode(&lt;yourPluginId&gt;)</code>.
	 *             To set default preferences for your plug-in, use
	 *             <code>DefaultScope.INSTANCE.getNode(&lt;yourPluginId&gt;)</code>.
	 *             To lookup an integer preference value for your plug-in, use
	 *             <code>Platform.getPreferencesService().getInt(&lt;yourPluginId&gt;, &lt;preferenceKey&gt;, &lt;defaultValue&gt;, null)</code>.
	 *             Similar methods exist on {@link IPreferencesService} for
	 *             obtaining other kinds of preference values (strings, booleans,
	 *             etc).
	 */
	@Deprecated
	public final Preferences getPluginPreferences() {
		final Bundle bundleCopy = getBundle();
		if (preferences != null) {
			if (InternalPlatform.DEBUG_PLUGIN_PREFERENCES)
				InternalPlatform.message("Plugin preferences already loaded for: " + bundleCopy.getSymbolicName()); //$NON-NLS-1$
			return preferences;
		}

		if (InternalPlatform.DEBUG_PLUGIN_PREFERENCES)
			InternalPlatform.message("Loading preferences for plugin: " + bundleCopy.getSymbolicName()); //$NON-NLS-1$

		// Performance: isolate PreferenceForwarder into an inner class so that it mere presence
		// won't force the PreferenceForwarder class to be loaded (which triggers Preferences plugin
		// activation).
		final Preferences[] preferencesCopy = new Preferences[1];
		Runnable innerCall = () -> preferencesCopy[0] = new org.eclipse.core.internal.preferences.legacy.PreferenceForwarder(
				this, bundleCopy.getSymbolicName());

		innerCall.run();
		preferences = preferencesCopy[0];
		return preferences;
	}

	/**
	 * Saves preferences settings for this plug-in. Does nothing if the preference
	 * store does not need saving.
	 * <p>
	 * Plug-in preferences are <b>not</b> saved automatically on plug-in shutdown.
	 * </p>
	 *
	 * @see Preferences#store(OutputStream, String)
	 * @see Preferences#needsSaving()
	 * @since 2.0
	 * @deprecated Replaced by InstanceScope.getNode(&lt;bundleId&gt;).flush()
	 */
	@Deprecated
	public final void savePluginPreferences() {

		Location instance = InternalPlatform.getDefault().getInstanceLocation();
		if (instance == null || !instance.isSet())
			// If the instance area is not set there is no point in getting or setting the preferences.
			// There is nothing to save in this case.
			return;
		// populate the "preferences" instance variable. We still might
		// need to save them because someone else might have
		// made changes via the OSGi APIs.
		getPluginPreferences();

		// Performance: isolate PreferenceForwarder and BackingStoreException into
		// an inner class to avoid class loading (and then activation of the Preferences plugin)
		// as the Plugin class is loaded.
		final Preferences preferencesCopy = preferences;
		Runnable innerCall = () -> {
			try {
				((org.eclipse.core.internal.preferences.legacy.PreferenceForwarder) preferencesCopy).flush();
			} catch (org.osgi.service.prefs.BackingStoreException e) {
				IStatus status = new Status(IStatus.ERROR, Platform.PI_RUNTIME, IStatus.ERROR,
						Messages.preferences_saveProblems, e);
				RuntimeLog.log(status);
			}
		};
		innerCall.run();
	}

	/**
	 * Initializes the default preferences settings for this plug-in.
	 * <p>
	 * This method is called sometime after the preference store for this
	 * plug-in is created. Default values are never stored in preference
	 * stores; they must be filled in each time. This method provides the
	 * opportunity to initialize the default values.
	 * </p>
	 * <p>
	 * The default implementation of this method does nothing. A subclass that needs
	 * to set default values for its preferences must reimplement this method.
	 * Default values set at a later point will override any default override
	 * settings supplied from outside the plug-in (product configuration or
	 * platform start up).
	 * </p>
	 * @since 2.0
	 * @deprecated
	 * This method has been refactored in the new preference mechanism
	 * to handle the case where the runtime compatibility layer does not exist. The
	 * contents of this method should be moved to the method named
	 * <code>initializeDefaultPreferences</code> in a separate subclass of
	 * {@link org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer}.
	 * This class should be contributed via the
	 * <code>org.eclipse.core.runtime.preferences</code> extension point.
	 * <pre>
	 * 	&lt;extension point=&quot;org.eclipse.core.runtime.preferences&quot;&gt;
	 *			&lt;initializer class=&quot;com.example.MyPreferenceInitializer&quot;/&gt;
	 *		&lt;/extension&gt;
	 *		...
	 *		package com.example;
	 *		public class MyPreferenceInitializer extends AbstractPreferenceInitializer {
	 *			public MyPreferenceInitializer() {
	 *				super();
	 *			}
	 *			public void initializeDefaultPreferences() {
	 *				Preferences node = new DefaultScope().getNode("my.plugin.id");
	 *				node.put(key, value);
	 *			}
	 *		}
	 * </pre>
	 */
	@Deprecated
	protected void initializeDefaultPluginPreferences() {
		// default implementation of this method - spec'd to do nothing
	}

	/**
	 * Internal method. This method is a hook for
	 * initialization of default preference values.
	 * It should not be called by clients.
	 *
	 * @since 3.0
	 * @deprecated
	 */
	@Deprecated
	public final void internalInitializeDefaultPluginPreferences() {
		initializeDefaultPluginPreferences();
	}

	/**
	 * Returns whether this plug-in is in debug mode.
	 * By default plug-ins are not in debug mode.  A plug-in can put itself
	 * into debug mode or the user can set an execution option to do so.
	 * <p>
	 * Note that the plug-in's debug flag is initialized when the
	 * plug-in is started. The result of calling this method before the plug-in
	 * has started is unspecified.
	 * </p>
	 *
	 * @return whether this plug-in is in debug mode
	 * XXX deprecate use the service and cache as needed
	 */
	public boolean isDebugging() {
		Bundle debugBundle = getBundle();
		if (debugBundle == null)
			return debug;
		String key = debugBundle.getSymbolicName() + "/debug"; //$NON-NLS-1$
		// first check if platform debugging is enabled
		final DebugOptions debugOptions = getDebugOptions();
		if (debugOptions == null)
			return debug;
		// if platform debugging is enabled, check to see if this plugin is enabled for debugging
		return debugOptions.isDebugEnabled() ? InternalPlatform.getDefault().getBooleanOption(key, false) : false;
	}

	/**
	 * Returns an input stream for the specified file. The file path
	 * must be specified relative this the plug-in's installation location.
	 *
	 * @param file path relative to plug-in installation location
	 * @return an input stream
	 * @exception IOException if the given path cannot be found in this plug-in
	 *
	 * @see #openStream(IPath,boolean)
	 * @deprecated use {@link FileLocator#openStream(Bundle, IPath, boolean)}
	 */
	@Deprecated
	public final InputStream openStream(IPath file) throws IOException {
		return FileLocator.openStream(getBundle(), file, false);
	}

	/**
	 * Returns an input stream for the specified file. The file path
	 * must be specified relative to this plug-in's installation location.
	 * Optionally, the path specified may contain $arg$ path elements that can
	 * be used as substitution arguments.  If this option is used then the $arg$
	 * path elements are processed in the same way as {@link #find(IPath, Map)}.
	 * <p>
	 * The caller must close the returned stream when done.
	 * </p>
	 *
	 * @param file path relative to plug-in installation location
	 * @param substituteArgs <code>true</code> to process substitution arguments,
	 * and <code>false</code> for the file exactly as specified without processing any
	 * substitution arguments.
	 * @return an input stream
	 * @exception IOException if the given path cannot be found in this plug-in
	 * @deprecated use {@link FileLocator#openStream(Bundle, IPath, boolean)}
	 */
	@Deprecated
	public final InputStream openStream(IPath file, boolean substituteArgs) throws IOException {
		return FileLocator.openStream(getBundle(), file, substituteArgs);
	}

	/**
	 * Sets whether this plug-in is in debug mode.
	 * By default plug-ins are not in debug mode.  A plug-in can put itself
	 * into debug mode or the user can set a debug option to do so.
	 * <p>
	 * Note that the plug-in's debug flag is initialized when the
	 * plug-in is started. The result of calling this method before the plug-in
	 * has started is unspecified.
	 * </p>
	 *
	 * @param value whether or not this plug-in is in debug mode
	 * XXX deprecate use the service and cache as needed
	 */
	public void setDebugging(boolean value) {
		Bundle debugBundle = getBundle();
		if (debugBundle == null) {
			this.debug = value;
			return;
		}
		String key = debugBundle.getSymbolicName() + "/debug"; //$NON-NLS-1$
		final DebugOptions options = getDebugOptions();
		if (options == null)
			this.debug = value;
		else {
			if (!options.isDebugEnabled())
				options.setDebugEnabled(true);
			options.setOption(key, value ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
		}
	}

	/**
	 * Returns the DebugOptions instance
	 *
	 * @since 3.5
	 * @return Either the DebugOptions instance or <code>null</code> if this plug-in does not have a bundle
	 */
	private DebugOptions getDebugOptions() {
		Bundle debugBundle = getBundle();
		if (debugBundle == null)
			return null;
		if (debugTracker == null) {
			BundleContext context = debugBundle.getBundleContext();
			if (context == null)
				return null;
			debugTracker = new ServiceTracker<>(context, DebugOptions.class.getName(), null);
			debugTracker.open();
		}
		return this.debugTracker.getService();
	}

	/**
	 * As the org.eclipse.core.runtime.compatibility plug-in has been removed in
	 * Eclipse 4.6 this method is not supported anymore.
	 *
	 * In Eclipse 3.0 this method has been replaced by
	 * {@link Plugin#stop(BundleContext context)}. Implementations of
	 * <code>shutdown()</code> should be changed to override
	 * <code>stop(BundleContext context)</code> and call
	 * <code>super.stop(context)</code> instead of <code>super.shutdown()</code>
	 * .
	 *
	 * The <code>shutdown()</code> method was called only for plug-ins which
	 * explicitly required the org.eclipse.core.runtime.compatibility plug-in.
	 * It is not called anymore.
	 *
	 * @throws CoreException Never thrown as method as no longer called.
	 *
	 * @deprecated
	 */
	@Deprecated
	public void shutdown() throws CoreException {
		// intentionally left empty
	}

	/**
	 * As the org.eclipse.core.runtime.compatibility plug-in has been removed in
	 * Eclipse 4.6 this method is not supported anymore.
	 *
	 * In Eclipse 3.0 this method has been replaced by
	 * {@link Plugin#start(BundleContext context)}. Implementations of
	 * <code>startup()</code> should be changed to extend
	 * <code>start(BundleContext context)</code> and call
	 * <code>super.start(context)</code> instead of
	 * <code>super.startup()</code>.
	 *
	 * The <code>startup()</code> method was called only for plug-ins which
	 * explicitly required the org.eclipse.core.runtime.compatibility plug-in.
	 * It is not called anymore.
	 *
	 * @throws CoreException Never thrown as method as no longer called.
	 *
	 * @deprecated
	 */
	@Deprecated
	public void startup() throws CoreException {
		// intentionally left empty
	}

	/**
	 * Returns a string representation of the plug-in, suitable
	 * for debugging purposes only.
	 */
	@Override
	public String toString() {
		Bundle myBundle = getBundle();
		if (myBundle == null)
			return ""; //$NON-NLS-1$
		String name = myBundle.getSymbolicName();
		return name == null ? String.valueOf(myBundle.getBundleId()) : name;
	}

	/**
	 * Starts up this plug-in.
	 * <p>
	 * This method should be overridden in subclasses that need to do something
	 * when this plug-in is started.  Implementors should call the inherited method
	 * at the first possible point to ensure that any system requirements can be met.
	 * </p>
	 * <p>
	 * If this method throws an exception, it is taken as an indication that
	 * plug-in initialization has failed; as a result, the plug-in will not
	 * be activated; moreover, the plug-in will be marked as disabled and
	 * ineligible for activation for the duration.
	 * </p>
	 * <p>
	 * Note 1: This method is automatically invoked by the platform
	 * the first time any code in the plug-in is executed.
	 * </p>
	 * <p>
	 * Note 2: This method is intended to perform simple initialization
	 * of the plug-in environment. The platform may terminate initializers
	 * that do not complete in a timely fashion.
	 * </p>
	 * <p>
	 * Note 3: The class loader typically has monitors acquired during invocation of this method.  It is
	 * strongly recommended that this method avoid synchronized blocks or other thread locking mechanisms,
	 * as this would lead to deadlock vulnerability.
	 * </p>
	 * <p>
	 * Note 4: The supplied bundle context represents the plug-in to the OSGi framework.
	 * For security reasons, it is strongly recommended that this object should not be divulged.
	 * </p>
	 * <p>
	 * Note 5: This method and the {@link #stop(BundleContext)} may be called from separate threads,
	 * but the OSGi framework ensures that both methods will not be called simultaneously.
	 * </p>
	 * <b>Clients must never explicitly call this method.</b>
	 *
	 * @param context the bundle context for this plug-in
	 * @exception Exception if this plug-in did not start up properly
	 * @since 3.0
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		bundle = context.getBundle();
	}

	/**
	 * Stops this plug-in.
	 * <p>
	 * This method should be re-implemented in subclasses that need to do something
	 * when the plug-in is shut down.  Implementors should call the inherited method
	 * as late as possible to ensure that any system requirements can be met.
	 * </p>
	 * <p>
	 * Plug-in shutdown code should be robust. In particular, this method
	 * should always make an effort to shut down the plug-in. Furthermore,
	 * the code should not assume that the plug-in was started successfully.
	 * </p>
	 * <p>
	 * Note 1: If a plug-in has been automatically started, this method will be automatically
	 * invoked by the platform when the platform is shut down.
	 * </p>
	 * <p>
	 * Note 2: This method is intended to perform simple termination
	 * of the plug-in environment. The platform may terminate invocations
	 * that do not complete in a timely fashion.
	 * </p>
	 * <p>
	 * Note 3: The supplied bundle context represents the plug-in to the OSGi framework.
	 * For security reasons, it is strongly recommended that this object should not be divulged.
	 * </p>
	 * <p>
	 * Note 4: This method and the {@link #start(BundleContext)} may be called from separate threads,
	 * but the OSGi framework ensures that both methods will not be called simultaneously.
	 * </p>
	 * <b>Clients must never explicitly call this method.</b>
	 *
	 * @param context the bundle context for this plug-in
	 * @exception Exception if this method fails to shut down this plug-in
	 * @since 3.0
	 */
	@Override
	public void stop(BundleContext context) throws Exception {

		if (this.debugTracker != null) {
			this.debugTracker.close();
			this.debugTracker = null;
		}
		// sub-classes to override
	}

	/**
	 * Returns the bundle associated with this plug-in.
	 *
	 * @return the associated bundle
	 * @since 3.0
	 */
	public final Bundle getBundle() {
		if (bundle != null)
			return bundle;
		ClassLoader cl = getClass().getClassLoader();
		if (cl instanceof BundleReference)
			return ((BundleReference) cl).getBundle();
		return null;
	}
}
