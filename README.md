# Real Camera

### [中文](README_ZH.md)

Make the camera more realistic in the first-person view.  
Supported versions: 1.18.2-1.20.4 Forge, 1.18.2-1.21.4 Fabric, 1.21-1.21.4 NeoForge.  
Download the mod from [Releases](https://github.com/xTracr/RealCamera/releases), [Modrinth](https://modrinth.com/mod/real-camera) or [CurseForge](https://curseforge.com/minecraft/mc-mods/real-camera)  
Snapshots are [here](https://github.com/xTracr/RealCamera/actions/workflows/build.yml)

## Features

* Bind the camera to a specific part of the body.
* Customize the position and rotation of the camera.
* Render player model in first-person perspective.
* Use F6 to toggle the feature on or off and other hotkeys to adjust the camera.
* Configure these features in the Config Screen (Cloth Config required) and the Model View Screen (0.6+).

### Configuration (0.6+)

* Theoretically, most mod models are supported, but need to be configured manually:
* First, set the key binding for `Open Model View Screen`.
  * ![model view screen](https://cdn.modrinth.com/data/fYYSAh4R/images/cc484d54238992077ab3632c274a2631efeca35f.png)
* Open the model view screen and left click with left Alt held to select the corresponding face of the model, scroll with left Alt held to switch between the different layers of the model.
* By clicking the `Selecting` button on the left, switch between the three to select the `Forward Vector`, `Upward Vector`, and `Target Plane`.
* From the top right button, enter the `Preview` section, where you can see the relative relationship between the camera and the model and make certain adjustments (you can also adjust through key bindings).
  * ![preview](https://cdn.modrinth.com/data/fYYSAh4R/images/22cfcf444bbf2d3c0d0280e470a29f01b9308617.png)
* Enter a name and save.

#### Tips

* Configs can have their priority adjusted - higher priority configs appear higher in the right-side list
* Disable depth in `Preview` section to hide models blocking the view
* About `Disable` section (current version still rough, complex operations):
    * When texture ID field is empty, use Left Alt+Left Click to select texture
    * Model parts contained by blue boxes won't be rendered
    * Left-click drag to select in texture view, other keys cancel selection
    * Left Alt+Left Click quickly selects hovered parts
    * In `All` mode, entire texture's model won't render (blue box containing the texture in left panel indicates this)

## Dependencies

* Fabric:
  * [Fabric API](https://modrinth.com/mod/fabric-api)
* Both:
  * (Optional but recommended) [Cloth Config API](https://modrinth.com/mod/cloth-config)

## FAQ ##

### Compatibility

* Incompatible:
  * OptiFine
  * Armourer's Workshop (Mod version 0.6+)
  * Armors based on GeckoLib
  * Customizable Player Models
  * Epic Fight (Mod version 0.6-)
  * Timeless and Classics Zero,version 1.1.4+

* Compatible:
  * most camera mods
  * most player model mods
  * Armourer's Workshop (Mod version 0.6-)
  * Epic Fight (Mod version 0.6+)
  * First-person Model
  * Not Enough Animations
  * ParCool!
  * Pehkui
  * Player Animation Lib
  * Timeless and Classics Zero,version 1.0.3-
  * Yes Steve Model (Not stably compatible)


* Model Mod Compatibility Requirements with `Real Camera` (Based on Official Mappings)：
* Render Timing Compatibility
  * `Real Camera` renders `Minecraft.getCameraEntity` through the public method `EntityRenderDispatcher.render`
  * The invocation timing has been moved to occur before the `Camera.setup` phase within the `GameRenderer.renderLevel` workflow
  * *Therefore*, mod implementations should ensure that the overall rendering behavior remains unaffected by this timing adjustment
* Vertex Data Acquisition
  * `Real Camera` obtains vertex data by overriding the `MultiBufferSource multiBufferSource` parameter in the `EntityRenderDispatcher.render` method
  * *Therefore*, mod implementations need to:
    - Use only the **provided** `multiBufferSource` parameter when rendering `Minecraft.getCameraEntity`
    - Avoid alternative approaches for obtaining or creating `MultiBufferSource` instances
    - Ensure all vertex data ultimately passes through `VertexConsumer` objects acquired via `MultiBufferSource.getBuffer`
