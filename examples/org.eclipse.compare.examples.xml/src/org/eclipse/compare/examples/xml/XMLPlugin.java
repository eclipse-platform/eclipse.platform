/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.compare.examples.xml;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.compare.CompareUI;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * This class is the plug-in runtime class for the 
 * <code>"org.eclipse.compare.xml"</code> plug-in.
 */
public final class XMLPlugin extends AbstractUIPlugin {
	
	public static final String PLUGIN_ID= "org.eclipse.compare.examples.xml"; //$NON-NLS-1$

	private static final String ID_MAPPING_EXTENSION_POINT= "idMapping"; //$NON-NLS-1$
	private static final String MAPPING_ELEMENT_NAME= "mapping"; //$NON-NLS-1$
	private static final String IDMAP_NAME_ATTRIBUTE= "name"; //$NON-NLS-1$
	private static final String EXTENSION_NAME_ATTRIBUTE= "extension"; //$NON-NLS-1$
	private static final String MAPPING_SIGNATURE_ATTRIBUTE= "signature"; //$NON-NLS-1$
	private static final String MAPPING_ID_ATTRIBUTE= "id"; //$NON-NLS-1$
	private static final String MAPPING_ID_SOURCE= "id-source"; //$NON-NLS-1$
	private static final String MAPPING_ID_SOURCE_BODY= "body"; //$NON-NLS-1$
	private static final String ORDERED_ELEMENT_NAME= "ordered"; //$NON-NLS-1$
	private static final String ORDERED_SIGNATURE_ATTRIBUTE= "signature"; //$NON-NLS-1$
	
	public static final String DEFAULT_PREFIX = "XML"; //$NON-NLS-1$
	public static final String IMAGE_TYPE_PREFIX = "xml_"; //$NON-NLS-1$
	public static final String IMAGE_TYPE_ORDERED_SUFFIX = "_ordered"; //$NON-NLS-1$
	public static final String IDMAP_PREFERENCE_NAME = "idmap"; //$NON-NLS-1$
	public static final String IDMAP_PREFIX = "idmap"; //$NON-NLS-1$
	public static final char IDMAP_SEPARATOR = '*';
	public static final char IDMAP_FIELDS_SEPARATOR = '!';
	
	public static final String ORDERED_PREFERENCE_NAME = "ordered"; //$NON-NLS-1$
	public static final char ORDERED_FIELDS_SEPARATOR = IDMAP_FIELDS_SEPARATOR;
	
	private static XMLPlugin fgXMLPlugin;
	private IPreferenceStore fPrefStore;
	
	private HashMap fIdMapsInternal;
	private HashMap fIdMaps;
	private HashMap fIdExtensionToName;
	private HashMap fOrderedElementsInternal;
	private HashMap fOrderedElements;
	
	private ListenerList fViewers= new ListenerList();


	/**
	 * Creates the <code>XMLPlugin</code> object and registers all
	 * structure creators, content merge viewers, and structure merge viewers
	 * contributed to this plug-in's extension points.
	 * <p>
	 * Note that instances of plug-in runtime classes are automatically created 
	 * by the platform in the course of plug-in activation.
	 */
	public XMLPlugin() {
		super();
		Assert.isTrue(fgXMLPlugin == null);
		fgXMLPlugin= this;
	}
	
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		
		CompareUI.removeAllStructureViewerAliases(DEFAULT_PREFIX);
		initPrefStore();
		CompareUI.registerImageDescriptor(IMAGE_TYPE_PREFIX + XMLStructureCreator.TYPE_ELEMENT, getImageDescriptor("obj16/element_obj.gif")); //$NON-NLS-1$
		CompareUI.registerImageDescriptor(IMAGE_TYPE_PREFIX + XMLStructureCreator.TYPE_ATTRIBUTE, getImageDescriptor("obj16/attribute_obj.gif")); //$NON-NLS-1$
		CompareUI.registerImageDescriptor(IMAGE_TYPE_PREFIX + XMLStructureCreator.TYPE_TEXT, getImageDescriptor("obj16/text_obj.gif")); //$NON-NLS-1$
		CompareUI.registerImageDescriptor(IMAGE_TYPE_PREFIX + XMLStructureCreator.TYPE_ELEMENT + IMAGE_TYPE_ORDERED_SUFFIX, getImageDescriptor("obj16/element_ordered_obj.gif")); //$NON-NLS-1$
		registerExtensions();
	}
		
	protected ImageDescriptor getImageDescriptor(String relativePath) {
		
		//URL installURL= getDescriptor().getInstallURL();
		
		URL installURL= fgXMLPlugin.getBundle().getEntry("/"); //$NON-NLS-1$
		if (installURL != null) {
			try {
				URL url= new URL(installURL, "icons/full/" + relativePath); //$NON-NLS-1$
				return ImageDescriptor.createFromURL(url);
			} catch (MalformedURLException e) {
				Assert.isTrue(false);
			}
		}
		return null;
	}

	/**
	 * Returns the singleton instance of this plug-in runtime class.
	 *
	 * @return the XMLPlugin instance
	 */
	public static XMLPlugin getDefault() {
		return fgXMLPlugin;
	}

	/**
	 * Reads the Preference Store associated with XMLPlugin and initializes ID Mappings.
	 */	
	public void initPrefStore() {
		fIdMaps = new HashMap();
		fIdExtensionToName= new HashMap();
		fPrefStore = getPreferenceStore();
		String IdMapPrefValue = fPrefStore.getString(IDMAP_PREFERENCE_NAME);
		int start = 0;
		int end = IdMapPrefValue.indexOf(IDMAP_SEPARATOR);
		while (end >= 0) {
			String CurrentIdMap = IdMapPrefValue.substring(start,end);
			int end_of_IdMapName = CurrentIdMap.indexOf(IDMAP_FIELDS_SEPARATOR);
			String IdMapName = CurrentIdMap.substring(0,end_of_IdMapName);
			int end_of_signature = CurrentIdMap.indexOf(IDMAP_FIELDS_SEPARATOR,end_of_IdMapName+1);
			String IdMapSignature = CurrentIdMap.substring(end_of_IdMapName+1,end_of_signature);
			int end_of_attribute= CurrentIdMap.indexOf(IDMAP_FIELDS_SEPARATOR,end_of_signature+1);
			String IdMapAttribute;
			if (end_of_attribute < 0) {//for backward compatibility
				IdMapAttribute = CurrentIdMap.substring(end_of_signature+1,CurrentIdMap.length());
			} else {//normal case
				IdMapAttribute = CurrentIdMap.substring(end_of_signature+1,end_of_attribute);
				String IdMapExtension= CurrentIdMap.substring(end_of_attribute+1,CurrentIdMap.length());
				//if extension already associated, do not associate with this idmap
				if (!IdMapExtension.isEmpty() && !fIdExtensionToName.containsKey(IdMapExtension)) {
					fIdExtensionToName.put(IdMapExtension,IdMapName);
					CompareUI.addStructureViewerAlias(DEFAULT_PREFIX, IdMapExtension);
				}
			}
			
			if (fIdMaps.containsKey(IdMapName)) {
				HashMap Mappings = (HashMap) fIdMaps.get(IdMapName);
				Mappings.put(IdMapSignature,IdMapAttribute);
			} else {
				HashMap Mappings = new HashMap();
				Mappings.put(IdMapSignature,IdMapAttribute);
				fIdMaps.put(IdMapName,Mappings);
			}
			start = end+1;
			end = IdMapPrefValue.indexOf(IDMAP_SEPARATOR,end+1);
		}
		
		fOrderedElements= new HashMap();
		String OrderedPrefValue= fPrefStore.getString(ORDERED_PREFERENCE_NAME);
		StringTokenizer orderedTokens= new StringTokenizer(OrderedPrefValue, (Character.valueOf(ORDERED_FIELDS_SEPARATOR)).toString());
		while (orderedTokens.hasMoreTokens()) {
			String IdMapName= orderedTokens.nextToken();
			String signature= orderedTokens.nextToken();
			if (fOrderedElements.containsKey(IdMapName)) {
				ArrayList idmapAL= (ArrayList) fOrderedElements.get(IdMapName);
				idmapAL.add(signature);
			} else {
				ArrayList idmapAL= new ArrayList();
				idmapAL.add(signature);
				fOrderedElements.put(IdMapName, idmapAL);
			}
		}

	}
	
	/*
	 * Updates the user Id Mappings, the IdExtensionToName mappings and refreshes the preference store.
	 * @param IdMap the new Id Mappings
	 * @param IdExtensionToName the new IdExtensionToName mappings
	 * @param refresh whether all the open StructureViewers should be refreshed with the new IdMapping settings
	 */
	public void setIdMaps(HashMap IdMap, HashMap IdExtensionToName, HashMap OrderedElements, boolean refresh) {
		fIdMaps = IdMap;
		if (IdExtensionToName != null && !IdExtensionToName.equals(fIdExtensionToName)) {
			CompareUI.removeAllStructureViewerAliases(DEFAULT_PREFIX);
			fIdExtensionToName= IdExtensionToName;
			Set newkeySet= fIdExtensionToName.keySet();
			for (Iterator iter= newkeySet.iterator(); iter.hasNext(); ) {
				String extension= (String)iter.next();
				CompareUI.addStructureViewerAlias(DEFAULT_PREFIX, extension);
			}
		}
		StringBuilder IdMapPrefValue = new StringBuilder();
		Set idmapKeys = fIdMaps.keySet();
		for (Iterator iter_idmap = idmapKeys.iterator(); iter_idmap.hasNext(); ) {
			String IdMapName = (String) iter_idmap.next();
			HashMap idmapHM = (HashMap) fIdMaps.get(IdMapName);
			Set mappingKeys = idmapHM.keySet();
			String extension= ""; //$NON-NLS-1$
			if (fIdExtensionToName.containsValue(IdMapName)) {
				Set keySet= fIdExtensionToName.keySet();
				for (Iterator iter= keySet.iterator(); iter.hasNext(); ) {
					extension= (String)iter.next();
					if ( ((String)fIdExtensionToName.get(extension)).equals(IdMapName) )
						break;
				}
			}
			for (Iterator iter_mapping = mappingKeys.iterator(); iter_mapping.hasNext(); ) {
				String signature = (String) iter_mapping.next();
				IdMapPrefValue.append(IdMapName+IDMAP_FIELDS_SEPARATOR+signature+IDMAP_FIELDS_SEPARATOR+idmapHM.get(signature)+IDMAP_FIELDS_SEPARATOR+extension+IDMAP_SEPARATOR);
			}
		}
		fPrefStore.setValue(IDMAP_PREFERENCE_NAME,IdMapPrefValue.toString());
		//fPrefStore.setValue(IDMAP_PREFERENCE_NAME,"");
		
		//stores OrderedElements
		if (OrderedElements != null) {
			fOrderedElements= OrderedElements;
			StringBuilder OrderedPrefValue= new StringBuilder();
			Set orderedKeys= fOrderedElements.keySet();
			for (Iterator iter_ordered= orderedKeys.iterator(); iter_ordered.hasNext();) {
				String IdMapName= (String) iter_ordered.next();
				ArrayList idmapAL= (ArrayList) fOrderedElements.get(IdMapName);
				for (Iterator iter_idmapAL= idmapAL.iterator(); iter_idmapAL.hasNext();) {
					String signature= (String) iter_idmapAL.next();
					OrderedPrefValue.append(IdMapName+ORDERED_FIELDS_SEPARATOR+signature+ORDERED_FIELDS_SEPARATOR);
				}
			}
			fPrefStore.setValue(ORDERED_PREFERENCE_NAME,OrderedPrefValue.toString());
			//fPrefStore.setValue(ORDERED_PREFERENCE_NAME,"");
		}

		if (refresh) {
			Object[] viewers = fViewers.getListeners();
			for (int i = 0; i < viewers.length; ++i) {
				XMLStructureViewer viewer = (XMLStructureViewer) viewers[i];
				viewer.updateIdMaps();
				viewer.contentChanged();
			}
		}
	}
	
	public HashMap getIdMaps() {
		return fIdMaps;
	}
	
	public HashMap getIdMapsInternal() {
		return fIdMapsInternal;
	}
	
	public HashMap getIdExtensionToName() {
		return fIdExtensionToName;
	}

	public HashMap getOrderedElements() {
		return fOrderedElements;
	}

	public HashMap getOrderedElementsInternal() {
		return fOrderedElementsInternal;
	}

	/**
	 * Registers all internal Id Mapping schemes
	 * that are found in plugin.xml files.
	 */
	private void registerExtensions() {
		IExtensionRegistry registry= Platform.getExtensionRegistry();
		
		// collect all Id Mappings
		IConfigurationElement[] idmaps= registry.getConfigurationElementsFor(PLUGIN_ID, ID_MAPPING_EXTENSION_POINT);
		fIdMapsInternal = new HashMap();
		fOrderedElementsInternal= new HashMap();
		for (IConfigurationElement idmap : idmaps) {
			//handle IDMAP_NAME_ATTRIBUTE
			String idmap_name= idmap.getAttribute(IDMAP_NAME_ATTRIBUTE);
			//ignores idmap if its name equals the reserved name for unordered matching or the the name for ordered matching
			if ( !idmap_name.equals(XMLStructureCreator.USE_UNORDERED) && !idmap_name.equals(XMLStructureCreator.USE_ORDERED) ) {
				//handle mappings
				HashMap idmapHM = new HashMap();
				fIdMapsInternal.put(idmap_name, idmapHM);
				IConfigurationElement[] mappings = idmap.getChildren(MAPPING_ELEMENT_NAME);
				for (IConfigurationElement mapping : mappings) {
					//add SIGN_SEPARATOR at the end because not contained in signatures of plugin.xml
					//also add prefix at beginning
					String signature= mapping.getAttribute(MAPPING_SIGNATURE_ATTRIBUTE);
					String attribute= mapping.getAttribute(MAPPING_ID_ATTRIBUTE);
					String idsource= mapping.getAttribute(MAPPING_ID_SOURCE);
					String bodyid= ""; //$NON-NLS-1$
					if (signature != null && !signature.isEmpty()
						&& attribute != null && !attribute.isEmpty()) {
						if (idsource != null && idsource.equals(MAPPING_ID_SOURCE_BODY))
							bodyid= (Character.valueOf(XMLStructureCreator.ID_TYPE_BODY)).toString();
						idmapHM.put(XMLStructureCreator.ROOT_ID	+ XMLStructureCreator.SIGN_SEPARATOR
								+ signature	+ XMLStructureCreator.SIGN_SEPARATOR, bodyid + attribute);
					}
				}
				//handles ordered entries
				IConfigurationElement[] orderedEntries= idmap.getChildren(ORDERED_ELEMENT_NAME);
				if (orderedEntries.length > 0) {
					ArrayList orderedAL= new ArrayList();
					for (IConfigurationElement ordered : orderedEntries) {
						//add SIGN_SEPARATOR at the end because not contained in signatures of plugin.xml
						//also add prefix at beginning
						String signature= ordered.getAttribute(ORDERED_SIGNATURE_ATTRIBUTE);
						if (signature != null && !signature.isEmpty())
							orderedAL.add(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + signature + XMLStructureCreator.SIGN_SEPARATOR);
					}
					if (orderedAL.size() > 0)
						fOrderedElementsInternal.put(idmap_name, orderedAL);
				}
				//handle EXTENSION_NAME_ATTRIBUTE
				String ext_name= idmap.getAttribute(EXTENSION_NAME_ATTRIBUTE);
				if (ext_name != null && !fIdExtensionToName.containsKey(ext_name)) {
					ext_name= ext_name.toLowerCase();
					fIdExtensionToName.put(ext_name,idmap_name);
					CompareUI.addStructureViewerAlias(DEFAULT_PREFIX, ext_name);
				}				
			}
		}
	}

	public ListenerList getViewers() {
		return fViewers;
	}

	public static Shell getActiveWorkbenchShell() {
		IWorkbenchWindow window= getActiveWorkbenchWindow();
		if (window != null)
			return window.getShell();
		return null;
	}
	
	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		IWorkbenchWindow window= PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window == null) {
			final WindowRef windowRef= new WindowRef();
			Display.getDefault().syncExec(() -> setActiveWorkbenchWindow(windowRef));
			return windowRef.window;
		}
		return window;
	}

	private static class WindowRef {
		public IWorkbenchWindow window;
	}

	private static void setActiveWorkbenchWindow(WindowRef windowRef) {
		windowRef.window= null;
		Display display= Display.getCurrent();
		if (display == null)
			return;
		Control shell= display.getActiveShell();
		while (shell != null) {
			Object data= shell.getData();
			if (data instanceof IWorkbenchWindow) {
				windowRef.window= (IWorkbenchWindow)data;
				return;
			}
			shell= shell.getParent();
		}
		Shell shells[]= display.getShells();
		for (Shell s : shells) {
			Object data = s.getData();
			if (data instanceof IWorkbenchWindow) {
				windowRef.window= (IWorkbenchWindow)data;
				return;
			}
		}
	}	
	
	public static void log(Throwable e) {
		log(new Status(IStatus.ERROR, getPluginId(), IStatus.ERROR, "Internal Error", e)); //$NON-NLS-1$
	}
	
	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}
	
	public static String getPluginId() {
		return getDefault().getBundle().getSymbolicName();
	}
}
