package org.eclipse.update.core.model;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

/**
 * Plug-in dependency model object.
 * <p>
 * This class may be instantiated or subclassed by clients. However, in most 
 * cases clients should instead instantiate or subclass the provided 
 * concrete implementation of this model.
 * </p>
 * @see org.eclipse.update.core.Import
 * @since 2.0
 */
public class ImportModel extends ModelObject {

	private String id;
	private String version;
	private String matchingRuleName;
	private boolean featureImport;

	/**
	 * Creates a uninitialized plug-in dependency model object.
	 * 
	 * @since 2.0
	 */
	public ImportModel() {
		super();
	}

	/**
	 * Returns the dependent plug-in identifier.
	 *
	 * @deprecated use getIdentifier() instead
	 * @return plug-in identifier, or <code>null</code>.
	 * @since 2.0
	 */
	public String getPluginIdentifier() {
		return id;
	}

	/**
	 * Returns the dependent identifier.
	 *
	 * @return  identifier, or <code>null</code>.
	 * @since 2.0.2
	 */
	public String getIdentifier() {
		return id;
	}

	/**
	 * Returns the dependent plug-in version.
	 *
	 * @deprecated use getVersion() instead
	 * @return plug-in version, or <code>null</code>.
	 * @since 2.0
	 */
	public String getPluginVersion() {
		return version;
	}

	/**
	 * Returns the dependent version.
	 *
	 * @return version, or <code>null</code>.
	 * @since 2.0.2
	 */
	public String getVersion() {
		return version;
	}
	
	/**
	 * Returns the dependent version matching rule name.
	 *
	 * @return matching rule name, or <code>null</code>.
	 * @since 2.0
	 */
	public String getMatchingRuleName() {
		return matchingRuleName;
	}

	/**
	 * Sets the dependent plug-in identifier.
	 * Throws a runtime exception if this object is marked read-only.
	 *
	 * @deprecated use setIdentifier()
	 * @param pluginId dependent plug-in identifier
	 * @since 2.0
	 */
	public void setPluginIdentifier(String pluginId) {
		assertIsWriteable();
		this.id = pluginId;
	}

	/**
	 * Sets the dependent plug-in version.
	 * Throws a runtime exception if this object is marked read-only.
	 *
	 * @deprecated use setVersion()
	 * @param pluginVersion dependent plug-in version
	 * @since 2.0
	 */
	public void setPluginVersion(String pluginVersion) {
		assertIsWriteable();
		this.version = pluginVersion;
	}

	/**
	 * Sets the dependent identifier.
	 * Throws a runtime exception if this object is marked read-only.
	 *
	 * @param id dependent identifier
	 * @since 2.0.2
	 */
	public void setIdentifier(String id) {
		assertIsWriteable();
		this.id = id;
	}

	/**
	 * Sets the dependent version.
	 * Throws a runtime exception if this object is marked read-only.
	 *
	 * @param version dependent version
	 * @since 2.0.2
	 */
	public void setVersion(String version) {
		assertIsWriteable();
		this.version = version;
	}
	
	/**
	 * Sets the dependent version matching rule name. 
	 * Throws a runtime exception if this object is marked read-only.
	 *
	 * @param matchingRuleName dependent version matching rule.
	 * @since 2.0
	 */
	public void setMatchingRuleName(String matchingRuleName) {
		assertIsWriteable();
		this.matchingRuleName = matchingRuleName;
	}
	/**
	 * Returns the isFeatureImport.
	 * @return boolean
	 */
	public boolean isFeatureImport() {
		return featureImport;
	}

	/**
	 * Sets the featureImport.
	 * @param featureImport The featureImport to set
	 */
	public void setFeatureImport(boolean featureImport) {
		this.featureImport = featureImport;
	}

}