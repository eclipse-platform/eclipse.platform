/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/


/**
 * ajaxRequest is used to initiate a request for an xml page
 * @param url the url of the requested page
 * @param callback, a function with a single parameter of type dom
 * @param errorCallback (optional), a parameterless function called if there is an error
 */

    function ajaxRequest(url, callback, errorCallback) {
        var ajax_request= false;

        if (window.XMLHttpRequest) { // Mozilla, Safari, ...
            ajax_request= new XMLHttpRequest();
            if (ajax_request.overrideMimeType) {
                ajax_request.overrideMimeType('text/xml');
            }
        } else if (window.ActiveXObject) { // IE
            try {
                ajax_request= new ActiveXObject("Msxml2.XMLHTTP");
            } catch (e) {
                try {
                    ajax_request= new ActiveXObject("Microsoft.XMLHTTP");
                } catch (e) {}
            }
        }

        if (!ajax_request) {
            // Unable to initiate the request
            errorCallback();
            return;
        }

        ajax_request.onreadystatechange = function() { ajaxStateChange(ajax_request, callback, errorCallback)};
        ajax_request.open('GET', url, true);
        ajax_request.send(null);

    }

    function ajaxStateChange(ajax_request, callback, errorCallback) {
        if (ajax_request.readyState == 4) {
            if (ajax_request.status == 200) {

                if (callback) {
                    callback(ajax_request.responseXML);
                } 
            } else {
                if (errorCallback) {
                    errorCallback;  
                } 
            }
        }

    }
