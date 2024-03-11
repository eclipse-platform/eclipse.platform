package org.eclipse.core.pki.auth;

import java.io.Console;

public enum PokeInConsole {
	PASSWD;
	protected static final String ENTER="Enter password :";
	public void get() {
		try {
			Console console = System.console();
			if (console == null) {
	            System.out.println("PokeInConsole - Couldn't get Console instance");
	            System.exit(0);
	        }
			char[] ch = console.readPassword( ENTER );
			String pw = new String(ch);
			System.out.println("PokeInConsole - PASSWD:"+ pw);
			System.setProperty("javax.net.ssl.keyStorePassword", pw); 
		    
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
}
