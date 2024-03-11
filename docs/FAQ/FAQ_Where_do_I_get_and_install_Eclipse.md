FAQ Where do I get and install Eclipse?
=======================================

The way you download Eclipse depends on how close you want to be to the live stream. Following are the release types that can be downloaded from the Eclipse [download site](https://eclipse.org/downloads), in increasing level of closeness to the CVS HEAD stream:

*   _The latest release_, used by most products in the market. These releases have little risk of surprises, yet those builds can be up to 6 months behind what is being worked on now. Example version numbers for these releases are 2.1.2 and 3.0.

*   _Stable builds_ (so-called milestone builds), which are typically 6 weeks apart and deliver a major collection of stable features. These builds are moderately tested, making them reliable enough for the needs of most developers. An example is Eclipse 3.0M4, which refers to the milestone 4 build of Eclipse 3.0.

*   _Integration builds_, done every week and sometimes more in case of failure or when closer to a release date. These builds integrate contributions from various Eclipse subteams to test the collaborations of various plug-ins. Build names start with the letter I.

*   _Nightly builds_, which provide a view of the contents of the CVS server's HEAD stream at 8pm (Eastern Standard Time) each night. You can compare this build to the result of [musical chairs](http://en.wikipedia.org/wiki/Musical_chairs). The music stops at 8pm, and the build captures what was released at that instant. Mileage accordingly varies. Build names start with the letter N.

Each release or build has a corresponding set of build notes and test results. Be sure to consult these notes before selecting a given build. Automated test suites are run against the nightly and integration builds. Having a build pass the tests can increase the confidence level people have in them. In all cases, the builds are shipped as a compressed archive, and installation is a simple matter of unzipping them anywhere on your local machine.

Alternatively, you may have already installed Eclipse without knowing it. Many commercial products are based on Eclipse, and so while installing these products, you often install a given version of Eclipse. These products usually are not shy about being based on Eclipse, so you can easily discover the location of the eclipse installation by investigating the installation directory of the product. Usually, the directory will contain a sub-directory called eclipse that contains the embedded Eclipse instance. While running your product, you can activate the menu option **Help > About ...** and then click on the Eclipse icon to see what version of the platform is being used.

Installing Eclipse
------------------

To install Eclipse, all you do is unpack the zip/tar file download in the desired directory. No further work is required (other than making sure you have a Java Runtime Environment installed). When you unzip the file, it creates a sub-directory called "eclipse", with multiple sub-directories under that. So, for example, in Windows you could unpack the zip file in the root directory (e.g., C:\\) and Eclipse would be installed in C:\\eclipse. Note that installing Eclipse does not change the Windows registry, and when doing this manually it is best to avoid unzipping into the official "Program Files" directories.

For Windows, a number of problems have been reported when people try to use Windows Explorer to unzip the zip file. Please use a third-party unzip program, such as 7-Zip, Winzip, JustZIPIt, EasyZip, or InfoZIP (part of Cygwin, although you may have to manually chmod the .dll and .exe files as executable afterward).

If you have problems running Eclipse after it's been installed, please see [FAQ I unzipped Eclipse, but it won't start](./FAQ_I_unzipped_Eclipse_but_it_wont_start.md "FAQ I unzipped Eclipse, but it won't start") for troubleshooting help.

See also [Eclipse/Installation](/Eclipse/Installation "Eclipse/Installation").

Installing Java
---------------

Since Eclipse is primarily a Java application, you will need to have Java installed to run Eclipse. Eclipse can run on a number of Java Virtual Machines. The most commonly used Java is from the [Oracle](http://www.java.com/en/). Eclipse officially recommends Java Standard Edition version 8, which is required by the most recent releases from eclipse.org.

Oracle's Java is available in two main distributions: the Java Runtime Environment (JRE) and the Java Development Kit (JDK). If you are using Eclipse for Java development, the JDK offers several advantages, including the availability of source code for the Java classes. If you are using Eclipse for something other than Java development, you can use either the JRE or JDK.

See Also:
---------

*   The Eclipse [download site](https://eclipse.org/downloads)
*   [FAQ I unzipped Eclipse, but it won't start](./FAQ_I_unzipped_Eclipse_but_it_wont_start.md "FAQ I unzipped Eclipse, but it won't start")
*   [Sun Developer Network](http://java.sun.com/)

