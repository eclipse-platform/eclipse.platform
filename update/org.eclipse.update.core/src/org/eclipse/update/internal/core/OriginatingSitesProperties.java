package org.eclipse.update.internal.core;

import java.io.*;
import java.util.Properties;

import org.eclipse.core.runtime.IPath;

/**
 * Properties to manage the sites the feature was installed from.
 * this is different from the UpdateSite of the feature as a feature
 * can be installed from the CDROM drive
 *
 */
public class OriginatingSitesProperties extends Properties {


	private static final String ORIGIN_FILE = "FeatureOrigin.properties";//$NON-NLS-1$
	private IPath path;
	private static OriginatingSitesProperties inst;

	/**
	 * 
	 */
	public static OriginatingSitesProperties getDefault(){
		if (inst==null){
			inst = new OriginatingSitesProperties();
			inst.load(UpdateManagerPlugin.getPlugin().getStateLocation());
		}
		return inst;
	}

	/**
	 * Constructor for OriginatingSitesProperties.
	 */
	private OriginatingSitesProperties() {
		super();
	}

	/**
	 * Constructor for OriginatingSitesProperties.
	 * @param defaults
	 */
	private OriginatingSitesProperties(Properties defaults) {
		super(defaults);
	}

	public void load(IPath location) {
		path = location.append(ORIGIN_FILE);
		File file = new File(path.toOSString());
		if (file.exists()) {
			try {
				FileInputStream fis = new FileInputStream(file);
				super.load(fis);
				fis.close();
			} catch (IOException e) {
			}
		}
	}
	
	public void store() {
		File file = new File(path.toOSString());
		try {
			FileOutputStream fos = new FileOutputStream(file);
			store(fos, "Eclipse: Update Manager:Originating Sites Properties");
			fos.flush();
			fos.close();
		} catch (IOException e) {
		}
	}


}
