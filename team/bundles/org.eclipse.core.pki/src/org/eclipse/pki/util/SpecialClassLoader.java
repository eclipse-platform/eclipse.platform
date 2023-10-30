package org.eclipse.pki.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SpecialClassLoader extends ClassLoader {
	
	private String findThisClassAt = null;
		
	public SpecialClassLoader(String classlocation){
		findThisClassAt = classlocation;
	}
	
	protected Class<?> findClass(String name) throws ClassNotFoundException{
		try{
			byte[] classBytes = null;
			classBytes = loadClassbytes(name);
			Class<?> cl = defineClass(name, classBytes, 0, classBytes.length);
			if (cl == null) throw new ClassNotFoundException(name);
			return cl;
		} catch (IOException e){
			throw new ClassNotFoundException(name);
		}
	}

	private byte[] loadClassbytes(String name) throws IOException {
		
		return Files.readAllBytes(Paths.get(findThisClassAt));
	}

	public String getFindThisClassAtLocation() {
		return findThisClassAt;
	}

}
