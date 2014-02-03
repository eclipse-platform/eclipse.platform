/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/

package org.eclipse.update.internal.jarprocessor;

import java.io.*;
import java.util.Properties;
import java.util.zip.ZipException;
import org.eclipse.update.internal.jarprocessor.Main.Options;

public class JarProcessorExecutor {
	public void runJarProcessor(Options options) {
		if (options.input.isFile() && options.input.getName().endsWith(".zip")) { //$NON-NLS-1$
			ZipProcessor processor = new ZipProcessor();
			processor.setWorkingDirectory(options.outputDir);
			processor.setSignCommand(options.signCommand);
			processor.setPack(options.pack);
			processor.setRepack(options.repack || (options.pack && options.signCommand != null));
			processor.setUnpack(options.unpack);
			processor.setVerbose(options.verbose);
			processor.setProcessAll(options.processAll);
			try {
				processor.processZip(options.input);
			} catch (ZipException e) {
				if (options.verbose)
					e.printStackTrace();
			} catch (IOException e) {
				if (options.verbose)
					e.printStackTrace();
			}
		} else {
			JarProcessor processor = new JarProcessor();
			JarProcessor packProcessor = null;

			processor.setWorkingDirectory(options.outputDir);
			processor.setProcessAll(options.processAll);
			processor.setVerbose(options.verbose);

			//load options file
			Properties properties = new Properties();
			if (options.input.isDirectory()) {
				File packProperties = new File(options.input, "pack.properties");
				if (packProperties.exists() && packProperties.isFile()) {
					InputStream in = null;
					try {
						in = new BufferedInputStream(new FileInputStream(packProperties));
						properties.load(in);
					} catch (IOException e) {
						if (options.verbose)
							e.printStackTrace();
					} finally {
						Utils.close(in);
					}
				}
			}

			if (options.unpack)
				addUnpackStep(processor, properties, options);

			if (options.repack || (options.pack && options.signCommand != null))
				addPackUnpackStep(processor, properties, options);

			if (options.signCommand != null)
				addSignStep(processor, properties, options);

			if (options.pack) {
				packProcessor = new JarProcessor();
				packProcessor.setWorkingDirectory(options.outputDir);
				packProcessor.setProcessAll(options.processAll);
				packProcessor.setVerbose(options.verbose);
				addPackStep(packProcessor, properties, options);
			}

			try {
				process(options.input, options.unpack ? Utils.PACK_GZ_FILTER : Utils.JAR_FILTER, options.verbose, processor, packProcessor);
			} catch (FileNotFoundException e) {
				if (options.verbose)
					e.printStackTrace();
			}
		}
	}

	protected void process(File input, FileFilter filter, boolean verbose, JarProcessor processor, JarProcessor packProcessor) throws FileNotFoundException {
		if (!input.exists())
			throw new FileNotFoundException();

		File[] files = null;
		if (input.isDirectory()) {
			files = input.listFiles();
		} else if (filter.accept(input)) {
			files = new File[] {input};
		}
		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory()) {
				String dir = processor.getWorkingDirectory();
				processor.setWorkingDirectory(dir + "/" + files[i].getName()); //$NON-NLS-1$
				if (packProcessor != null)
					packProcessor.setWorkingDirectory(dir + "/" + files[i].getName());
				process(files[i], filter, verbose, processor, packProcessor);
				processor.setWorkingDirectory(dir);
				if (packProcessor != null)
					packProcessor.setWorkingDirectory(dir);
			} else if (filter.accept(files[i])) {
				try {
					File result = processor.processJar(files[i]);
					if (packProcessor != null && result != null && result.exists()) {
						packProcessor.processJar(result);
					}
				} catch (IOException e) {
					if (verbose)
						e.printStackTrace();
				}
			}
		}
	}

	public void addPackUnpackStep(JarProcessor processor, Properties properties, Options options) {
		processor.addProcessStep(new PackUnpackStep(properties, options.verbose));
	}

	public void addSignStep(JarProcessor processor, Properties properties, Options options) {
		processor.addProcessStep(new SignCommandStep(properties, options.signCommand, options.verbose));
	}

	public void addPackStep(JarProcessor processor, Properties properties, Options options) {
		processor.addProcessStep(new PackStep(properties, options.verbose));
	}

	public void addUnpackStep(JarProcessor processor, Properties properties, Options options) {
		processor.addProcessStep(new UnpackStep(properties, options.verbose));
	}
}
