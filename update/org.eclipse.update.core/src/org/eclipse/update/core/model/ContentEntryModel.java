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
 * Content entry model object.
 * This is the base class for plug-in and non-plug-in entry models.
 * <p>
 * This class must be subclassed by clients. 
 * </p>
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @see org.eclipse.update.core.model.PluginEntryModel
 * @see org.eclipse.update.core.model.NonPluginEntryModel
 * @since 2.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public abstract class ContentEntryModel extends ModelObject {

	/**
	 * An indication the size could not be determined
	 * 
	 * @since 2.0
	 */
	public static final long UNKNOWN_SIZE = -1;

	private long downloadSize = UNKNOWN_SIZE;
	private long installSize = UNKNOWN_SIZE;
	private String os;
	private String ws;
	private String nl;
	private String arch;

	/**
	 * Creates a uninitialized content entry model object.
	 * 
	 * @since 2.0
	 */
	protected ContentEntryModel() {
		super();
	}

	/**
	 * Returns the download size of the entry, if it can be determined.
	 * 
	 * @return download size of the entry in KiloBytes, or an indication 
	 * the size could not be determined
	 * @since 2.0 
	 */

	public long getDownloadSize() {
		return downloadSize;
	}

	/**
	 * Returns the install size of the entry, if it can be determined.
	 * 
	 * @return install size of the entry in KiloBytes, or an indication 
	 * the size could not be determined
	 * @since 2.0
	 */
	public long getInstallSize() {
		return installSize;
	}

	/**
	 * Returns optional operating system specification.
	 *
	 * @return the operating system specification or <code>null</code>.
	 * @since 2.0 
	 */
	public String getOS() {
		return os;
	}

	/**
	 * Returns optional windowing system specification.
	 *
	 * @return the windowing system specification or <code>null</code>.
	 * @since 2.0 
	 */

	public String getWS() {
		return ws;
	}

	/**
	 * Returns optional system architecture specification. 
	 * 
	 * @return the system architecture specification or <code>null</code>.
	 * @since 2.0 
	 */
	public String getOSArch() {
		return arch;
	}

	/**
	 * Returns optional locale specification.
	 *
	 * @return the locale specification, or <code>null</code>.
	 * @since 2.0 
	 */
	public String getNL() {
		return nl;
	}

	/**
	 * Sets the download size of the entry.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param downloadSize download size of the entry in KiloBytes
	 * @since 2.0 
	 */
	public void setDownloadSize(long downloadSize) {
		assertIsWriteable();
		if (downloadSize < 0)
			this.downloadSize = UNKNOWN_SIZE;
		else
			this.downloadSize = downloadSize;
	}

	/**
	 * Sets the install size of the entry.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param installSize install size of the entry in KiloBytes
	 * @since 2.0
	 */
	public void setInstallSize(long installSize) {
		assertIsWriteable();
		if (installSize < 0)
			this.installSize = UNKNOWN_SIZE;
		else
			this.installSize = installSize;
	}

	/**
	 * Sets the operating system specification.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param os comma-separated list of OS identifiers as defined by Eclipse.
	 * @since 2.0
	 */
	public void setOS(String os) {
		assertIsWriteable();
		this.os = os;
	}

	/**
	 * Sets the windowing system specification.
	 * Throws a runtime exception if this object is marked read-only.
	 *
	 * @param ws comma-separated list of WS identifiers as defined by Eclipse.
	 * @since 2.0
	 */
	public void setWS(String ws) {
		assertIsWriteable();
		this.ws = ws;
	}

	/**
	 * Sets the system architecture specification.
	 * Throws a runtime exception if this object is marked read-only.
	 *
	 * @param arch comma-separated list of arch identifiers as defined by Eclipse.
	 * @since 2.0
	 */
	public void setArch(String arch) {
		assertIsWriteable();
		this.arch = arch;
	}

	/**
	 * Sets the locale specification.
	 * Throws a runtime exception if this object is marked read-only.
	 *
	 * @param nl comma-separated list of locale identifiers.
	 * @since 2.0
	 */
	public void setNL(String nl) {
		assertIsWriteable();
		this.nl = nl;
	}
}
