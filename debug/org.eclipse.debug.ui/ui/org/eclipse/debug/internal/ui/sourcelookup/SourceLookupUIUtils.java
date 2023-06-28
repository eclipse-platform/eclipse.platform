/*******************************************************************************
 * Copyright (c) 2003, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.sourcelookup;

import java.util.Hashtable;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.sourcelookup.ISourceContainerBrowser;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;

/**
 * Utility methods for the UI portion of the source lookup solution.
 *
 * @since 3.0
 */
public class SourceLookupUIUtils {
	/**
	 * Constant for the container presentation extension id.
	 * @since 3.0
	 */
	public static final String CONTAINER_PRESENTATION_EXTENSION = "sourceContainerPresentations"; //$NON-NLS-1$
	/**
	 * Constant for the container presentation icon attribute.
	 * @since 3.0
	 */
	public static final String ICON_ATTRIBUTE = "icon";	 //$NON-NLS-1$
	/**
	 * Constant for the container presentation browser attribute.
	 * @since 3.0
	 */
	public static final String BROWSER_CLASS_ATTRIBUTE = "browserClass"; //$NON-NLS-1$
	/**
	 * Constant for the container presentation type id attribute.
	 * @since 3.0
	 */
	public static final String CONTAINER_ID_ATTRIBUTE = "containerTypeID";	//$NON-NLS-1$

	private static Hashtable<String, IConfigurationElement> fSourceContainerPresentationHashtable;

	/**
	 * Constructor. Reads in Source Container Presentation extension implementations.
	 */
	public SourceLookupUIUtils(){
		IExtensionPoint extensionPoint= Platform.getExtensionRegistry().getExtensionPoint(DebugUIPlugin.getUniqueIdentifier(), CONTAINER_PRESENTATION_EXTENSION);
		//read in SourceContainer presentation extensions
		IConfigurationElement[] sourceContainerPresentationExtensions =extensionPoint.getConfigurationElements();
		fSourceContainerPresentationHashtable = new Hashtable<>();
		for (IConfigurationElement extension : sourceContainerPresentationExtensions) {
			fSourceContainerPresentationHashtable.put(extension.getAttribute(CONTAINER_ID_ATTRIBUTE), extension);
			registerContainerImages(extension);
		}
	}


	/**
	 * Retrieves the icon associated with a source container type.
	 * @param id the container type id
	 * @return the image for the type specified
	 */
	public static Image getSourceContainerImage(String id){
		if(fSourceContainerPresentationHashtable == null) {
			new SourceLookupUIUtils();
		}
		return DebugPluginImages.getImage(id);
	}

	/**
	 * Retrieves the browser class associated with the source container type specified.
	 * @param typeID the source container type id
	 * @return the browser class
	 */
	public static ISourceContainerBrowser getSourceContainerBrowser(String typeID)
	{
		if(fSourceContainerPresentationHashtable == null) {
			new SourceLookupUIUtils();
		}
		IConfigurationElement element = fSourceContainerPresentationHashtable.get(typeID);
		ISourceContainerBrowser browser = null;
		try{
			if(element!= null && element.getAttribute(BROWSER_CLASS_ATTRIBUTE) != null) {
				browser = (ISourceContainerBrowser) element.createExecutableExtension(BROWSER_CLASS_ATTRIBUTE);
			}
		}catch(CoreException e){}
		return browser;
	}

	private void registerContainerImages(IConfigurationElement configElement){
		ImageDescriptor imageDescriptor = DebugUIPlugin.getImageDescriptor(configElement, ICON_ATTRIBUTE);
		if (imageDescriptor == null) {
			imageDescriptor = ImageDescriptor.getMissingImageDescriptor();
		}
		String configTypeID = configElement.getAttribute(CONTAINER_ID_ATTRIBUTE);
		ImageRegistry imageRegistry = DebugPluginImages.getImageRegistry();
		imageRegistry.put(configTypeID, imageDescriptor);
	}

}
