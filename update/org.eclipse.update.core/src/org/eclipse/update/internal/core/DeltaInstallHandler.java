/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.update.internal.core;

import org.eclipse.core.runtime.IStatus;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.update.core.BaseInstallHandler;
import org.eclipse.update.core.ContentReference;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.core.IFeatureContentConsumer;
import org.eclipse.update.core.IPluginEntry;
import org.eclipse.update.core.PluginEntry;
import org.eclipse.update.core.Site;
import org.eclipse.update.internal.operations.UpdateUtils;

/**
 * Install handler for partial plugin delivery: copy delta content from old plugin into the new one.
 * The new plugin should only contain files that have changed.
 */
public class DeltaInstallHandler extends BaseInstallHandler {
	private final static String PLUGIN_XML = "plugin.xml"; //$NON-NLS-1$
	private final static String FRAGMENT_XML = "fragment.xml"; //$NON-NLS-1$
	private final static String META_MANIFEST = "META-INF/MANIFEST.MF"; //$NON-NLS-1$

	protected IFeature oldFeature;

	/* (non-Javadoc)
	 * @see org.eclipse.update.core.IInstallHandler#completeInstall(org.eclipse.update.core.IFeatureContentConsumer)
	 */
	public void completeInstall(IFeatureContentConsumer consumer)
		throws CoreException {
		try {
			if (pluginEntries == null)
				return;

			if (!feature.isPatch()) {
				// Get the old feature
				IFeature[] oldFeatures = UpdateUtils
						.getInstalledFeatures(feature);
				if (oldFeatures.length == 0)
					return;
				oldFeature = oldFeatures[0];
			} else {
				oldFeature = UpdateUtils.getPatchedFeature(feature);
				if (oldFeature == null) {
					return;
				}
			}
			
			IPluginEntry[] oldPlugins = oldFeature.getPluginEntries();
			for (int i = 0; i < pluginEntries.length; i++) {
				IPluginEntry newPlugin = pluginEntries[i];
				IPluginEntry oldPlugin =
					getPluginEntry(
						oldPlugins,
						newPlugin.getVersionedIdentifier().getIdentifier());
				if (oldPlugin == null)
					continue;
				try {
					overlayPlugin(oldPlugin, newPlugin, consumer);
				} catch (IOException e) {
					throw new CoreException(
						new Status(
							IStatus.ERROR,
							UpdateUtils.getPluginId(),
							1,
							"", //$NON-NLS-1$
							e));
				}
			}
		} finally {
			//if (contentConsumer != null)
			//	contentConsumer.close();
		}
	}

	protected IPluginEntry getPluginEntry(IPluginEntry[] plugins, String id) {
		for (int i = 0; i < plugins.length; i++)
			if (plugins[i].getVersionedIdentifier().getIdentifier().equals(id))
				return plugins[i];
		return null;
	}

	protected boolean referenceExists(
		ContentReference[] references,
		ContentReference ref) {
		String id = ref.getIdentifier();
		if (id == null)
			return false;

		for (int i = 0; i < references.length; i++)
			if (id.equals(references[i].getIdentifier()))
				return true;
		return false;
	}

	protected void overlayPlugin(
		IPluginEntry oldPlugin,
		IPluginEntry newPlugin,
		IFeatureContentConsumer consumer)
		throws CoreException, IOException {
		if(newPlugin instanceof PluginEntry && !((PluginEntry)newPlugin).isUnpack()){
			// partial plug-ins (in patches) must always be unpacked
			return;
		}
		
		// copy the content of the old plugin over the new one, but only
		// those files that do not exist on the target
		ContentReference[] oldReferences =
			oldFeature
				.getFeatureContentProvider()
				.getPluginEntryContentReferences(
				oldPlugin,
				null);
		ContentReference[] newReferences =
			feature
				.getFeatureContentProvider()
				.getPluginEntryContentReferences(
				newPlugin,
				null);

		URL newURL = new URL(consumer.getFeature().getSite().getURL(),
				Site.DEFAULT_PLUGIN_PATH
						+ newPlugin.getVersionedIdentifier().toString());
		String pluginPath = newURL.getFile();
		for (int i = 0; i < oldReferences.length; i++) {
			if (isPluginManifest(oldReferences[i])
				|| referenceExists(newReferences, oldReferences[i]))
				continue;

			InputStream input = null;
			try {
				input = oldReferences[i].getInputStream();
				File targetFile =
					new File(pluginPath, oldReferences[i].getIdentifier());
				UpdateManagerUtils.copyToLocal(
					input,
					targetFile.getAbsolutePath(),
					null);
				UpdateManagerUtils.checkPermissions(
					oldReferences[i],
					pluginPath);
				// 20305
			} catch (IOException e) {
				continue;
			} finally {
				if(input != null){
					try{
						input.close();
					} catch (IOException ioe) {
					}
				}
			}
		}
	}

	protected boolean isPluginManifest(ContentReference ref) {
		String id = ref.getIdentifier();
		return PLUGIN_XML.equals(id) || FRAGMENT_XML.equals(id) || META_MANIFEST.equals(id);
	}
}
