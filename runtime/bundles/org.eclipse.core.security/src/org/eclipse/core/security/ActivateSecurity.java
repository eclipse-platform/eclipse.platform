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
package org.eclipse.core.security;

import org.eclipse.core.runtime.ServiceCaller;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.io.File;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.text.html.parser.Element;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.core.runtime.spi.RegistryStrategy;
import org.eclipse.core.security.incoming.InBoundController;
import org.eclipse.core.security.state.X509SecurityState;
import org.eclipse.core.security.identification.PublishPasswordUpdateIfc;
import org.eclipse.core.security.identification.PublishPasswordUpdate;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import org.osgi.service.component.annotations.Activate; 
import org.osgi.service.component.annotations.Component;

//import org.eclipse.ecf.internal.core.ECFPlugin;
//import org.eclipse.ecf.internal.ssl.ECFSSLSocketFactory;
//import org.eclipse.ecf.core.security.SSLContextFactory;


public class ActivateSecurity implements BundleActivator, IStartup, ServiceTrackerCustomizer<PublishPasswordUpdateIfc,PublishPasswordUpdateIfc> {
	public static final String ID = "org.eclipse.core.security"; //$NON-NLS-1$
	private static ActivateSecurity instance;
	static boolean isPkcs11Installed = false;
	public static boolean isKeyStoreLoaded = false;
	private BundleContext context;
	//protected ECFSSLSocketFactory ecfSSLSocketFactory;
	
	protected SSLContext sslContext;
	
	private ServiceTracker<PublishPasswordUpdateIfc,PublishPasswordUpdateIfc> subscriberServiceTracker;
	private static final ServiceCaller<ILog> logger = new ServiceCaller(ActivateSecurity.class, ILog.class);
	protected static KeyStore keyStore = null;
	
	Optional<KeyStore> keystoreContainer = null;
	private static final int DIGITAL_SIGNATURE = 0;
	private static final int KEY_CERT_SIGN = 5;
	private static final int CRL_SIGN = 6;

	
	public ActivateSecurity() {
		super();
		setInstance(this);
	}

	@Override
	public void start(BundleContext context) throws Exception {
		ActivateSecurity.getInstance().context=context;
		Startup();
	}
	@Override 
	public void earlyStartup() {
		// required by implementation
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		context=null;
		if (subscriberServiceTracker != null) {
			subscriberServiceTracker.close();
			subscriberServiceTracker = null;
		}
	}

	public static ActivateSecurity getInstance() {
		return instance;
	}

	public static void setInstance(ActivateSecurity instance) {
		ActivateSecurity.instance = instance;
	}

	public void log(String message) {
		logger.call(logger -> logger.info(message));
	}

	public BundleContext getContext() {
		return context;
	}
	public void Startup() {
		/*
		 * Initialize preliminary PKCS settings
		 */
		
		X509SecurityState.getInstance().setPKCS11on(false);
		X509SecurityState.getInstance().setPKCS12on(false);
		try {
			ActivateSecurity.getInstance().log("ActivateSecurity Begin Controller process.");
			InBoundController.getInstance().controller();
		} catch(Exception e) {
			ActivateSecurity.getInstance().log("ActivateSecurity could not Run.");
		} 
		
		
		ActivateSecurity.getInstance().log("ActivateSecurity Controller process complete.");
		
		
		// Create and open Service tracker
		this.subscriberServiceTracker = 
				new ServiceTracker<PublishPasswordUpdateIfc,PublishPasswordUpdateIfc>(
						ActivateSecurity.getInstance().context,
						PublishPasswordUpdateIfc.class,this);
		this.subscriberServiceTracker.open();
	}
	
	public void completeSecureContext() {
		//containerContext();
		
		ActivateSecurity.getInstance().log("ActivateSecurity setup SSLContextFactory.");
		//BundleContext ecfContext = ECFPlugin.getDefault().getContext();
		
		//extractServicesInfo( ecfContext.getBundle(), ecfContext);
		
		try {
			//ecfSSLSocketFactory = (ECFSSLSocketFactory) ecfContext.getServiceReference(ECFSSLSocketFactory.class);
			//ActivateSecurity.getInstance().log("ActivateSecurity service access done SSLSocketFactory.");
			//TimeUnit.SECONDS.sleep(10);
			//ecfSSLSocketFactory.getSSLContext("TLS").setDefault( SSLContext.getDefault() );
			
			//SSLContext sctx = ecfSSLSocketFactory.getSSLContext("TLS");
			//ActivateSecurity.getInstance().log("ActivateSecurity Got the context.");
			//sctx.setDefault( SSLContext.getDefault());
			
			//ActivateSecurity.getInstance().log("ActivateSecurity SSLContext for TLS has been set.");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	// Fetching ServiceReferernces exposed by the bundle
   public static void extractServicesInfo(Bundle bundle, BundleContext bundleContext) {
       ServiceReference[] registeredServices = bundle.getRegisteredServices();
       if (registeredServices != null) {
           for (ServiceReference registeredService : bundle.getRegisteredServices()) {
               // Fetching any property of the Service
               ActivateSecurity.getInstance().log("service.pid: " + registeredService.getProperty("service.pid"));
 
               // Fetch Service from ServiceReference
               ActivateSecurity.getInstance().log("Service: " + bundleContext.getService(registeredService));
           }
       }
   }

	public SSLContext getSSLContext() {
		return sslContext;
	}

	public void setSSLContext(SSLContext context) {
		this.sslContext = context;
	}

	public boolean isKeyStoreLoaded() {
		return isKeyStoreLoaded;
	}

	public void setKeyStoreLoaded(boolean isKeyStoreLoaded) {
		ActivateSecurity.isKeyStoreLoaded = isKeyStoreLoaded;
	}

	private static boolean isDigitalSignature(boolean[] ba) {
		if (ba != null) {
			return ba[DIGITAL_SIGNATURE] && !ba[KEY_CERT_SIGN] && !ba[CRL_SIGN];
		} else {
			return false;
		}
	}
	/**
	 * NOTE:  The method will be called when the Service is discovered.
	 */
	public PublishPasswordUpdateIfc addingService(
			ServiceReference<PublishPasswordUpdateIfc> reference) {
		// XXX Here is where the ITimeService is received, when discovered.
		System.out.println("ITimeServicePublishPasswordUpdateIfc discovered!");
		System.out.println("Service Reference="+reference);
		// Get the time service proxy
		PublishPasswordUpdateIfc subscriberService = this.context.getService(reference);
		System.out.println("Calling Service="+subscriberService);
		// Call the service!
		//Long time = timeService.getCurrentTime();
		// Print out the result
		//System.out.println("Call Done.  Current time given by ITimeService.getCurrentTime() is: "+time);
		return subscriberService;
	}
	public void modifiedService(ServiceReference<PublishPasswordUpdateIfc> reference,
			PublishPasswordUpdateIfc service) {
		// do nothing
	}
	public void removedService(ServiceReference<PublishPasswordUpdateIfc> reference,
			PublishPasswordUpdateIfc service) {
		System.out.println("SubscriberService undiscovered!");
	}
}