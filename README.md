# Real Camera #

## [中文](README_ZH.md) ##

Make the camera more realistic in the first-person view.  
Supported versions: 1.20-1.21 Fabric & NeoForge.  
Download the mod from [Releases](https://github.com/xTracr/RealCamera/releases), [Modrinth](https://modrinth.com/mod/real-camera) or [CurseForge](https://curseforge.com/minecraft/mc-mods/real-camera)  
Snapshots are [here](https://github.com/xTracr/RealCamera/actions/workflows/build.yml)

## Features ##

* Bind the camera to a specific part of the body.
* Customize the position and rotation of the camera.
* Render player model in first-person perspective.
* Use F6 to toggle the feature on or off and other hotkeys to adjust the camera.
* Configure these features in the config screen (Cloth Config required).

### Configuration (0.6+) ###

* Theoretically, most mod models are supported, but need to be configured manually:
* First, set the key binding for `Open Model View Screen`.
  * ![model view screen](https://cdn.modrinth.com/data/fYYSAh4R/images/cc484d54238992077ab3632c274a2631efeca35f.png)
* Open the model view screen and left click with left Alt held to select the corresponding face of the model, scroll with left Alt held to switch between the different layers of the model.
* By clicking the `Selecting` button on the left, switch between the three to select the `Forward Vector`, `Upward Vector`, and `Target Plane`.
* Enter the `Preview` section, where you can see the relative relationship between the camera and the model and make certain adjustments (you can also adjust through key bindings).
  * ![preview](https://cdn.modrinth.com/data/fYYSAh4R/images/22cfcf444bbf2d3c0d0280e470a29f01b9308617.png)
* Enter a name and save (if needed, other settings such as priority can be changed).

## Dependencies ##

* Fabric:
  * [Fabric API](https://modrinth.com/mod/fabric-api)
* Both:
  * (Optional but recommended) [Cloth Config API](https://modrinth.com/mod/cloth-config)

## FAQ ##

* A part of the model (e.g. hair) is always in the way, how to make it invisible?
  * Increasing this value may help  
    ![screenshot_2024_6_2_22_42](https://github.com/xTracr/RealCamera/assets/57320980/78c246e8-34aa-4979-89de-780ee907870b)

### Compatibility ###

* Incompatible:
  * OptiFine
  * Armourer's Workshop
  * Armors based on GeckoLib
* Compatible:
  * most camera mods
  * most player model mods
