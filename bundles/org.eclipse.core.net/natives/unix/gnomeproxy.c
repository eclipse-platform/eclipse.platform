/*
 * Copyright 2008, 2018 Oakland Software Incorporated and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Oakland Software Incorporated - initial API and implementation
 *     IBM Corporation - enabling JNI calls for gconfInit method (bug 232495)
 *     IBM Corporation - gnomeproxy cannot be built with latest versions of glib (bug 385047)
 *     Red Hat - GSettings implementation and code clean up (bug 394087)
 */

#include <jni.h>

#include <glib.h>
#include <gio/gio.h>

#ifdef __linux__
#include <string.h>
#else
#include <strings.h>
#endif

static GSettings *proxySettings = NULL;
static GSettings *httpProxySettings = NULL;
static GSettings *httpsProxySettings = NULL;
static GSettings *socksProxySettings = NULL;
static GSettings *ftpProxySettings = NULL;

static jclass proxyInfoClass;
static jclass stringClass;
static jmethodID proxyInfoConstructor;
static jmethodID toString;

static jmethodID hostMethod;
static jmethodID portMethod;
static jmethodID userMethod;
static jmethodID passwordMethod;

#define CHECK_NULL(X) { if ((X) == NULL) fprintf (stderr,"JNI error at line %d\n", __LINE__); }

/*
 * Class:     org_eclipse_core_internal_net_proxy_unix_UnixProxyProvider
 * Method:    gsettingsInit
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_eclipse_core_internal_net_proxy_unix_UnixProxyProvider_gsettingsInit(
		JNIEnv *env, jclass clazz) {

	proxySettings = g_settings_new ("org.gnome.system.proxy");
	httpProxySettings = g_settings_new ("org.gnome.system.proxy.http");
	httpsProxySettings = g_settings_new ("org.gnome.system.proxy.https");
	socksProxySettings = g_settings_new ("org.gnome.system.proxy.socks");
	ftpProxySettings = g_settings_new ("org.gnome.system.proxy.ftp");
	jclass cls= NULL;
	CHECK_NULL(cls = (*env)->FindClass(env, "org/eclipse/core/internal/net/ProxyData"));
	proxyInfoClass = (*env)->NewGlobalRef(env, cls);

	CHECK_NULL(cls = (*env)->FindClass(env, "java/lang/String"));
	stringClass = (*env)->NewGlobalRef(env, cls);

	CHECK_NULL(proxyInfoConstructor = (*env)->GetMethodID(env, proxyInfoClass, "<init>", "(Ljava/lang/String;)V"));

	CHECK_NULL(toString = (*env)->GetMethodID(env, proxyInfoClass, "toString", "()Ljava/lang/String;"));

	CHECK_NULL(hostMethod = (*env)->GetMethodID(env, proxyInfoClass, "setHost",
					"(Ljava/lang/String;)V"));
	CHECK_NULL(portMethod = (*env)->GetMethodID(env, proxyInfoClass, "setPort",
					"(I)V"));
	CHECK_NULL(userMethod = (*env)->GetMethodID(env, proxyInfoClass, "setUserid",
					"(Ljava/lang/String;)V"));
	CHECK_NULL(passwordMethod = (*env)->GetMethodID(env, proxyInfoClass, "setPassword",
					"(Ljava/lang/String;)V"));
}

/*
 * Class:     org_eclipse_core_internal_net_UnixProxyProvider
 * Method:    getGSettingsProxyInfo
 * Signature: ([Ljava/lang/String);
 */
JNIEXPORT jobject JNICALL Java_org_eclipse_core_internal_net_proxy_unix_UnixProxyProvider_getGSettingsProxyInfo(
		JNIEnv *env, jclass clazz, jstring protocol) {

	jboolean isCopy;
	const char *cprotocol;

	jobject proxyInfo= NULL;

	if (proxySettings == NULL) {
		Java_org_eclipse_core_internal_net_proxy_unix_UnixProxyProvider_gsettingsInit(env, clazz);
	}

	CHECK_NULL(proxyInfo = (*env)->NewObject(env, proxyInfoClass, proxyInfoConstructor, protocol));

	cprotocol = (*env)->GetStringUTFChars(env, protocol, &isCopy);
	if (cprotocol == NULL)
		return NULL;

	gboolean useSame = g_settings_get_boolean(proxySettings,
				"use-same-proxy");

	if (strcasecmp(cprotocol, "http") == 0 || useSame) {
		gboolean useProxy = g_settings_get_boolean(httpProxySettings,
				"enabled");
		if (!useProxy) {
			proxyInfo = NULL;
			goto exit;
		}

		gchar *host = g_settings_get_string(httpProxySettings,
				"host");
		jobject jhost = (*env)->NewStringUTF(env, host);
		(*env)->CallVoidMethod(env, proxyInfo, hostMethod, jhost);
		g_free(host);

		gint port = g_settings_get_int(httpProxySettings, "port");
		(*env)->CallVoidMethod(env, proxyInfo, portMethod, port);

		gboolean reqAuth = g_settings_get_boolean(httpProxySettings,
				"use-authentication");
		if (reqAuth) {
			gchar *user = g_settings_get_string(httpProxySettings,
					"authentication-user");
			jobject juser = (*env)->NewStringUTF(env, user);
			(*env)->CallVoidMethod(env, proxyInfo, userMethod, juser);

			gchar *password = g_settings_get_string(httpProxySettings,
					"authentication-password");
			jobject jpassword = (*env)->NewStringUTF(env, password);
			(*env)->CallVoidMethod(env, proxyInfo, passwordMethod,
					jpassword);
			g_free(user);
			g_free(password);
		}
		goto exit;
	}

	// Everything else applies only if the system proxy mode is manual
	gchar *mode = g_settings_get_string(proxySettings, "mode");
	if (strcasecmp(mode, "manual") != 0) {
		proxyInfo = NULL;
		goto exit;
	}
	g_free(mode);

	gchar *host;
	gint port;
	if (strcasecmp(cprotocol, "https") == 0) {
		host = g_settings_get_string(httpsProxySettings, "host");
		port = g_settings_get_int(httpsProxySettings, "port");
	} else if (strcasecmp(cprotocol, "socks") == 0) {
		host = g_settings_get_string(socksProxySettings, "host");
		port = g_settings_get_int(socksProxySettings, "port");
	} else if (strcasecmp(cprotocol, "ftp") == 0) {
		host = g_settings_get_string(ftpProxySettings, "host");
		port = g_settings_get_int(ftpProxySettings, "port");
	} else {
		proxyInfo = NULL;
		goto exit;
	}

	jobject jhost = (*env)->NewStringUTF(env, host);
	(*env)->CallVoidMethod(env, proxyInfo, hostMethod, jhost);
	(*env)->CallVoidMethod(env, proxyInfo, portMethod, port);
	g_free(host);

	exit: if (isCopy == JNI_TRUE)
		(*env)->ReleaseStringUTFChars(env, protocol, cprotocol);
	return proxyInfo;
}

typedef struct {
	jobjectArray npHostArray;
	JNIEnv *env;
	int index;
} ListProcContext;

// user_data is the ListProcContext
void listProc(gpointer data, gpointer user_data) {
	ListProcContext *lpc = user_data;
	jobject jnpHost = (*lpc->env)->NewStringUTF(lpc->env, (char *)data);
	(*lpc->env)->SetObjectArrayElement(lpc->env, lpc->npHostArray,
			lpc->index++, jnpHost);
}

/*
 * Class:     org_eclipse_core_internal_net_UnixProxyProvider
 * Method:    getGSettingsNonProxyHosts
 * Signature: ()[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_org_eclipse_core_internal_net_proxy_unix_UnixProxyProvider_getGSettingsNonProxyHosts(
		JNIEnv *env, jclass clazz) {

	if (proxySettings == NULL) {
		Java_org_eclipse_core_internal_net_proxy_unix_UnixProxyProvider_gsettingsInit(env, clazz);
	}

	gchar **npfHostsArray;
	GSList *npHosts = NULL;
	gint size, i;

	npfHostsArray = g_settings_get_strv(proxySettings, "ignore-hosts");

	for (i = 0; npfHostsArray[i] != NULL; i++) {
		npHosts = g_slist_prepend(npHosts, npfHostsArray[i]);
	}

	npHosts = g_slist_reverse(npHosts);
	size = g_slist_length(npHosts);
	jobjectArray ret = (*env)->NewObjectArray(env, size, stringClass, NULL);

	ListProcContext lpc;
	lpc.env = env;
	lpc.npHostArray = ret;
	lpc.index = 0;

	g_slist_foreach(npHosts, listProc, &lpc);
	g_strfreev(npfHostsArray);
	g_slist_free(npHosts);
	return ret;
}

