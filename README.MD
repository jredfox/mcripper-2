Rip Minecraft assets from mojang for archiving purposes. You will still need to buy an account to run the game of course. this program is for archiving purposes only. You can rip from .minecraft

Commands
- rip dir/version.json/assetsIndex.json outputDir --mcDir=value -s -a
- rip assetsIndex.json & minecraft.jar & outputDir -s -a
- checkMojang --skipSnaps --mcDir=value
- checkDisk --skipSnaps --skipOldMajors --mcDir=value
- checkOmni
- checkOld --mcDir=value --skipSnaps
- recomputeHashes
- verify
- help

Details:
- `rip -a` flag specifies to rip all assets from the `minecraft.jar` that are not in the assets index including unessary files like `.png, .nbt` files
- `rip -s` skips the `minecraft.jar` ripping part which will lead to broken resource packs as it's missing the `.mcmeta` files
- `checkMojang` rips all assets obtainable from checking mojang's master json files from https://launchermeta.mojang.com/mc/game/version_manifest.json and https://launchermeta.mojang.com/mc/game/version_manifest_v2.json
- `checkDisk` checks all jsons files stored at `mcripped/mojang/jsons/*` the purpose of this command is to recheck all jsons stored on the disk even custom ones no longer obtainable
- `checkOmni` checks omni archive that has mc versions that the current launcher does not
- `checkOld` checks files of older minecraft sites such as s3.amazonaws.com all minecraft domains and uses legacy json apis. the major json is found at http://s3.amazonaws.com/Minecraft.Download/versions/versions.json
- `verify` verifies the integraty of your current archive downloads and deletes invalid files without the `--info` flag. As checking the hash integraty of every download would slow down the process and mojang's files don't always read the hash the json/url provides
- `recomputeHashes` use this command after the verify command to recompute your index.hash and reset your machine learned data

Links / API notes:
```
JSON/Web APIS:
- New Major V1: https://launchermeta.mojang.com/mc/game/version_manifest.json
- New Major V2: https://launchermeta.mojang.com/mc/game/version_manifest_v2.json
- Old Major: http://s3.amazonaws.com/Minecraft.Download/versions/versions.json
- Old Assets Index: https://s3.amazonaws.com/Minecraft.Download/indexes/<assetsIndexId>.json
- Old Minor Json: https://s3.amazonaws.com/Minecraft.Download/versions/<versions>/<version>.json
- Old Version: http://s3.amazonaws.com/Minecraft.Download/versions/<version>/<version>.jar
- Old Server: http://s3.amazonaws.com/Minecraft.Download/versions/<version>/minecraft_server.<version>.jar
- Old Server Exe: http://s3.amazonaws.com/Minecraft.Download/versions/<version>/minecraft_server.<version>.exe
- Old < 1.6: http://assets.minecraft.net/<version>.replaceAll("\\.","_")/minecraft.jar
- Old < 1.6: http://assets.minecraft.net/<version>.replaceAll("\\.","_")/minecraft_server.jar
- Old < 1.6: http://assets.minecraft.net/<version>.replaceAll("\\.","_")/Minecraft_Server.exe

Amazon Aws indexes:
- http://assets.minecraft.net uses the same index as regular amazonaws however it's currently down so it's embedded into this project now
- http://s3.amazonaws.com/MinecraftResources
- Old 2012 and below Resources: http://s3.amazonaws.com/Minecraft.Resources
- Old: http://s3.amazonaws.com/MinecraftDownload
```
