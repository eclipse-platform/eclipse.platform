Version Numbering
=================

These guidelines have been [revised](/Version_Numbering_Europa_Update "Version Numbering Europa Update") in 2006 for the [Europa Simultaneous Release](/Europa_Simultaneous_Release "Europa Simultaneous Release"), and [revised](/Version_Numbering_Galileo_Update "Version Numbering Galileo Update") again in 2009 for the [Galileo Simultaneous Release](/Galileo_Simultaneous_Release "Galileo Simultaneous Release").

Contents
--------

*   [1 Guidelines on versioning plug-ins](#Guidelines-on-versioning-plug-ins)
    *   [1.1 When to change the major segment](#When-to-change-the-major-segment)
    *   [1.2 When to change the minor segment](#When-to-change-the-minor-segment)
    *   [1.3 When to change the service segment](#When-to-change-the-service-segment)
    *   [1.4 Overall example](#Overall-example)
    *   [1.5 When to change the qualifier segment](#When-to-change-the-qualifier-segment)
    *   [1.6 Plug-ins with no API](#Plug-ins-with-no-API)
    *   [1.7 Versioning plug-ins that wrap external libraries](#Versioning-plug-ins-that-wrap-external-libraries)
*   [2 How to specify plug-in requirements](#How-to-specify-plug-in-requirements)
    *   [2.1 How to specify versions when plug-ins re-export other plug-ins](#How-to-specify-versions-when-plug-ins-re-export-other-plug-ins)
    *   [2.2 How to version packages](#How-to-version-packages)
    *   [2.3 Which version to use in Javadoc tags](#Which-version-to-use-in-Javadoc-tags)
*   [3 Versioning features](#Versioning-features)
    *   [3.1 To require features or to require bundles](#To-require-features-or-to-require-bundles)
        *   [3.1.1 Require bundles](#Require-bundles)
        *   [3.1.2 Require features](#Require-features)
    *   [3.2 Feature includes](#Feature-includes)
    *   [3.3 Patch features](#Patch-features)
*   [4 API Baseline in API Tools](#API-Baseline-in-API-Tools)
*   [5 pom.xml Versions](#pom.xml-Versions)
*   [6 Further reading](#Further-reading)

Guidelines on versioning plug-ins
---------------------------------

This document contains a set of guidelines expressing how to evolve plug-in version numbers in a way that captures the nature of the changes that have been made.

Reminder: In Eclipse, version numbers are composed of four (4) segments: 3 integers and a string respectively named major.minor.service.qualifier.

Each segment captures a different intent:

*   the major segment indicates breakage in the API
*   the minor segment indicates "externally visible" changes
*   the service segment indicates bug fixes and the change of development stream (the semantics attached to development stream is new to this proposal, see below)
*   the qualifier segment indicates a particular build

### When to change the major segment

The major segment number must be increased when a plug-in makes breaking changes to its API. When the major segment is changed the minor and service segments are reset to 0. See [Evolving Java-based APIs](/Evolving_Java-based_APIs "Evolving Java-based APIs") for details on what constitutes a breaking change.

**Example**: From the version 2.2.7, an incompatible change would lead to 3.0.0. By definition, such changes should not be made when working in a maintenance stream.

### When to change the minor segment

The minor segment number must be incremented when a plug-in changes in an "externally visible" way. Examples of externally visible changes include [binary compatible API changes](/Evolving_Java-based_APIs_2 "Evolving Java-based APIs 2"), an updated [BREE](/BREE "BREE") (Bundle-RequiredExecutionEnvironment), significant performance changes, major code rework, adding a new extension point, changing files with a somewhat unclear API status (e.g. changing icons from gif to png), etc. Another way to know when this version number should be changed is by exclusion: it should indicate changes that are neither bug fixes (indicated by the service segment) nor breaking API changes (indicated by the major segment). When the minor segment is changed, the service segment is reset to 0.

**Example**: From the version 2.2.7, a minor change would lead to 2.3.0.

API changes in a maintenance branch are not recommended. If they happen, the minor version will not be increased. The usual service segment increase also applies to this case. The PDE API Tools errors have to be suppressed.

### When to change the service segment

The service segment number must be incremented whenever there have been changes to a plug-in between releases that are not visible in its API. For example, a bug has been fixed in the code, the plug-in manifest has changed, documentation has changed, compiler settings have changed. In general, if that change happens in a service (a.k.a. maintenance) release, then 1 is added. If it happens for the next official release, 100 has to be added. As a result, the service segment number for official releases normally ends with a zero (0, 100, 200, etc.). If that is not true for whatever reason, then one must not add 100 but instead set the service segment number to the next number that is divisible by 100, so that the normal numbering scheme is restored. This practice makes it easy to manage one line of descent after a release and still guarantee that plug-ins coming in the next release will have a higher version number than ones from maintenance releases (thus enabling the usage of update manager from maintenance releases to the new releases).

**Example**: At the end of the development stream N, the version of the plug-in P is 2.4.0. When P makes its first change in the development stream N+1, then the version should be changed to 2.4.100. If P version 2.4.0 needs to receive a bug fix in the maintenance stream started from N, then its version number will be 2.4.1.

### Overall example

This example shows how the version of a plug-in reacts to changes (indicated in parenthesis) in the context of different development stream. Both the text and the diagram illustrate the same example.

    First development stream
     - 1.0.0
    
    Second development stream
     - 1.0.100 (indicates a bug fix)
     - 1.1.0 (a new API has been introduced)
     The plug-in ships as 1.1.0
    
    Third development stream
     - 1.1.100 (indicates a bug fix)
     - 2.0.0 (indicates a breaking change)
     The plug-in ships as 2.0.0
    
    Maintenance stream after 1.1.0
     - 1.1.1
     The plug-in ships as 1.1.1
    
    

 

[![Plugin-versioning-fig1.jpg](/images/Plugin-versioning-fig1.jpg)](/File:Plugin-versioning-fig1.jpg)

### When to change the qualifier segment

Because changing the version number of a plug-in on every commit can be burdensome to the development team, we recommend only applying the previous guidelines once per release cycle. However, since we want to enable the use of the update manager by the development teams, we will use the qualifier segment to indicate changes between builds.

Since Eclipse 3.1, PDE Build can automatically derive the value of the qualifier from the tag associated with the plug-in in the [map file](http://git.eclipse.org/c/platform/eclipse.platform.releng.basebuilder.git/plain/readme.html#createmap) that has been fed as input to the build. This leaves the responsibility to the developer preparing the input for the build to tag their plug-ins with a value that is [lexicographically](http://en.wikipedia.org/wiki/Lexicographical_order) higher than the previous one. To facilitate this, we recommend using the date formatted as vYYYYMMDD (year, month day). If you have multiple builds in a day, you can add "-HHMM" (hour, minute) to ensure it is unique.

It is also recommended that you prefix the tag on a maintenance branch with a unique branch identifier to ensure that builds on that branch can be distinguished from builds on the main development branch. For example, a branch for maintenance of the 1.0 release can use a prefix of "R10x_" so that all builds on that branch for the 1.0.x maintenance releases are grouped together. Note that the "x" in "R10x_" is not intended as a variable to be replaced by the current release number. The prefix should remain consistent throughout all maintenance releases within the same branch, because the same version of a plug-in may appear in multiple maintenance releases. For example a plug-in with version 1.0.1.R10x_v20030629 may appear in the 1.0.1 and 1.0.2 releases of a product.

**Example**: The map file for the plug-in P indicates v20050506, and P's version is 4.2.3. The resulting fully qualified version number would be 4.2.3.v20050506. Deriving the qualifier from the build input offers the advantage that if the plug-in code has not changed, no new version will be created and therefore update manager won't download the plug-in again.

### Plug-ins with no API

There are certain kinds of plug-ins that have no API, and therefore would never evolve more than their service segment according to the above rules. For these plug-ins, the version number can be evolved in sync with another plug-in they are associated with. Note that since these plug-ins do not contain any API, they are generally only explicitly required by plug-ins they are closely associated with anyway.

In particular, a source/test/documentation plug-in that has changes in the current stream should evolve its version number in sync with the plug-in(s) it is providing source/test/documentation for. A fragment with no API should evolve its version number in sync with its host plug-in.

A branding plug-in should keep its version in sync with its feature.

### Versioning plug-ins that wrap external libraries

The version range guidelines above are only effective if the required bundle or feature follows the Eclipse version number evolution guidelines outlined in this document. When specifying a dependency on third party libraries (e.g. those from Orbit), be sure you understand the semantics of that library's version numbers, and specify your version range accordingly. In the absence of any well defined version evolution semantics, you should just specify the version number you require as a lower bound.

**Example**: JFace requires a third party library wrapped in bundle com.xyz.widgets, and is compiled against version 3.8.1 of that bundle. It should specify its dependency as follows: Require-Bundle: com.xyz.widgets;bundle-version="3.8.1"

How to specify plug-in requirements
-----------------------------------

Plug-ins that require other plug-ins must qualify their requirements with a version range since the absence of a version range means that any version can satisfy the dependency. Given that all the changes between the version x.0.0 and the version x+1.0.0 excluded must be compatible (given the previous guidelines); the recommended range includes the minimal required version up-to but not including the next major release.

**Example**: JFace 3.1.0 should probably express the following requirement on SWT: \[3.1.0, 4.0.0).

Also, while setting values for prerequisites, watch for opportunities to widen the set of plug-ins against which a plug-in can work.

**Example**: A plug-in using basic functions from the job API, may express a dependency on runtime 3.0.0 ( \[3.0.0, 4.0.0) ) instead of 3.1.0 ( \[3.1.0, 4.0.0) ).

Also consider [#Versioning plug-ins that wrap external libraries](#Versioning-plug-ins-that-wrap-external-libraries).

**Example**: A plug-in requiring org.junit 3.8.2 should declare: Require-Bundle: org.junit;bundle-version="3.8.2"

### How to specify versions when plug-ins re-export other plug-ins

When a plug-in exports a range of versions for another plug-in, it is promising that some version in that range will be available. Specifically, it provides the guarantee that at least the version specified by the lower bound will be available. Therefore, whenever a plug-in changes the version range of an exported plug-in, it must change its own version number as follows:

*   Any change to upper bound: increase service segment
*   Decrease service segment of lower bound: increase service segment
*   Decrease major or minor segment of lower bound: increase major segment
*   Increase lower bound: increase version by the same magnitude

**Example**: JFace 8.4.2 re-exports SWT \[1.1.1,2.0)

*   If the next version of JFace re-exports SWT \[1.1.1,1.5), JFace version must increase to at least 8.4.3.
*   If the next version of JFace re-exports SWT \[1.1,2.0), JFace version must increase to at least 8.4.3.
*   If the next version of JFace re-exports SWT \[1.0,2.0), JFace version must increase to at least 9.0.0.
*   If the next version of JFace re-exports SWT \[1.1.2,2.0), JFace version must increase to at least 8.4.3.
*   If the next version of JFace re-exports SWT \[1.2,2.0), JFace version must increase to at least 8.5.0.
*   If the next version of JFace re-exports SWT \[2.0,3.0), JFace version must increase to 9.0.0

Exporting a version range that spans multiple major versions of a plug-in is not recommended, because it forces downstream plug-ins to support versions with arbitrary breaking changes between them. Therefore, the most common change to a version range will be increasing the minor or service segment of one of the bounds, or incrementing both bounds up to a range within the next major version.

### How to version packages

Exported packages being used as service APIs must have a version number. The guidelines to evolve those version numbers are the same as for plug-ins. For plug-ins importing individual packages, you should follow the same guidelines as when requiring a plug-in to specify the version range of packages being imported.

### Which version to use in Javadoc tags

In the Javadoc, @since tags are used to indicate the version of a **plug-in** in which a specific API has been added. Because Javadoc describes API, only the first two segment of the plug-in version number should be used. This represents a change from the previous practice where @since indicated the development stream. In addition to using the plug-in version, we recommend to prefix the version number by the plug-in id. This allows tracking of APIs moving from one plug-in to another (this can happen when a plug-in is split into multiple plug-ins but the package names are kept). **Example**: In the 3.2 development stream, the API of the new plug-in org.eclipse.core.filesystem should be tagged as follows:


    /**
     * This class is the main entry point for clients of the Eclipse file system API.  This
     * class has factory methods for obtaining instances of file systems and file
     * stores, and provides constants for option values and error codes.
     * 
     * @noextend This class is not intended to be subclassed by clients.
     * @noinstantiate This class is not intended to be instantiated by clients.
     * @since org.eclipse.core.filesystem 1.0
     */

Versioning features
-------------------

Features are a grouping mechanism that supports reasoning in terms of sets of plug-ins. Therefore, features hide the plug-in boundaries of the plug-ins they contain and act as if their API was the set of all the APIs of all the constituting plug-ins. Because of this, the version of a feature must indicate the most significant type of change between all the plug-ins and features it contains:

*   Increment the feature's major number if any contained plug-in or feature increases their major number
*   Otherwise, increment the feature's minor number if any contained plug-in or feature increases their minor number
*   Otherwise, increment the feature's service number if any contained plug-in or feature increases their service number.

Note that the magnitude of the change does not need to match between the feature and its plug-ins. If a plug-in increments its service number by two for some reason, it does not mean that the feature must also increase the number by two.

A branding plug-in should keep its version in sync with its feature.

### To require features or to require bundles

A feature can express its external dependencies as required features, required plug-ins, or a combination of the two. How dependencies are expressed has consequences on the install-time behavior of your feature, so it is important to understand the different approaches. These approaches are described below along with a discussion of their effect. It is important to note that since [Ganymede](/Ganymede "Ganymede") (Eclipse 3.4), feature dependencies do not have to express dependencies that are already expressed at the plug-in level. Such duplication or further refinement of dependency information between features and plug-ins may unnecessarily restrict the ability to install the feature. With the classic Eclipse Update Manager that was the default install/update technology prior to Eclipse 3.4, dependency information was required at the feature level because the provisioning technology only reasoned at the level of features.

#### Require bundles

If your feature only requires a subset of plug-ins from another feature, you should express your dependencies at the plug-in level. This avoids the brittleness caused by version changes in required features, and allows system integrators to deliver the required plug-ins using different features if desired. Note that feature plug-in dependencies are only needed for plug-ins that are not already required by plug-ins in your feature. In other words, plug-in dependencies at the feature level are for expressing "soft" dependencies on plug-ins that are not strictly required by the plug-ins in your feature, such as documentation.

Expressing dependencies directly at the plug-in level has the benefit of isolating feature authors from changes that do not impact them, thus resulting in greater reusability of the feature.

**Example:**

     Case 1: Assuming the feature org.eclipse.gef is as follows:
        requires feature:
          org.eclipse.platform		3.1.0 match="compatible"
        contains plugins:
          org.eclipse.draw2d		3.1.0
          org.eclipse.gef			3.1.0
     
     Case 2: It is better to express this as:
         contains plugins:
           org.eclipse.draw2d		3.1.0
           org.eclipse.gef			3.1.0  
         requires plugins:
           org.eclipse.core.runtime	3.1.0 match="compatible"
           org.eclipse.ui.views		3.1.0 match="compatible"
           org.eclipse.ui.workbench	3.1.0 match="compatible"
           org.eclipse.jface		3.1.0 match="compatible"
           org.eclipse.swt			3.1.0 match="compatible"
    
    

 

In case 1, if the version of the org.eclipse.platform feature changes to 4.0.0 (because org.eclipse.core.resources changes its major version number), org.eclipse.gef is required to deliver a new version of its features. In case 2, such changes are transparent to the author of GEF.

*   **Note:** The example above is for the purpose of illustration only. In practice the "requires" dependencies mentioned above are already expressed at the GEF bundle level, so they do not need to be repeated at the feature level:
    *   MANIFEST.MF of org.eclipse.draw2d already "requires" org.eclipse.swt
    *   MANIFEST.MF of org.eclipse.gef already "requires" org.eclipse.core.runtime, org.eclipse.ui.views, org.eclipse.ui.workbench, org.eclipse.jface

#### Require features

Use required features when you want another entire feature to be present when your feature is installed. This typically results in a user-level awareness of the required feature, rather than a hidden implementation detail of your feature. For example, users installing Java EE tools from the [Web Tools Platform](http://www.eclipse.org/webtools/) project also require [Java development tools](/JDT "JDT"). This is not just because their plug-ins depend on plug-ins in JDT, but because users of the Java EE tools really expect the full JDT to be there, including documentation, help content, and possibly source. In this case the dependency should be expressed at the feature level to ensure the entire required feature is installed. Feature-level dependencies are also required if you are targeting a platform using the classic Eclipse Update Manager, which operated purely at the level of feature dependencies.

### Feature includes

When a feature includes another feature, it is expressing a tightly bound relation to that other feature. In effect, it is declaring the included feature as a subset of itself. When including another feature, you must always specify in the feature declaration the exact four part version of the feature you are including.

### Patch features

A patch feature is a special kind of feature that updates or replaces some part of an existing feature. A patch feature version should take the same three part identifier as the feature being patched. The qualifier segment is used to distinguish multiple patches to the same feature. As with qualifiers in general, the only hard requirement is that the number increase lexicographically each time a new version is created.

API Baseline in API Tools
-------------------------

The Eclipse [API Tools](/PDE/API_Tools/User_Guide "PDE/API Tools/User Guide") detect some violations of the rules outlined in this document. The API Baseline should always be set to the last released version from the development stream you're working on (or from the N-1 stream, if the current stream has no released version yet).

**Examples**:

*   When developing for 3.6, set the baseline to 3.5, 3.5.1, 3.5.2, as soon as the maintenance releases become available.
*   When developing for 3.5.1, set the baseline to 3.5.
*   When developing for 3.5.2, set the baseline to 3.5.1.
*   When developing for 3.5.2+, set the baseline to 3.5.2.

If you use an older version as API Baseline, you will miss some API problems.

pom.xml Versions
----------------

Since [CBI](/CBI "CBI") builds depend on Maven and neither Maven nor Tycho can consume version numbers from the MANIFEST.MF ([bug 387802](https://bugs.eclipse.org/bugs/show_bug.cgi?id=387802)), every bundle version change also needs to be reflected in the pom.xml. The **Eclipse Releng Tools** can flag missing pom.xml updates. Install them from an [Eclipse Project Updates p2 repository](/Eclipse_Project_Update_Sites "Eclipse Project Update Sites").

Open **Preferences > POM Version Tool** and make sure the severity is set to **Error**. (Starting with the Neon stream, the POM Version Tool is enabled by default).

Further reading
---------------

*   See [Evolving Java-based APIs Part 1](/Evolving_Java-based_APIs "Evolving Java-based APIs")
*   See [Evolving Java-based APIs Part 2](/Evolving_Java-based_APIs_2 "Evolving Java-based APIs 2")
*   See [Evolving Java-based APIs Part 3](/Evolving_Java-based_APIs_3 "Evolving Java-based APIs 3")

