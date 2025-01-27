package org.eclipse.core.pki.auth;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;

public enum PokeInConsole implements Serializable {
	PASSWD;
	protected static final String ENTER="Enter password:";
	public void get() {
		try {
			Console console = System.console();
			if (console == null) {
				
	            //System.out.println("PokeInConsole - Couldn't get Console instance");
	            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
	            System.out.print(ENTER);
	            String name=new String();
				try {
					//System.out.println((char)27 + "[37mWHITE");
					//System.out.println((char)27 + "[37m");
					System.out.println((char)27 + "[8m");
					name = reader.readLine();
					System.out.flush();
					//System.out.println((char)27 + "[30mBLACK");
					System.out.println((char)27 + "[0m");
					System.out.println((char)27 + "[30m");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	            
	            System.setProperty("javax.net.ssl.keyStorePassword", name);
	        } else {
	        	char[] ch = console.readPassword( ENTER );
	        	String pw = new String(ch);
	        	System.setProperty("javax.net.ssl.keyStorePassword", pw);
	        }
		    
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
}
