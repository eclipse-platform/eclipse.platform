package org.eclipse.core.pki.util;

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
import java.util.Properties;
import java.util.Set;

public enum DotPkiPropertiesRequired {
	CHECKER;
	List<String> list = get();
	public boolean testFile(Path path) {
		Properties properties=new Properties();
		//System.out.println("DotPkiPropertiesRequired testFile:"+path.toString());
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
			    	//System.out.println("DotPkiPropertiesRequired All proeprties exist:"+list.size());
			    	return true;
			    } else {
			    	//System.out.println("DotPkiPropertiesRequired missing preoperties");
			    	LogUtil.logWarning("Missing properies;"+ list.toString());
			    }
			} else {
				LogUtil.logWarning("DotPkiPropertiesRequired- NO PKI file detected");
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
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
<<<<<<< HEAD
				"javax.net.ssl.keyStore",
				//"javax.net.ssl.keyStorePassword", "javax.net.ssl.keyStore");
				"javax.net.ssl.keyStorePassword", "javax.net.ssl.keyStore");

=======
				"javax.net.ssl.keyStorePassword", "javax.net.ssl.keyStore");
>>>>>>> branch 'master' of https://github.com/JavaJoeS/eclipse.platform.git
		List<String> list = new ArrayList<String>(l);
		return list;
	}
}
