/*******************************************************************************
 *  Copyright (c) 2000, 2025 IBM Corporation and others.
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
package org.eclipse.update.configurator;

import java.io.IOException;
import java.net.URL;

/**
 * Platform configuration interface. Represents the runtime
 * configuration used by the Eclipse platform. Any configuration
 * changes do not take effect until next startup of the Eclipse
 * platform.
 * <p>
 * Do not provide implementations of this interface or its nested interfaces. Use the factory methods
 * on IPlatformConfigurationFactory to create a IPlatformConfiguration, then use the factory methods
 * on the IPlatformConfiguration to create instances of ISiteEntry,IFeatureEntry and ISitePolicy.
 * </p>
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @since 3.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
@Deprecated(forRemoval = true, since = "2025-06 (removal in 2027-06 or later)")
public interface IPlatformConfiguration {

	/**
	 * Configuration entry representing an install site.
	 *
	 * @since 2.0
	 */
	@Deprecated
	public interface ISiteEntry {

		/**
		 * Returns the URL for this site
		 *
		 * @return site url
		 * @since 2.0
		 */
		public URL getURL();

		/**
		 * Returns the policy for this site
		 *
		 * @return site policy
		 * @since 2.0
		 */
		public ISitePolicy getSitePolicy();

		/**
		 * Sets the site policy
		 *
		 * @param policy site policy
		 * @since 2.0
		 */
		public void setSitePolicy(ISitePolicy policy);

		/**
		 * Returns a list of features visible on the site. Note, that this is simply a
		 * reflection of the site content. The features may or may not be actually configured.
		 *
		 * @return an array of feature entries, or an empty array if no features are found.
		 * A feature entry is returned as a path relative to the site URL
		 * @since 2.0
		 */
		public String[] getFeatures();

		/**
		 * Returns a list of plug-ins visible on the site. Note, that this is simply a
		 * reflection of the site content and the current policy for the site. The plug-ins
		 * may or may not end up being used by Eclipse (depends on which plug-in are
		 * actually bound by the platform).
		 *
		 * @return an array of plug-in entries, or an empty array if no plug-ins are found.
		 * A plug-in entry is returned as a path relative to the site URL		 *
		 * @since 2.0
		 */
		public String[] getPlugins();

		/**
		 * Returns a stamp reflecting the current state of the site. If called repeatedly,
		 * returns the same value as long as no changes were made to the site (changes to
		 * features or plugins).
		 *
		 * @return site change stamp
		 * @since 2.0
		 */
		public long getChangeStamp();

		/**
		 * Returns a stamp reflecting the current state of the features on the site.
		 * If called repeatedly, returns the same value as long as no changes were made to
		 * features on the site.
		 *
		 * @return site features change stamp
		 * @since 2.0
		 * @deprecated Do not use this method
		 */
		@Deprecated
		public long getFeaturesChangeStamp();

		/**
		 * Returns a stamp reflecting the current state of the plug-ins on the site.
		 * If called repeatedly, returns the same value as long as no changes were made to
		 * plug-ins on the site.
		 *
		 * @return site plug-ins change stamp
		 * @since 2.0
		 * @deprecated Do not use this method
		 */
		@Deprecated
		public long getPluginsChangeStamp();

		/**
		 * Returns an indication whether the site can be updated.
		 *
		 * @return <code>true</code> if site can be updated, <code>false</code> otherwise
		 * @since 2.0
		 */
		public boolean isUpdateable();

		/**
		 * Returns an indication whether the site represents an install site
		 * that has been linked via a native installer (using the links/linkfile
		 * mechanism)
		 *
		 * @return <code>true</code> if the site is linked, <code>false</code> otherwise
		 * @since 2.0
		 */
		public boolean isNativelyLinked();
	}

	/**
	 * Site policy. The site policy object determines how plug-ins
	 * contained on the site are processed during startup. In general,
	 * there are 3 ways of configuring a site policy
	 * <ul>
	 * <li>explicitly specify which plug-ins are to be included at
	 * startup (type==USER_INCLUDE). Any other plug-ins located
	 * at the site are ignored at startup. This is typically the best
	 * policy when using remote sites where the user wishes
	 * to retain explicit control over the plug-ins that are included
	 * from such site.
	 * <li>explicitly specify which plug-ins are to be excluded at
	 * startup (type==USER-EXCLUDE). All other plug-ins located
	 * at the site are used at startup. This policy requires that
	 * the site support an access "protocol" that allows plug-in
	 * discovery. In general, these are sites defined using the "file"
	 * URL protocol. This is typically the best policy for local
	 * install sites (on the user system).
	 * </ul>
	 *
	 * @since 2.0
	 */
	@Deprecated
	public interface ISitePolicy {

		/**
		 * Policy type constants.
		 */

		/**
		 * User-defined inclusion list. The list associated with this
		 * policy type is interpreted as path entries to included plugin.xml
		 * or fragment.xml <b>relative</b> to the site URL
		 */
		public static final int USER_INCLUDE = 0;

		/**
		 * User-defined exclusion list. The list associated with this
		 * policy type is interpreted as path entries to excluded plugin.xml
		 * or fragment.xml <b>relative</b> to the site URL
		 */
		public static final int USER_EXCLUDE = 1;

		/**
		 * When this site policy is used, only plug-ins specified by the configured features
		 * are contributed to the runtime.
		 * @since 3.1
		 */
		public static final int MANAGED_ONLY = 2;

		/**
		 * Return policy type
		 *
		 * @return policy type
		 * @since 2.0
		 */
		public int getType();

		/**
		 * Return policy inclusion/ exclusion list
		 *
		 * @return the list as an array
		 * @since 2.0
		 */
		public String[] getList();

		/**
		 * Set new policy list. The list entries are interpreted based on the policy
		 * type. See description of the policy type constants for details.
		 *
		 * @param list policy inclusion/ exclusion list as an array.
		 * Returns an empty array if there are no entries.
		 * @see #USER_INCLUDE
		 * @see #USER_EXCLUDE
		 * @since 2.0
		 */
		public void setList(String[] list);
	}

	/**
	 * Feature entry.
	 * Represents runtime "hints" about configured features.
	 * The information is used during execution to locate the
	 * correct attribution information for a feature. Note,
	 * that a typical configuration can declare multiple feature
	 * entries. At execution time, only one can be selected as
	 * the active primary feature. This is determined based on
	 * specified command line arguments or computed defaults.
	 *
	 * @since 2.0
	 */
	@Deprecated
	public interface IFeatureEntry {

		/**
		 * Returns feature identifier.
		 * @return feature identifier
		 * @since 2.0
		 */
		public String getFeatureIdentifier();

		/**
		 * Returns the currently configured version for the feature.
		 * @return feature version (as string), or <code>null</code>
		 * @since 2.0
		 */
		public String getFeatureVersion();

		/**
		 * Returns the identifier of the feature plug-in for this feature entry.
			 *  Note,      that there is no guarantee that a feature in fact
			 * supplies a corresponding feature plugin, so the result can be
			 * <code>null</code>. Also, if supplied, there is no guarantee that the
			 * plugin will in fact be loaded into the plug-in registry at runtime
			 * (due to rules and constraint checking performed by the registry
			 * loading support). Consequently code making use of this method must
			 * handle these conditions.
		 * @return feature identifier (as string), or <code>null</code>
		 * @since 2.1
		 */
		public String getFeaturePluginIdentifier();

		/**
		 * Returns the version of the feature plug-in for this feature
		 * entry. Note, that there is no guarantee that a feature in fact
		 * supplies a corresponding feature plugin, so the result can be
		 * <code>null</code>. Also, if supplied, there is no guarantee that the
		 * plugin will in fact be loaded into the plug-in registry at runtime
		 * (due to rules and constraint checking performed by the registry
		 * loading support). Consequently code making use of this method must
		 * handle these conditions.
		 * @return feature version (as string), or <code>null</code>
		 * @since 2.0
		 */
		public String getFeaturePluginVersion();

		/**
		 * Returns the application to run when this feature is the
		 * primary feature.
		 * @return application identifier, or <code>null</code>
		 * @since 2.0
		 */
		public String getFeatureApplication();

		/**
		 * Returns URLs to the feature "root" locations. The root
		 * URLs are install locations of the feature plugin and its
		 * fragments.
		 *
		 * @return array of URLs, or an empty array
		 * @since 2.0
		 */
		public URL[] getFeatureRootURLs();

		/**
		 * Returns an indication whether this feature has been defined
		 * to act as a primary feature.
		 * @return <code>true</code> if the feature can be primary,
		 * <code>false</code> otherwise.
		 * @since 2.0
		 */
		public boolean canBePrimary();
	}

	/**
	 * Create a site entry
	 *
	 * @param url site URL
	 * @param policy site policy
	 * @return created site entry
	 * @since 2.0
	 */
	@Deprecated
	public ISiteEntry createSiteEntry(URL url, ISitePolicy policy);

	/**
	 * Create a site policy. The policy determines the way the site
	 * plug-in are processed at startpu
	 *
	 * @param type policy type
	 * @param list an array of site-relative paths representing the
	 * inclusion/ exclusion list
	 * @return created site policy entry
	 * @since 2.0
	 */
	@Deprecated
	public ISitePolicy createSitePolicy(int type, String[] list);

	/**
	 * Create a feature entry
	 * @param id feature identifier. Must not be <code>null</code>.
	 * @param version feature version (as String). Can be <code>null</code>.
	 * @param pluginVersion version of the feature plugin (as String). Can be
	 * <code>null</code>.
	 * @param primary <code>true</code> if the feature is defined as a primary
	 * feature, otherwise <code>false</code>.
	 * @param application identifier of the application to run when
	 * this feature is the primary feature. Can be <code>null</code>.
	 * If specified, the identifier must represent a valid extension
	 * registered in the <code>org.eclipse.core.runtime.applications</code>
	 * extension point.
	 * @param root an array of URLs to feature root directories.
	 * These are URLs to install locations for the feature plugin
	 * and its fragments. Can be <code>null</code>.
	 * @return create feature entry
	 * @since 2.0
	 */
	@Deprecated
	public IFeatureEntry createFeatureEntry(String id, String version, String pluginVersion, boolean primary, String application, URL[] root);

	/**
	 * Create a feature entry
	 * @param id feature identifier. Must not be <code>null</code>.
	 * @param version feature version (as String). Can be <code>null</code>.
	 * @param pluginIdentifier identifier of the feature plugin (as String). Can
	 * be <code>null</code>.
	 * @param  pluginVersion  version of the feature plugin (as String). Can be
	 * <code>null</code>.
	 * @param primary <code>true</code> if the feature is defined as a primary
	 * feature, otherwise <code>false</code>.
	 * @param application identifier of the application to run when
	 * this feature is the primary feature. Can be <code>null</code>.
	 * If specified, the identifier must represent a valid extension
	 * registered in the <code>org.eclipse.core.runtime.applications</code>
	 * extension point.
	 * @param root an array of URLs to feature root directories.
	 * These are URLs to install locations for the feature plugin
	 * and its fragments. Can be <code>null</code>.
	 * @return create feature entry
	 * @since 2.1
	 */
	@Deprecated
	public IFeatureEntry createFeatureEntry(String id, String version, String pluginIdentifier, String pluginVersion, boolean primary, String application, URL[] root);

	/**
	 * Configures the specified site entry. If a site entry with the
	 * same site URL is already configured, the entry is <b>not</b> replaced.
	 *
	 * @param entry site entry
	 * @since 2.0
	 */
	@Deprecated
	public void configureSite(ISiteEntry entry);

	/**
	 * Configures the specified site entry. If a site entry with the
	 * same site URL is already configured, the replacement behavior for
	 * the entry can be specified.
	 *
	 * @param entry site entry
	 * @param  replace indicating whether an existing configured entry with
	 * the same URL should be replaced (<code>true</code>) or not (<code>false</code>).
	 * @since 2.0
	 */
	@Deprecated
	public void configureSite(ISiteEntry entry, boolean replace);

	/**
	 * Unconfigures the specified entry. Does not do anything if the entry
	 * is not configured.
	 *
	 * @param entry site entry
	 * @since 2.0
	 */
	@Deprecated
	public void unconfigureSite(ISiteEntry entry);

	/**
	 * Returns configured site entries
	 *
	 * @return array of site entries. Returns an empty array if no sites are
	 * configured
	 * @since 2.0
	 */
	@Deprecated
	public ISiteEntry[] getConfiguredSites();

	/**
	 * Returns a site entry matching the specified URL
	 *
	 * @param url site url
	 * @return matching site entry, or <code>null</code> if no match found
	 * @since 2.0
	 */
	@Deprecated
	public ISiteEntry findConfiguredSite(URL url);

	/**
	 * Configures the feature entry.
	 * If another feature entry with the same feature identifier
	 * already exists, it is replaced.
	 * @param entry feature entry
	 * @since 2.0
	 */
	@Deprecated
	public void configureFeatureEntry(IFeatureEntry entry);

	/**
	 * Unconfigures the specified feature entry if it exists.
	 * @param entry feature entry
	 * @since 2.0
	 */
	@Deprecated
	public void unconfigureFeatureEntry(IFeatureEntry entry);

	/**
	 * Returns a list of configured feature entries.
	 * @return array or entries, or an empty array if no entries
	 * are configured
	 * @since 2.0
	 */
	@Deprecated
	public IFeatureEntry[] getConfiguredFeatureEntries();

	/**
	 * Locates the specified feature entry.
	 * @param id feature identifier
	 * @return ferature entry, or <code>null</code>.
	 * @since 2.0
	 */
	@Deprecated
	public IFeatureEntry findConfiguredFeatureEntry(String id);

	/**
	 * Returns the URL location of the configuration information
	 *
	 * @return configuration location URL, or <code>null</code> if the
	 * configuration location could not be determined.
	 * @since 2.0
	 */
	@Deprecated
	public URL getConfigurationLocation();

	/**
	 * Returns a stamp reflecting the current state of the configuration. If called repeatedly,
	 * returns the same value as long as no changes were made to the configuration (changes to
	 * sites, features or plugins).
	 *
	 * @return configuration change stamp
	 * @since 2.0
	 */
	@Deprecated
	public long getChangeStamp();

	/**
	 * Returns a stamp reflecting the current state of the features in the configuration.
	 * If called repeatedly, returns the same value as long as no changes were made to
	 * features in the configuration.
	 *
	 * @return configuration features change stamp
	 * @since 2.0
	 * @deprecated Do not use this method.
	 */
	@Deprecated
	public long getFeaturesChangeStamp();

	/**
	 * Returns a stamp reflecting the current state of the plug-ins in the configuration.
	 * If called repeatedly, returns the same value as long as no changes were made to
	 * plug-ins in the configuration.
	 *
	 * @return configuration plug-ins change stamp
	 * @since 2.0
	 * @deprecated Do not use this method
	 */
	@Deprecated
	public long getPluginsChangeStamp();

	/**
	 * Returns the identifier of the configured primary feature. A primary feature
	 * is used to specify product customization information for a running instance
	 * of Eclipse.
	 *
	 * @return primary feature identifier, or <code>null</code> if none configured
	 * @since 2.0
	 */
	@Deprecated
	public String getPrimaryFeatureIdentifier();

	/**
	 * Computes the plug-in path for this configuration. The result includes all plug-ins
	 * visible on each of the configured sites based on each site policy.
	 *
	 * @return an array of plug-in path elements (full URL entries), or an empty array.
	 * @since 2.0
	 */
	@Deprecated
	public URL[] getPluginPath();

	/**
	 * Returns an array of bootstrap plugin identifiers whose
	 * location needs to be explicitly identified in the configuration.
	 *
	 * @return an array of identifiers, or empty array
	 * otherwise
	 * @since 2.0
	 * @deprecated Do not use this method. Check the osgi.bundles system property for the
	 * urls of the automatically started bundles
	 */
	@Deprecated
	public String[] getBootstrapPluginIdentifiers();

	/**
	 * Sets the location of a bootstrap plugin.
	 *
	 * @see IPlatformConfiguration#getBootstrapPluginIdentifiers()
	 * @param id plugin identifier. Must match one of the entries returned
	 * by getBootstrapPluginIdentifiers()
	 * @param location Location of the bootstrap plugin.
	 * @since 2.0
	 * @deprecated Do not use this method. Use the osig.bundles system property contains
	 * the urls of the automatically started bundles.
	 */
	@Deprecated
	public void setBootstrapPluginLocation(String id, URL location);

	/**
	 * Returns an indication whether the configuration can be updated.
	 *
	 * @return <code>true</code> if configuration can be updated, <code>false</code>
	 * otherwise
	 * @since 2.0
	 */
	@Deprecated
	public boolean isUpdateable();

	/**
	 * Returns an indication whether the configuration is transient. A transient
	 * configuration typically represents a scenario where the configuration
	 * was computed for a single instantiation of the platform and is not
	 * guaranteed to be valid on subsequent instantiations.
	 *
	 * @return <code>true</code> if configuration is transient, <code>false</code>
	 * otherwise
	 * @since 2.0
	 */
	@Deprecated
	public boolean isTransient();

	/**
	 * Indicates whether the configuration is transient or not. A transient
	 * configuration typically represents a scenario where the configuration
	 * was computed for a single instantiation of the platform and is not
	 * guaranteed to be valid on subsequent instantiations. This method has
	 * no effect if called on the current platform configuration.
	 *
	 * @param value <code>true</code> if configuration is transient, <code>false</code>
	 * otherwise
	 * @since 2.0
	 */
	@Deprecated
	public void isTransient(boolean value);

	/**
	 * Called to refresh the configuration information. In particular,
	 * causes change stamps to be recomputed based on the current
	 * configuration state, and updates the lists of available plug-ins.
	 * @since 2.0
	 */
	@Deprecated
	public void refresh();

	/**
	 * Called to save the configuration information
	 * @since 2.0
	 */
	@Deprecated
	public void save() throws IOException;

	/**
	 * Called to save the configuration information in the
	 * specified location
	 *
	 * @param url save location.
	 * @since 2.0
	 */
	@Deprecated
	public void save(URL url) throws IOException;

}
