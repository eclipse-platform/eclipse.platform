/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.core.model;

/**
 * Plug-in dependency model object.
 * <p>
 * This class may be instantiated or subclassed by clients. However, in most 
 * cases clients should instead instantiate or subclass the provided 
 * concrete implementation of this model.
 * </p>
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @see org.eclipse.update.core.Import
 * @since 2.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public class ImportModel extends ModelObject {

	private String id;
	private String version;
	private String matchingIdRuleName;
	private String matchingRuleName;
	private boolean featureImport;
	private boolean patch;
	private String osArch;
	private String ws;
	private String os;
	private String nl;

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
	 * Returns the dependent id matching rule name.
	 *
	 * @return matching rule name, or <code>null</code>.
	 * @since 2.1
	 */
	public String getMatchingIdRuleName() {
		return matchingIdRuleName;
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
	 * Sets the dependent id matching rule name. 
	 * Throws a runtime exception if this object is marked read-only.
	 *
	 * @param matchingIdRuleName dependent id matching rule.
	 * @since 2.1
	 */
	public void setMatchingIdRuleName(String matchingIdRuleName) {
		assertIsWriteable();
		this.matchingIdRuleName = matchingIdRuleName;
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
	
	/**
	 * Returns the patch mode.
	 */
	public boolean isPatch() {
		return patch;
	}
	
	/**
	 * Sets the patch mode.
	 */
	public void setPatch(boolean patch) {
		this.patch = patch;
	}
	/**
	 * Returns the os.
	 * @return String
	 */
	public String getOS() {
		return os;
	}

	/**
	 * Returns the osArch.
	 * @return String
	 */
	public String getOSArch() {
		return osArch;
	}

	/**
	 * Returns the ws.
	 * @return String
	 */
	public String getWS() {
		return ws;
	}

	/**
	 * Sets the os.
	 * @param os The os to set
	 */
	public void setOS(String os) {
		this.os = os;
	}

	/**
	 * Sets the osArch.
	 * @param osArch The osArch to set
	 */
	public void setOSArch(String osArch) {
		this.osArch = osArch;
	}

	/**
	 * Sets the ws.
	 * @param ws The ws to set
	 */
	public void setWS(String ws) {
		this.ws = ws;
	}

	/**
	 * Returns the nl.
	 * @return String
	 */
	public String getNL() {
		return nl;
	}

	/**
	 * Sets the nl.
	 * @param nl The nl to set
	 */
	public void setNL(String nl) {
		this.nl = nl;
	}

}
