/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.update.tests.standalone;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.update.core.SiteManager;
import org.eclipse.update.internal.core.LocalSite;
import org.eclipse.update.standalone.StandaloneUpdateApplication;

import org.eclipse.core.runtime.IPlatformRunnable;

public class TestFeatureEnable extends StandaloneManagerTestCase {
	public static boolean isEnabled;
	
	public TestFeatureEnable(String arg0) {
		super(arg0);
		isEnabled = false;
	}

	public void umSetUp() {

		super.umSetUp();
		
		String featureId = "my.alphabet";
		String version = "1.0.0";

		checkConfiguredSites();
		if (!isEnabled) {
			System.out.println(
				"==============" + this.getClass() + "=============");
			StandaloneUpdateApplication app = new StandaloneUpdateApplication();
			try {
				exitValue = (Integer)app.run(getCommand(
					"enable",
					featureId,
					version,
					null,
					null,
					TARGET_FILE_SITE.getFile()));
			} catch (Exception e) {
				e.printStackTrace();
			}
			isEnabled = true;
			checkConfiguredSites();
		}
	}
	
	// ensure exit without problems
	public void testExitValue() throws Exception {
		System.out.println("exitValue: " + exitValue);
		assertEquals(exitValue, IPlatformRunnable.EXIT_OK);
	}

	public void testFeatureStatus() throws Exception {
	
		File localFile = new File(((LocalSite)SiteManager.getLocalSite()).getLocationURL().getFile());
		System.out.println(localFile.getAbsolutePath());
		assertTrue(localFile.exists());
		File configFile = getLatestConfigurationFile(localFile);
		
		String[] names =
			{
				"features/my.alphabet.round.letters_1.0.0/",
				"features/my.alphabet.straight.letters_1.0.0/",
				"features/my.alphabet_1.0.0/" };
		assertTrue(configFile.exists());
		assertTrue(checkFeaturesEnabled(getArrayList(names),configFile, TARGET_FILE_SITE.getFile()));
	}

	public boolean checkFeaturesEnabled(ArrayList featureNames, File configFile, String siteLocation) {
		BufferedReader breader = null;
		try {
			breader = new BufferedReader(new FileReader(configFile));
			String line;
			System.out.println("now reading..." + configFile.getAbsolutePath());
			boolean isSiteToCheck = false;
			while (breader.ready()) {
				line = breader.readLine();
				if (line.trim().startsWith("<site url")) {
					isSiteToCheck = line.trim().split("\"")[1].replaceFirst("file:", "").equals(siteLocation);
				} else if (isSiteToCheck && line.trim().startsWith("<feature configured")) {
					System.err.println(line);
					String[] configLine = line.split("\"");
					if (featureNames.contains(configLine[3]) && configLine[1].equals("false")) {
						fail(configLine[3] + " has not been enabled.");
					}
					// temp - delete later
					if (featureNames.contains(configLine[3]))
						System.err.println(line);
					// end of temp delete
				}
			}
		} catch (Exception e) {
			System.err.println(e);
		} finally {
			if (breader != null) {
				try {
					breader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return true;
	}

}
