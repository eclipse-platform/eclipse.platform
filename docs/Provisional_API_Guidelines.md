Eclipse API and Provisional Guidelines
==========================

Contents
--------

*   [1 Overview](#Overview)
*   [2 Before the API freeze](#Before-the-API-freeze)
    *   [2.1 Package naming](#Package-naming)
    *   [2.2 Bundle manifest](#Bundle-manifest)
    *   [2.3 Javadoc](#Javadoc)
*   [3 After the API freeze](#After-the-API-freeze)
    *   [3.1 Package naming](#Package-naming-2)
    *   [3.2 Bundle manifest](#Bundle-manifest-2)
    *   [3.3 Javadoc](#Javadoc-2)
*   [4 Changing provisional APIs](#Changing-provisional-APIs)

Overview
--------

[Eclipse quality](http://www.eclipse.org/projects/dev_process/eclipse-quality.php) APIs don't appear fully formed out of nowhere. 
All APIs undergo a development process, passing through many phases from initial embroyonic forms to real battle-hardened APIs with guarantees of long term support.
It is important that API clients understand the state of the APIs in any given build of Eclipse.
This document sets out API guidelines for the Eclipse Project committers on how to indicate APIs that are still under development and subject to change. 
These guidelines are also useful for API clients who want to know about the state of a given API they are using.

The development cycle for each major and minor Eclipse project release has a release designated as the API freeze. 
The rules for development and treatment of APIs are different for the periods before and after this point, so this document outlines guidelines for both phases of the development cycle.

Definition of terms used in this document:

**API package** 

A package must be exported via the MANIFEST.MF to be considered API.
However, any package that does contain the segment "internal" and which has not set the x-internal or the x-friends directive in the MANIFEST.MF is not API. 
See [Naming Conventions](/Naming_Conventions "Naming Conventions") for details)

**Internal API**

Any Java package containing the segment "internal".

**API element** 

A public Java class or interface in an API package, or a public or protected method or field in such a class or interface

**Provisional API**

An API element that has not existed in any release of the Eclipse project. 
All provisional API is subject to arbitrary change or removal without any notice. 
Although the [Eclipse quality](http://www.eclipse.org/projects/dev_process/eclipse-quality.php) guidelines distinguish between several forms of transient APIs, this document will refer to all non-final APIs simply as provisional APIs. Provisional API has set the x-internal or the x-friends directive for the package in the MANIFEST.MF

Before the API freeze
---------------------

Prior to the API freeze, any API element that did not exist in the previous release is provisional by definition. This is true regardless of the package name, bundle manifest, or javadoc of the types involved. 
However, the conventions below should be used to indicate where API is provisional.

### Package naming

Provisional API should be marked as x-internal or x-friends in the MANIFEST.MF of the plug-in.
It should be kept in a separate package from code that is never intended to become API.
New code that are not intended to become API in time for one of the upcoming releases should be in an internal package.

### Bundle manifest

All API packages should be exported unconditionally by the bundle manifest. 
If internal packages are exported, they should be marked via the x-internal or the x-friends directive in the MANIFEST.MF.

### Javadoc

The primary indicator of provisional API is the @since tag, indicating that it was introduced during the current development period. 
For example, during development leading up to the 3.4 release of Eclipse, a tag of @since 3.4 designates provisional API. 
If the API is particularly volatile, experimental, or at risk of removal, a further comment in the javadoc can be used to clarify the state of the API:

    * <p>
    * <strong>EXPERIMENTAL</strong>. This class or interface has been added as
    * part of a work in progress. There is no guarantee that this API will
    * work or that it will remain the same. Please do not use this API without
    * consulting with the <Your Team Name> team.
    * </p>` 

  

Though not required, inserting this paragraph in your class or interface javadoc makes the state of the API very clear to potential clients. By indicating the name of the responsible team in the comment, you allow for interaction with prospective clients. If a prospective API client contacts the developer of the provisional API, they can agree on a certain level of stability and advanced warning before changes are made in the API. Also note that while you don't need to use the exact template described above, a consistent template does make it easier to find and remove these comments when finalizing the API.

After the API freeze
--------------------

From the perspective of code maintenance, there is really no such thing as "provisional API". 
Either it is complete and committed platform API, or it is internal code. 
API that is new in the current release cycle is still subject to change, but changes after this point are rare and require approval from the Eclipse project [PMC](http://www.eclipse.org/eclipse/team-leaders.html). 

Note that there are no guarantees about the existence or shape of internal code, even if the package name or comments suggest that it may become API in the next release. 
In particular, the API contract (binary upwards compatibility) does not apply. 
Clients who think they must use internal code may do so at their own risk, or with slightly less risk if they can reach an agreement with the team that developed the internal code. 
Note also that in such cases, the required versions for plug-in dependencies need to be specified with great care.

### Package naming

All internal code that is not intended to become API at some point must be in a package whose name contains the segment "internal". 
Internal code that is planned to become API in a future release must be marked as internal via the x-internal or x-friends directive in the MANIFEST.MF.

### Bundle manifest

All API packages should be exported unconditionally by the bundle manifest. 
Internal packages may also exported, they must be marked as x-internal in this case.

### Javadoc

No special javadoc treatment for internal code is needed. Note that @since tags also have little significance for internal code at this point. If internal code is added in the 3.4 development period, but promoted to real API in the 3.5 development period, the correct tag for that API will be @since 3.5. The [experimental](/Provisional_API_Guidelines#experimental "Provisional API Guidelines") javadoc paragraph can be left in the class or interface comment, but is not required.

Changing provisional APIs
-------------------------

Technically, a provisional API can change arbitrarily or be removed at any time without notice. 
Clients in the greater community who are consuming Eclipse milestone and integration builds cannot make any assumptions about the state of any provisional API between any two non-release builds. 
However, committers have a shared responsibility to ensure [Eclipse Project](/Eclipse_Project "Eclipse Project") integration builds are not broken due to changes in provisional APIs. 
Known clients of the provisional API within the SDK should be given fair warning and a chance to react before breaking changes are made. 
As a courtesy to the community, and to minimize the risk of build failures, it is useful to deprecate provisional APIs slated for change or removal in at least one integration build before making the change. Although not required, adding such a temporary tag can ease the transition for early adopters of the API:

    * @deprecated This API will be removed in I20380119. Method {@link #blort()} should be used instead.` 

  

Note that there are no restrictions on changing internal code before or after the API freeze. Since all such code is in internal packages after this point, it can change arbitrarily without notice up to and including the final build of the release.

