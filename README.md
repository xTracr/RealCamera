# Real Camera

### [中文](README_ZH.md)

Make the camera more realistic in the first-person view.  
Supported versions: 1.18.2-1.20.4 Forge, 1.18.2-1.21.8 Fabric, 1.21-1.21.8 NeoForge.  
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
  * ![gui_configs](https://cdn.modrinth.com/data/fYYSAh4R/images/4625282a5683cd38eba2947be1ca82e595e18bad.png)
* Open the model view screen and left click with left Alt held to select the corresponding face of the model, scroll with left Alt held to switch between the different layers of the model.
* By clicking the `Selecting` button on the left, switch between the three to select the `Forward Vector`, `Upward Vector`, and `Target Plane`.
* From the top right button, enter the `Preview` section, where you can see the relative relationship between the camera and the model and make certain adjustments (you can also adjust through key bindings).
  * ![gui_preview](https://cdn.modrinth.com/data/fYYSAh4R/images/44c87a6f1750f8d1b03422120e6042d1098896cb.png)
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
  * ![gui_disable](https://cdn.modrinth.com/data/fYYSAh4R/images/b49ac4da6bf8a59f13c7e93ca1ef76b73e5d23b4.png)

## Dependencies

* Fabric:
  * [Fabric API](https://modrinth.com/mod/fabric-api)
* Both:
  * (Optional but recommended) [Cloth Config API](https://modrinth.com/mod/cloth-config)

## FAQ

* A part of the model (e.g. hair) is always in the way, how to make it invisible?
  * Increasing this value may help, or use `Disable` section  
    ![disable_depth](https://github.com/xTracr/RealCamera/assets/57320980/78c246e8-34aa-4979-89de-780ee907870b)
* What key to press can open Model View Screen?
  * Set the key binding by yourself
* Why can't i open the Config Screen?
  * Please downlowd [Cloth Config API](https://modrinth.com/mod/cloth-config)
* Why does it show binding failed when use YSM models?
  * Snapshot 0.6.15 "try" to fix problems, but it didn't fix completely
  * If Minecraft version is 1.20.1, then bind upward vector to head left (or right) side, and set roll angle to 90 (or -90) in preview section
* Why does the head of YSM model disappear when in the Model View Screen?
  * You downloaded Better Combat, just put it away

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
  * Superb Warfare

* Model Mod Compatibility Requirements with `Real Camera` (Based on Official Mappings)：
* Vertex Data Acquisition
  * `Real Camera` obtains vertex data by overriding the `MultiBufferSource multiBufferSource` parameter of the public method `EntityRenderDispatcher.render`
  * The call timing is before the `Camera.setup` phase in the `GameRenderer.renderLevel` method
  * *Therefore*, the mod needs to meet the following technical conditions:
    * Use only the **provided** `multiBufferSource` parameter when rendering `Minecraft.getCameraEntity`, and avoid alternative approaches for obtaining or creating `MultiBufferSource` instances
    * The overall rendering behavior remains unaffected when rendering the player model at the adjusted timing
    * Ensure all vertex data ultimately passes through `VertexConsumer` gotten by `MultiBufferSource.getBuffer`
* Render Times Compatibility
  * `Real Camera` renders `Minecraft.getCameraEntity` by calling the public method `EntityRenderDispatcher.render`
  * This is the second call to the `EntityRenderDispatcher.render` method in a frame (the first call is to solve the camera parameters).
  * *Therefore*, the mod needs to meet the condition that the overall rendering behavior remains unaffected when rendering the player model **multiple times** in the same frame
