# Deploying the information center as a Web Archive

Using Eclipse 3.4 or later it is possible to configure the help plugins to be deployed as a web archive (war file) which will act as a fully
functioning information center. The instructions below assume a Tomcat server has been installed, but with minor modifications
these steps should work for any full featured server.


* Clone the eclipse.platform.ua repository
  ``git clone https://github.com/eclipse-platform/eclipse.platform.ua/``
* In the Git repository locate the `infocenter-web` directory and underneath that there will be two directories titled `infocenter-app` and
`infocenter-product`.
* Make sure you have the "m2e - Maven Integration for Eclipse" feature installed in your Eclipse IDE.
* Import the `infocenter-web` Maven project using File->Import->Existing Project.
* Add some documentation plugins to the `infocenter-web/infocenter-app/src/main/webapp/WEB-INF/plugins` directory.
* Register the plugins in `infocenter-web/infocenter-app/src/main/webapp/WEB-INF/configuration/org.eclipse.equinox.simpleconfigurator/bundles.info`
* Install required Maven POMs from Eclipse Platform:
   ``git clone -q --depth 1 https://github.com/eclipse-platform/eclipse.platform.releng.aggregator && mvn install -N -f eclipse.platform.releng.aggregator/eclipse-platform-parent && mvn install -N -f eclipse.platform.releng.aggregator/eclipse.platform.releng.prereqs.sdk``
* Execute a Maven build in `infocenter-web`
** Either within Eclipse, right-click on `infocenter-build.launch` and select _Run As -> infocenter-build_
** Or from command-line using the command `mvnw`
* For Tomcat only. In `conf/server.xml` add `URIEncoding="UTF-8"` to the connector element, for example
  ``<Connector port="8080" URIEncoding="UTF-8" etc.>``
  If this step is not performed search will fail if the search term contains non ASCII characters.
* Start Tomcat and see the help system start up. The default URL is <http://localhost:8080/help/>.


Notes: If you look in the `config.ini` in the `help.war` file under directory `help/WEB_INF/configuration` you will notice the
line `eclipse.product=org.eclipse.productname`. If your product has help system customizations in a product plugin you can
activate these by changing this line to point to your product plugin.

## Troubleshooting

### HTTP 404 with Message "BridgeServlet: /help/"

In the web.xml activate the init parameter `enableFrameworkControls`. This enables endpoints to control the embedded OSGi container. Call <http://localhost:8080/help/sp_test>.

You should see the message "`Servlet delegate registered - org.eclipse.equinox.http.servlet.HttpServiceServlet`". You may instead see the message "`Servlet delegate not registered.`". 
This indicates that bundle activator from bundle `org.eclipse.equinox.http.servletbridge` was not started or that it accesses a different instance of class `org.eclipse.equinox.servletbridge.BridgeServlet`.

For all available framework control endpoints refer to `org.eclipse.equinox.servletbridge.BridgeServlet.serviceFrameworkControls(HttpServletRequest, HttpServletResponse)`.
