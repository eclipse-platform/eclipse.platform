Eclipse/API Central/Deprecation Policy
======================================

This page contains [Eclipse Project](/Eclipse_Project "Eclipse Project") guidelines on API deprecation. This page is maintained by the [Eclipse/PMC](/Eclipse/PMC "Eclipse/PMC").

Contents
--------

*   [1 What is Deprecation?](#What-is-Deprecation.3F)
    *   [1.1 Process to deprecate an API](#Process-to-deprecate-an-API)
*   [2 Identifying Deprecated API](#Identifying-Deprecated-API)
    *   [2.1 Java API](#Java-API)
    *   [2.2 Extension Points](#Extension-Points)
*   [3 Removal of Deprecated API](#Removal-of-Deprecated-API)
    *   [3.1 Third Party API](#Third-Party-API)
*   [4 References](#References)

What is Deprecation?
====================

API deprecation is used to inform API clients that a particular API element is no longer recommended for use. The deprecation comment should describe the reason for the deprecation, and directions for how to replace their usage with the new recommended way of doing things.

Process to deprecate an API
---------------------------

*   Created a pull request to deprecated the API and get a review from the project lead

Identifying Deprecated API
==========================

This section describes how clients can identify what API is deprecated. To identify API from non-API, see [\[1\]](https://help.eclipse.org/index.jsp?topic=%2Forg.eclipse.jdt.doc.isv%2Freference%2Fmisc%2Fapi-usage-rules.html)

Java API
--------

Java API is deprecated through use of the @deprecated javadoc tag on types, methods, and fields. The javadoc paragraph following the @deprecated tag defines the rationale for the deprecation and instructions on moving to equivalent new API.

Extension Points
----------------

Elements and attributes in extension points are deprecated by setting the "Deprecated" property to true in the [PDE](/PDE "PDE") extension point schema editor. The entire extension point can be deprecated by deprecating the "extension" element, which is the top level element at the root of any contribution to the extension point.

![Schema-deprecation.png](https://raw.githubusercontent.com/eclipse-platform/eclipse.platform/master/docs/images/Schema-deprecation.png)

Removal of Deprecated API
=========================

Deprecated API can be marked for deletion. See [https://github.com/eclipse-platform/.github/wiki/PMC-project-guidelines#api-removal-process](https://github.com/eclipse-platform/.github/wiki/PMC-project-guidelines#api-removal-process) for the process.

Third Party API
---------------

The Eclipse Project consumes and delivers bundles that are produced by other projects (including Orbit, ECF, and Equinox). The Eclipse Project sometimes consumes new major versions of these bundles, which may include API removals or other breaking changes in those bundles. As long as those changes don't affect the API of Eclipse Project bundles, such changes to third party bundles can happen at any time and without a two year waiting period. If those changes affect the API of Eclipse Project bundles, for example they are re-exported by Eclipse Project bundles or extended by Eclipse Project API, then this deprecation policy will be in effect. That is, the intent to remove API will be announced and will the API will remain in place for two full years prior to a release containing the deletion.

References
==========

*   [Java Deprecation Guidelines](http://java.sun.com/j2se/1.4.2/docs/guide/misc/deprecation/deprecation.html)
*   [Architecture Council/Meetings/API Deprecation 20080119](https://wiki.eclipse.org/Architecture_Council/Meetings/API_Deprecation_20080119 "Architecture Council/Meetings/API Deprecation 20080119")

