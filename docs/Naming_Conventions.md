Naming Conventions
==================

Contents
--------

*   [1 General](#General)
    *   [1.1 Eclipse Workspace Projects](#Eclipse-Workspace-Projects)
    *   [1.2 Java Packages](#Java-Packages)
    *   [1.3 API Packages](#API-Packages)
    *   [1.4 Internal Implementation Packages](#Internal-Implementation-Packages)
    *   [1.5 Test Suite Packages](#Test-Suite-Packages)
    *   [1.6 Examples Packages](#Examples-Packages)
    *   [1.7 Additional rules](#Additional-rules)
*   [2 Classes and Interfaces](#Classes-and-Interfaces)
*   [3 Methods](#Methods)
*   [4 Variables](#Variables)
*   [5 Constants](#Constants)
*   [6 Plug-ins and Extension Points](#Plug-ins-and-Extension-Points)
*   [7 System Files and Settings](#System-Files-and-Settings)

General
-------

Like other open source projects, the code base for the Eclipse project should avoid using names that reference a particular company or their commercial products.

### Eclipse Workspace Projects

When Eclipse is being used to develop plug-ins for the Eclipse project, the name of the Eclipse workspace project should match the name of the plug-in. 
For example, org.eclipse.core.runtime plug-in is developed in an Eclipse workspace project named org.eclipse.core.runtime.

### Java Packages

The Eclipse Platform consists of a collection of Java packages. 
The package namespace is managed in conformance with Sun's package naming guidelines; subpackages should not be created without prior approval from the owner of the package subtree. 
The packages for the open-source Eclipse project are all subpackages of org.eclipse.

The first package name segment after org.eclipse is generally the project name, followed by the component name.

       org.eclipse.<project>.<component>[.*]- General form of package names
    

The following projects are assigned at the time of writing:

       org.eclipse.equinox.<component>[.*] - Equinox OSGi framework
       org.eclipse.jdt.<component>[.*] - Java development tooling
       org.eclipse.pde.<component>[.*] - Plug-in development environment
    
The following package name segments are reserved:

       internal - indicates an internal implementation package that contains no API
       tests - indicates a non-API package that contains only test suites
       examples - indicates a non-API package that contains only examples
 

These names are used as qualifiers and appear between the project and component name:

       org.eclipse.<project>.internal.<component>[.*] - internal package
       org.eclipse.<project>.tests.<component>[.*] - tests
       org.eclipse.<project>.examples.<component>[.*] - examples
    

In the case of the Eclipse Platform proper, there is no project name, and the qualifiers appear immediately after the component name:
 
       org.eclipse.<component>[.*] -  Eclipse Platform proper
       org.eclipse.<component>.internal[.*] - Eclipse Platform internal package
       org.eclipse.<component>.tests[.*] - Eclipse Platform tests
       org.eclipse.<component>.examples[.*] - Eclipse Platform examples
 

The following components of the Eclipse Platform proper are assigned at the time of writing:
 

       org.eclipse.ant[.*] - Ant support
       org.eclipse.compare[.*] - Compare support
       org.eclipse.core[.*] - Platform core
       org.eclipse.debug[.*] - Debug
       org.eclipse.help[.*] - Help support
       org.eclipse.jdi[.*] - Eclipse implementation of Java Debug Interface (JDI)
       org.eclipse.jface[.*] - JFace
       org.eclipse.platform[.*] - Documentation
       org.eclipse.scripting[.*] - Scripting support
       org.eclipse.sdk[.*] - SDK configuration
       org.eclipse.search[.*] - Search support
       org.eclipse.swt[.*] - Standard Widget Toolkit
       org.eclipse.ui[.*] - Workbench
       org.eclipse.update[.*] - Plug-in live update
       org.eclipse.vcm[.*] - Version and Configuration Management
       org.eclipse.webdav[.*] - WebDAV support
    

For example,

       org.eclipse.jdt.internal.core.compiler - Correct usage
       org.eclipse.jdt.core.internal.compiler - Incorrect. internal should immediately follow project name.
       org.eclipse.core.internal.resources - Correct usage
       org.eclipse.internal.core.resources - Incorrect. internal should never immediately follow org.eclipse.
       org.eclipse.core.resources.internal - Incorrect. internal should immediately follow Eclipse Platform component name.
    
### API Packages

API packages are ones that contain classes and interfaces that must be made available to ISVs. 
The names of API packages need to make sense to the ISV. 
The number of different packages that the ISV needs to remember should be small, since a profusion of API packages can make it difficult for ISVs to know which packages they need to import. 
Within an API package, all public classes and interfaces are considered API. 
The names of API packages should not contain internal, tests, or examples to avoid confusion with the scheme for naming non-API packages. 
Consult [Eclipse/API Central](/Eclipse/API_Central "Eclipse/API Central") for more detailed information on choosing and naming API elements.

### Internal Implementation Packages

All packages that are part of the platform implementation but contain no API that should be exposed to ISVs are considered internal implementation packages. 
All implementation packages should be flagged as internal, with the tag occurring just after the major package name. 
ISVs will be told that all packages marked internal are out of bounds. 
(A simple text search for ".internal." detects suspicious reference in source files; likewise, "/internal/" is suspicious in .class files).

### Test Suite Packages

All packages containing test suites should be flagged as tests, with the tag occurring just after the major package name. 
Fully automated tests are the norm; so, for example, org.eclipse.core.tests.resources would contain automated tests for API in org.eclipse.core.resources. 
Interactive tests (ones requiring a hands-on tester) should be flagged with interactive as the last package name segment; so, for example, org.eclipse.core.tests.resources.interactive would contain the corresponding interactive tests.

### Examples Packages

All packages containing examples that ship to ISVs should be flagged as examples, with the tag occurring just after the major package name. 
For example, org.eclipse.swt.examples would contain examples for how to use the SWT API.

### Additional rules

*   Package names should contain only lowercase ASCII alphanumerics, and avoid underscore _ or dollar sign $ characters.

Classes and Interfaces
----------------------

Sun's naming guidelines states

Class names should be nouns, in mixed case with the first letter of each internal word capitalized. 
Try to keep your class names simple and descriptive. Use whole words - avoid acronyms and abbreviations (unless the abbreviation is much more widely used than the long form, such as URL or HTML).

Examples:

*   class Raster;
*   class ImageSprite;

Interface names should be capitalized like class names.

For interface names, we follow the "I"-for-interface convention: all interface names are prefixed with an "I". 
For example, "IWorkspace" or "IIndex". 
This convention aids code readability by making interface names more readily recognizable.

Additional rules:

The names of exception classes (subclasses of Exception) should follow the common practice of ending in "Exception".

Methods
-------

Sun's naming guidelines states

Methods should be verbs, in mixed case with the first letter lowercase, with the first letter of each internal word capitalized.

  
Examples:

*   run();
*   runFast();
*   getBackground();

  
Additional rules:

The names of methods should follow common practice for naming getters (getX()), setters (setX()), and predicates (isX(), hasX()).

Variables
---------

Sun's naming guidelines states

Except for variables, all instance, class, and class constants are in mixed case with a lowercase first letter. 
Internal words start with capital letters. 
Variable names should not start with underscore _ or dollar sign $ characters, even though both are allowed.

Variable names should be short yet meaningful. 
The choice of a variable name should be mnemonic - that is, designed to indicate to the casual observer the intent of its use. 
One-character variable names should be avoided except for temporary "throwaway" variables. 
Common names for temporary variables are i, j, k, m, and n for integers; c, d, and e for characters.

Examples:

*   int i;
*   char c;
*   float myWidth;

Constants
---------

Sun's naming guidelines states

The names of variables declared class constants and of ANSI constants should be all uppercase with words separated by underscores ("_").

Examples:

*   static final int MIN_WIDTH = 4;
*   static final int MAX_WIDTH = 999;
*   static final int GET\_THE\_CPU = 1;

Plug-ins and Extension Points
-----------------------------

All plug-ins (and plug-in fragments), including the ones that are part of the Eclipse Platform, like the Resources and Workbench plug-ins, must have unique identifiers following the same style of naming convention as Java packages. 
For example, the workbench plug-in is named org.eclipse.ui.

The names of a plug-in and the names of the Java packages declared within the code library of that plug-in commonly align. 
For example, the org.eclipse.ui plug-in declares much of its code in packages named org.eclipse.ui.* . 
While alignment is the recommended practice, it is not an absolute requirement. 
For instance, the org.eclipse.ui plug-in also declares code in packages named org.eclipse.jface.*. 
The org.eclipse.ant.core plug-in declares code in packages named org.eclipse.ant.core and org.apache.tools.ant.*.

The plug-in namespace is managed hierarchically; do not create plug-in without prior approval from the owner of the enclosing namespace.

Extension points that expect multiple extensions should have plural names. For example, "builders" rather than "builder".

System Files and Settings
-------------------------

By convention, files or folders that start with a period ('.') are considered "system files" and should not be edited by users or, directly, by other components that do not "own" them.

Of special note is the ".settings" folder in a workspace project. 
This folder holds various forms of preference or metadata specific to that workspace project. 
Files in this directory do not have to start with a period (they are assumed "system files" as they are in a "system folder") but they must follow the same naming conventions outlined elsewhere in this guide. 
That is, they must identify themselves with their Eclipse Project's namespace (e.g. org.eclipse.jdt, org.eclipse.jst, etc). and they should be as specific as possible to denote the package they come from, or the function they are serving. 
For example,

     org.eclipse.jdt.core.prefs
     org.eclipse.jst.common.project.facet.core.prefs
     org.eclipse.wst.common.project.facet.core.xml
    
Two obvious exceptions to this convention are the .classpath and .project files, but ... that's just because they were the first, before the large community of Eclipse was grasped. 
Following these namespace guidelines will help avoid conflicts where two plugins or projects could accidently pick the same name for a metadata file.

