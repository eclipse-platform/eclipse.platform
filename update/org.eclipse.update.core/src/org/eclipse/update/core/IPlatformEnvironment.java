package org.eclipse.update.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.IAdaptable;

/**
 * Plug-in entry defines a packaging reference from a feature to a plug-in.
 * It indicates that the referenced plug-in is to be considered as
 * part of the feature. Note, that this does not necessarily indicate
 * that the plug-in files are packaged together with any other
 * feature files. The actual packaging details are determined by the
 * feature content provider for the feature.
 * <p>
 * Clients may implement this interface. However, in most cases clients should 
 * directly instantiate or subclass the provided implementation of this 
 * interface.
 * </p>
 * @see org.eclipse.update.core.PluginEntry
 * @see org.eclipse.update.core.FeatureContentProvider
 * @since 2.0
 */
public interface IPlatformEnvironment extends IAdaptable {


	/**
	 * Returns optional operating system specification.
	 * A comma-separated list of os designators defined by the platform.
	 * Indicates this entry should only be installed on one of the specified
	 * os systems. If this attribute is not specified, the entry can be
	 * installed on all systems (portable implementation).
	 * 
	 * This information is used as a hint by the installation and update
	 * support.
	 *
	 * @see org.eclipse.core.boot.BootLoader 
	 * @return the operating system specification, or <code>null</code>.
	 * @since 2.0 
	 */
	public String getOS();

	/**
	 * Returns optional system architecture specification. 
	 * A comma-separated list of arch designators defined by the platform.
	 * Indicates this entry should only be installed on one of the specified
	 * systems. If this attribute is not specified, the entry can be
	 * installed on all systems (portable implementation).
	 * 
	 * This information is used as a hint by the installation and update
	 * support.
	 * 
	 * @see org.eclipse.core.boot.BootLoader 
	 * @return system architecture specification, or <code>null</code>.
	 * @since 2.0 
	 */
	public String getWS();

	/**
	 * Returns optional system architecture specification. 
	 * A comma-separated list of arch designators defined by the platform.
	 * Indicates this entry should only be installed on one of the specified
	 * systems. If this attribute is not specified, the entry can be
	 * installed on all systems (portable implementation).
	 * 
	 * This information is used as a hint by the installation and update
	 * support.
	 * 
	 * @see org.eclipse.core.boot.BootLoader 
	 * @return system architecture specification, or <code>null</code>.
	 * @since 2.0 
	 */
	public String getOSArch();

	/**
	 * Returns optional locale specification. 
	 * A comma-separated list of locale designators defined by Java.
	 * Indicates this entry should only be installed on a system running
	 * with a compatible locale (using Java locale-matching rules).
	 * If this attribute is not specified, the entry can be installed 
	 * on all systems (language-neutral implementation). 
	 * 
	 * This information is used as a hint by the installation and update
	 *  support.
	 * 
	 * @return the locale specification, or <code>null</code>.
	 * @since 2.0 
	 */
	public String getNL();


}