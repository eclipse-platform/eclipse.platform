package org.eclipse.core.security;


import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.net.ssl.SSLContext;

import org.eclipse.ecf.core.security.SSLContextFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import org.eclipse.ecf.internal.core.ECFPlugin;
import org.eclipse.ecf.internal.ssl.ECFSSLSocketFactory;
import org.eclipse.ecf.core.security.SSLContextFactory;

@Component(immediate=true)
public class ContextFactoryConsumer {

	@Reference
	void bindSSLContextFactory(SSLContextFactory sslContextFactory) {
		System.out.println("Got it "+ sslContextFactory);
		// Now get PKIJoe SSLContext
		try {
			SSLContext sslContext = sslContextFactory.getInstance("TLS", "PKIJoe");
			// # do stuff with sslContext here!
			System.out.println("sslContext="+sslContext);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	void unbindSSLContextFactory(SSLContextFactory sslContextFactory) {
		System.out.println("Ungot it "+ sslContextFactory);
	}
}
