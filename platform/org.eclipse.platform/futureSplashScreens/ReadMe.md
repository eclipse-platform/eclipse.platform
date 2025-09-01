## Description

This directory contains the Splash-screen and About-dialog images of **future** versions of Eclipse.
It serves as a kind of staging area and is not included in the built bundle.
These images will be moved to their final location by the `prepareNextDevCycle.sh` script, which is called during the preparation of the development of the corresponding release.
Therefore the image(s) for a corresponding release must be available in this directory **BEFORE the DEVELOPMENT of that release is PREPARED** and the following requirements apply:

- The Splash-screen images **must** be named according to the following schema:
`splash_${NEXT_RELEASE_YEAR}.png`
for example `splash_2025-12.png`.

- The About-dialog image (and it's high-DPI version) of a future version must be added to this directory, if changed, and be named
`eclipse_lg.png` respectively `eclipse_lg@2x.png`

While there is a dedicated Splash-screen image for each release (containing the version of the release),
the About-dialog image is the same for multiple releases as long as they share the same Splash-screen base style/template.

The Splash-screen and About-dialog images of the currently developed and past Eclipse versions don't need to be included in this directory.

## Providing new Splash-screen images for a future release

Due to the requirements described above, the images for a new Splash-screen base style/template must be added to this folder
before the development of the first release that is intended to use them is prepared.
For example if a Splash-screen style/template is used for 2026-06 ff., the new images should be added at latest near the end of the 2026-03 development.
When a new Splash-screen style/template is introduced, typically the Splash-screen images for four consecutive releases are added to this directory,
following the naming requirements described above.
The About-dialog image(s) is typically the same for these four consecutive releases, i.e. as long as the Splash-screen style/template is unchanged.

The Splash-screen and About-dialog images of the currently developed and past Eclipse versions in this folder are irrelevant and should be removed.
In fact the mentioned `prepareNextDevCycle.sh` script will _move_ the images to their production location when they become effective, i.e. when a new release is prepared.
This behavior clears this directory automatically over time.

To test the result of a future release preparation, you can run the following bash snippet (adjust the numbers accordingly):
````
export NEXT_RELEASE_VERSION=4.39
export NEXT_RELEASE_NAME=2026-03
./prepareNextDevCycle.sh
```
**Please make sure to not submit the resulting commit!**
