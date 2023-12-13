/**
 * Copyright (c) 2014 Codetrails GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marcel Bruch - initial API and implementation.
 */
package org.eclipse.core.pki.auth;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.concurrent.Executor;

//import org.apache.commons.httpclient.HttpHost;
//import org.apache.commons.lang3.StringUtils;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Builder;

import org.eclipse.core.internal.net.ProxyManager;
import org.eclipse.core.net.proxy.IProxyData;

@SuppressWarnings("restriction")
public final class Proxies {

    private Proxies() {
        // Not meant to be instantiated
    }

    private static final String DOUBLEBACKSLASH = "\\\\"; //$NON-NLS-1$
    private static final String ENV_USERDOMAIN = "USERDOMAIN"; //$NON-NLS-1$
    private static final String PROP_HTTP_AUTH_NTLM_DOMAIN = "http.auth.ntlm.domain"; //$NON-NLS-1$

    /**
     * Returns the domain of the current machine- if any.
     *
     * @param userName
     *            the user name which may be null. On windows it may contain the domain name as prefix "domain\\username".
     */
    public static Optional<String> getUserDomain(String userName) {

        // check the app's system properties
        String domain = System.getProperty(PROP_HTTP_AUTH_NTLM_DOMAIN);
        if (domain != null) {
            return Optional.of(domain);
        }

        // check the OS environment
        domain = System.getenv(ENV_USERDOMAIN);
        if (domain != null) {
            return Optional.of(domain);
        }

        // test the user's name whether it may contain an information about the domain name
        //if (StringUtils.contains(userName, DOUBLEBACKSLASH)) {
        if (userName.contains(DOUBLEBACKSLASH)) { 	
            //return Optional.of(substringBefore(userName, DOUBLEBACKSLASH));
        	userName.substring(0, userName.indexOf(DOUBLEBACKSLASH)-1);
            return Optional.of(userName.substring(0, userName.indexOf(DOUBLEBACKSLASH)-1));
        }

        // no domain name found
        return Optional.empty();
    }

    /**
     * Returns the host name of this workstation (localhost)
     */
    public static Optional<String> getWorkstation() {
        try {
            return Optional.of(InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            return Optional.empty();
        }
    }

    /**
     * Returns the user name without a (potential) domain prefix
     *
     * @param userName
     *            a String that may look like "domain\\userName"
     */
    public static Optional<String> getUserName(String userName) {
        if (userName == null) {
            return Optional.empty();
        }
        //return contains(userName, DOUBLEBACKSLASH) ? of(substringAfterLast(userName, DOUBLEBACKSLASH)) : of(userName);
        return  userName.contains(DOUBLEBACKSLASH) ? 
        		Optional.of(userName.substring(userName.indexOf(DOUBLEBACKSLASH))) : Optional.of(userName);
    }
    //public static Optional<HttpHost> getProxyHost(URI target) {
    public static Optional<HttpClient> getProxyHost(URI target) {
        //IProxyData proxy = getProxyData(target).orNull();
        IProxyData proxy = getProxyData(target).orElse(null);
        if (proxy == null) {
            return Optional.empty();
        }
        //return Optional.of(new HttpHost(proxy.getHost(), proxy.getPort()));
        return Optional.of(HttpClient.newBuilder()
        		.proxy(ProxySelector.of(
        				new InetSocketAddress(proxy.getHost(), proxy.getPort()))).build());
    }

    public static Executor proxyAuthentication(Executor executor, URI target) throws IOException {
        IProxyData proxy = getProxyData(target).orElse(null);
        if (proxy != null) {
            //HttpHost proxyHost = new HttpHost(proxy.getHost(), proxy.getPort());
        	HttpClient.newBuilder().proxy(ProxySelector.of(new InetSocketAddress(proxy.getHost(), proxy.getPort())));
            if (proxy.getUserId() != null) {
                String userId = getUserName(proxy.getUserId()).orElse(null);
                String pass = proxy.getPassword();
                String workstation = getWorkstation().orElse(null);
                String domain = getUserDomain(proxy.getUserId()).orElse(null);
                
                System.out.println("Proxies - proxyAuthentication needs to be FIXED");
                //return executor.auth(proxyHost, userId, pass, workstation, domain);
                //return executor.  auth(proxyHost, userId, pass, workstation, domain);
            } else {
                return executor;
            }
        }
        return executor;
    }

    private static Optional<IProxyData> getProxyData(URI target) {
        IProxyData[] proxies = ProxyManager.getProxyManager().select(target);
        Optional op = Optional.of(proxies);
        if (op.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(proxies[0]);
    }

    public static Optional<String> getProxyUser(URI target) {
        IProxyData proxy = getProxyData(target).orElse(null);
        if (proxy == null) {
            return Optional.empty();
        }
        if (proxy.getUserId() == null) {
            return Optional.empty();
        }
        return getUserName(proxy.getUserId());
    }

    public static Optional<String> getProxyPassword(URI target) {
        IProxyData proxy = getProxyData(target).orElse(null);
        if (proxy == null) {
            return Optional.empty();
        }
        if (proxy.getUserId() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(proxy.getPassword());
    }
}
