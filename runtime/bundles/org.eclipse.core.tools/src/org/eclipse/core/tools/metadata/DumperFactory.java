/*******************************************************************************
 * Copyright (c) 2002, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tools.metadata;

import java.io.File;
import java.util.Properties;
import org.eclipse.core.runtime.*;

/**
 * A dumper factory creates a dumper object given a file name.
 */
public class DumperFactory {

	private static final String TOOLS_EXTENSION_POINT = "org.eclipse.core.tools"; //$NON-NLS-1$
	private static final String ELEM_DUMPER = "dumper"; //$NON-NLS-1$
	private static final String ATTR_FILE_NAME = "file-name"; //$NON-NLS-1$
	private static final String PT_METADATA_DUMPERS = "metadataDumpers"; //$NON-NLS-1$
	/**
	 * The <code>DumperFactory</code> singleton reference.
	 */
	private static DumperFactory ref;

	/**
	 * This dumper factory mappings configuration.
	 */
	private Properties configuration = new Properties();

	/**
	 * Provides access to a DumperFactory instance.
	 *
	 * @return a <code>DumperFactory</code> instance
	 */
	public synchronized static DumperFactory getInstance() {
		// currently we allow only one instance for this class
		if (ref == null)
			ref = new DumperFactory();
		return ref;
	}

	/**
	 * Constructs a dumper factory, reading dumper definitions from the
	 * extension registry. Forbids instantiation from outside this class.
	 */
	private DumperFactory() {
		loadDumpers();
	}

	private void loadDumpers() {
		IExtensionPoint dumpersPoint = Platform.getExtensionRegistry().getExtensionPoint(TOOLS_EXTENSION_POINT,
				PT_METADATA_DUMPERS);
		IConfigurationElement[] dumperDefinitions = dumpersPoint.getConfigurationElements();
		for (IConfigurationElement dumperDefinition : dumperDefinitions)
			if (dumperDefinition.getName().equals(ELEM_DUMPER))
				configuration.put(dumperDefinition.getAttribute(ATTR_FILE_NAME), dumperDefinition);
	}

	/**
	 * Returns an array containing all known file names.
	 *
	 * @return an array containing file names registered in this factory.
	 */
	public String[] getRegisteredFileNames() {
		String[] fileNames = new String[configuration.size()];
		return configuration.keySet().toArray(fileNames);
	}

	/**
	 * Returns an instance of the dumper class registered for the provided file name.
	 * If there is no dumper class registered for the provided file name, raises an
	 * exception.
	 *
	 * @param fileName the file to be dumped's name
	 * @return a <code>IDumper</code> that knows how to read the file
	 * @throws DumpException if there is no dumper class registered for the
	 * provided file name of if we cannot instanciate the dumper class
	 */
	public IDumper getDumper(String fileName) throws DumpException {
		fileName = fileName.substring(fileName.lastIndexOf(File.separator) + 1);

		Object dumper = configuration.get(fileName);

		if (dumper == null) {
			String NO_DUMPER_MSG = "There is no dumper class for <" + fileName + "> files"; //$NON-NLS-1$ //$NON-NLS-2$
			NO_DUMPER_MSG += getListOfDumperClass(configuration);
			throw new DumpException(NO_DUMPER_MSG);
		}
		// legacy-style definition (from the properties file)
		if (dumper instanceof String)
			try {
				return (IDumper) Class.forName((String) dumper).getDeclaredConstructor().newInstance();
			} catch (Exception e) {
				throw new DumpException("Error instantiating dumper named " + dumper + " for <" + fileName + "> file", e); //$NON-NLS-1$ //$NON-NLS-2$
			}
		// dumper defined through extension mechanism
		try {
			return (IDumper) ((IConfigurationElement) dumper).createExecutableExtension("class"); //$NON-NLS-1$
		} catch (CoreException ce) {
			throw new DumpException("Error instantiating dumper for <" + fileName + "> file", ce); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private String getListOfDumperClass(Properties cfg) {
		StringBuilder buf = new StringBuilder("\r\nList of files who have a dumper class");
		for (String element : cfg.stringPropertyNames()) {
			buf.append("\r\n");
			buf.append(element);
		}
		return buf.toString();
	}
}
