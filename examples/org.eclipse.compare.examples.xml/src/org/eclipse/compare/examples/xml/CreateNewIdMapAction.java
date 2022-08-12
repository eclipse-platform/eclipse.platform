/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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

import java.util.*;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.Window;

/**
 * Button to create a new id mapping scheme
 */
public class CreateNewIdMapAction extends Action {

	private HashMap fIdMaps;// HashMap ( idname -> HashMap (signature -> id) )
	private HashMap fIdMapsInternal;
	private HashMap fIdExtensionToName;
	
	public CreateNewIdMapAction(XMLStructureViewer viewer) {		
		setImageDescriptor(XMLPlugin.getDefault().getImageDescriptor("obj16/addidmap.gif")); //$NON-NLS-1$
		setToolTipText(XMLCompareMessages.XMLStructureViewer_newtask); 
	}
	
	@Override
	public void run() {
		XMLPlugin plugin= XMLPlugin.getDefault();
		fIdMapsInternal= plugin.getIdMapsInternal();//fIdMapsInternal is only read, not modified
		
		fIdMaps = new HashMap();
		HashMap PluginIdMaps = plugin.getIdMaps();
		Set keySet = PluginIdMaps.keySet();
		for (Iterator iter = keySet.iterator(); iter.hasNext(); ) {
			String key = (String) iter.next();
			fIdMaps.put(key, ((HashMap)PluginIdMaps.get(key)).clone());
		}
		
		fIdExtensionToName= new HashMap();
		HashMap PluginIdExtensionToName= plugin.getIdExtensionToName();
		keySet= PluginIdExtensionToName.keySet();
		for (Iterator iter= keySet.iterator(); iter.hasNext(); ) {
			String key= (String) iter.next();
			fIdExtensionToName.put(key, PluginIdExtensionToName.get(key));
		}
		
		IdMap idmap = new IdMap(false);
		XMLCompareAddIdMapDialog dialog= new XMLCompareAddIdMapDialog(XMLPlugin.getActiveWorkbenchShell(),idmap,fIdMaps,fIdMapsInternal,fIdExtensionToName,false);
		if (dialog.open() == Window.OK) {
			if (!fIdMaps.containsKey(idmap.getName())) {
				fIdMaps.put(idmap.getName(),new HashMap());
				if (!idmap.getExtension().isEmpty())
					fIdExtensionToName.put(idmap.getExtension(),idmap.getName());
				XMLPlugin.getDefault().setIdMaps(fIdMaps,fIdExtensionToName,null,false);
			}
		}
	}
}
