/*******************************************************************************
 * Copyright (c) 2025 Eclipse Platform, Security Group and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eclipse Platform - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.security.incoming;

import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.security.ActivateSecurity;

public class DotPkiPropertiesRequired {
	
	private static DotPkiPropertiesRequired INSTANCE;
	List<String> list = get();
	private DotPkiPropertiesRequired() {}
	
	public static DotPkiPropertiesRequired getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new DotPkiPropertiesRequired();
		}
		return INSTANCE;
	}
	
	public boolean testFile(Path path) {
		Properties properties=new Properties();
		try {
			if (Files.exists(path)) {
				final FileChannel channel = FileChannel.open(path, StandardOpenOption.READ);
			    final FileLock lock = channel.lock(0L, Long.MAX_VALUE, true); 
			    properties.load(Channels.newInputStream(channel));
			    Set<Object> keys=properties.keySet();
			    lock.close();
			    for ( Object key: keys ) {
			    	isProperty((String)key);
			    }
			    if ( list.isEmpty()) {
			    	return true;
			    } else {
			    	Optional<Object> pkiType = Optional.ofNullable(properties.get("javax.net.ssl.keyStoreType"));
			    	if ( !(pkiType.isEmpty())) {
			    		if (pkiType.get().toString().contains("12")) { //PKCS12 type. no cfg needed, no provider needed
			    			isProperty("javax.net.ssl.cfgFileLocation");
			    			isProperty("javax.net.ssl.keyStoreProvider");
			    		}
			    	}
			    	if (!(list.isEmpty())) {
			    		ActivateSecurity.getInstance().log("Missing properies;"+ list.toString());// $NON-NLS-1$
			    		return true;
			    	} else {
			    		return true;
			    	}
			    }
			} else {
				ActivateSecurity.getInstance().log("NO PKI config file detected in $HOME/.eclipse");// $NON-NLS-1$
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
			
		return false;
	}
	private void isProperty(String s) {
		if ( list.contains(s)) {
			list.remove(s);
		}
		
	}
	private List<String> get() {
		List<String> l = new LinkedList<String>();
		l = Arrays.asList("javax.net.ssl.trustStore","javax.net.ssl.trustStoreType",
				"javax.net.ssl.trustStorePassword","javax.net.ssl.keyStoreType",
				"javax.net.ssl.keyStoreProvider","javax.net.ssl.cfgFileLocation",
				"javax.net.ssl.keyStore");
		List<String> list = new ArrayList<String>(l);
		return list;
	}
}
