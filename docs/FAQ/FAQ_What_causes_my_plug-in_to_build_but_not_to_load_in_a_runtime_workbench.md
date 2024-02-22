

FAQ What causes my plug-in to build but not to load in a runtime workbench?
===========================================================================

Runtime problems are usually indicated by a `java.lang.NoClassDefFoundError` when the plug-in is started.

To troubleshoot runtime issues, it is helpful to understand a little bit more about Eclipse classpaths. Eclipse distinguishes between a compile-time classpath and a runtime classpath. The compile-time classpath provides the Eclipse development workbench with all necessary information for compiling a plug-in and is declared in the plug-in project's `.classpath` file. The runtime classpath, on the other hand, tells OSGi and the Eclipse runtime workbench which plug-ins and JARs are necessary to execute the plug-in. The runtime classpath is determined by the `MANIFEST.MF` file. Typically, the compile-time classpath includes most runtime dependencies, and the recommended way to incorporate these dependencies also at compile-time is to use the **Plug-in Dependencies** classpath container. This container will be automatically populated with the dependencies listed in the `MANIFEST.MF` file.

`java.lang.NoClassDefFoundError`s are runtime problems, so editing the `.classpath` file will usually not solve these problems. Instead, check these files:

*   the `MANIFEST.MF` file
*   the `build.properties` file
*   the launch configuration that you used to start the plug-in

  
In most cases, the `MANIFEST.MF` is the culprit. The most common problems with the manifest are:

*   missing "`.`" entry in the `Bundle-ClassPath`, especially when additional JARs were added to the plug-in's runtime classpath
*   missing `Export-Package` in the manifest of required plug-ins

  
Next in line of the trouble-makers is the `build.properties` file. Most developers consider the `build.properties` file a compile-time file, because it mainly tells the PDE build how to package a deployable plug-in. However, the `build.properties` file is also used by PDE to create a runtime workbench that (as closely as possible) replicates the behavior that one would see if the packaged plug-in were to be deployed into a primary workbench. This "black magic" behind the scenes may preclude certain JARs or packages from being visible at runtime if the `build.properties` file is incorrect or incomplete. If you are 100% sure that your manifest is absolutely correct then you should check for warning markers in the `build.properties` file.

Finally, if your plug-in (or another plug-in that is required by your plug-in) is not enabled as part of your launch configuration its classes obviously cannot be loaded at runtime. If your launch configuration uses the "plug-ins selected below only" option, make sure that all required plug-ins are indeed selected in the list.

  

See Also:
---------

\[\[FAQ\_How\_do\_I\_add\_an\_extra\_library\_to\_my\_project%26%23146%3Bs_classpath%3F\]\]

  
[FAQ\_My\_runtime\_workbench\_runs,\_but\_my\_plug-in\_does\_not\_show._Why?](./FAQ_My_runtime_workbench_runs_but_my_plug-in_does_not_show.md "FAQ My runtime workbench runs, but my plug-in does not show. Why?")

  
\[\[FAQ\_When\_does\_PDE\_change\_a\_plug-in%26%23146%3Bs\_Java\_build_path%3F\]\]

