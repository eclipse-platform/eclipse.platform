/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.core;


import java.io.*;
import java.util.jar.*;

import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.core.*;
import org.eclipse.update.core.JarContentReference.*;
import org.eclipse.update.core.model.*;

/**
 * Default Implementation of InstallHandler
 */
public class DefaultInstallHandler extends BaseInstallHandler {

	/*
	 * @see IInstallHandler#nonPluginDataDownloaded(INonPluginEntry[], IVerificationListener)
	 */
	public void nonPluginDataDownloaded(
		INonPluginEntry[] nonPluginData,
		IVerificationListener listener)
		throws CoreException {

		// verify non-plugin archives. The DefaultInstallHandler assumes
		// the verifier associated with the feature is able to verify the
		// data archives.
		if (nonPluginData == null || nonPluginData.length == 0)
			return;

		this.nonPluginEntries = nonPluginData;
		IFeatureContentProvider provider = this.feature.getFeatureContentProvider();
		IVerifier verifier = provider.getVerifier();
		if (verifier == null)
			return;

		for (int i = 0; i < this.nonPluginEntries.length; i++) {
			ContentReference[] archives =
				provider.getNonPluginEntryArchiveReferences(nonPluginEntries[i], this.monitor);
			IVerificationResult result;
			for (int j = 0; j < archives.length; j++) {

				// see if the data entry is a jar
				ContentReference archive = archives[j];
				if (!(archives[j] instanceof JarContentReference)
					&& archives[j].getIdentifier().endsWith(".jar")) { //$NON-NLS-1$
					try {
						archive =
							new JarContentReference(archives[j].getIdentifier(), archives[j].asFile());
					} catch (IOException e) {
					}
				}

				result = verifier.verify(this.feature, archive, false, this.monitor);
				if (result != null)
					promptForVerification(result, listener);
			}
		}
	}

	/*
	 * @see IInstallHandler#completeInstall(IFeatureContentConsumer)
	 */
	public void completeInstall(IFeatureContentConsumer consumer)
		throws CoreException {

		// plugins have been installed. Check to see if we have any
		// non-plugin entries that need to be handled.
		if (this.nonPluginEntries == null || this.nonPluginEntries.length == 0)
			return;

		// install non-plugin archives
		IFeatureContentProvider provider = this.feature.getFeatureContentProvider();
		for (int i = 0; i < this.nonPluginEntries.length; i++) {
			ContentReference[] archive =
				provider.getNonPluginEntryArchiveReferences(nonPluginEntries[i], this.monitor);
			IContentConsumer nonPluginConsumer = consumer.open(nonPluginEntries[i]);
			for (int j = 0; j < archive.length; j++) {
				String id = archive[j].getIdentifier();
				if (id.endsWith(".jar")) { //$NON-NLS-1$
					// the non-plugin archive is a jar. Unpack it into
					// a directory constructed using the archive id
					try {
						final String prefix = id.substring(0, id.length() - 4) + "/"; //$NON-NLS-1$
						JarContentReference jarRef = new JarContentReference("", archive[j].asFile()); //$NON-NLS-1$
						ContentSelector selector = new ContentSelector() {
							public String defineIdentifier(JarEntry entry) {
								if (entry == null)
									return null;
								else
									return prefix + entry.getName();
							}
						};
						ContentReference[] entries = jarRef.peek(selector, this.monitor);
						for (int k = 0; k < entries.length; k++) {
							nonPluginConsumer.store(entries[k], this.monitor);
						}
					} catch (IOException e) {
						throw Utilities
							.newCoreException(NLS.bind(Messages.JarVerificationService_CancelInstall, (new String[] { id })),
						e);
					}

				} else {
					// the non-plugin archive is not a jar. Install it asis.
					nonPluginConsumer.store(archive[j], this.monitor);
				}
			}
			nonPluginConsumer.close();
		}
	}

	/*
	 * 
	 */
	private void promptForVerification(
		IVerificationResult verificationResult,
		IVerificationListener listener)
		throws CoreException {

		if (listener == null)
			return;
		int result = listener.prompt(verificationResult);

		if (result == IVerificationListener.CHOICE_ABORT) {
			Exception e = verificationResult.getVerificationException();
			throw new InstallAbortedException(Messages.JarVerificationService_CancelInstall,e); 
		}
		if (result == IVerificationListener.CHOICE_ERROR) {
			throw Utilities
				.newCoreException(Messages.JarVerificationService_UnsucessfulVerification, 
			verificationResult.getVerificationException());
		}

		return;
	}
}
