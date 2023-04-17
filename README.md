# Ambassador

This is a Velocity plugin that makes it possible to host a modern Forge server behind a Velocity proxy!

Unlike other solutions, this plugin does not require any special modifications to the backend server nor the client. (The player doesn't need to do anything)

## How to get started:
1. Download and install this plugin to your proxy.
2. After starting the server, configure the plugin it to your liking using the config file found in the folder "Ambassador".
3. If you want to use player-information forwarding you can use any of these mods on the 1.13+ forge server:
- https://github.com/adde0109/Proxy-Compatible-Forge (Modern forwarding)

- https://github.com/caunt/BungeeForge (Legacy forwarding)

## Features
* Server switching using kick to reset the client with configureble message and switch timeout.
* Server switching using client mod for instant server switching: 
https://github.com/Just-Chaldea/Forge-Client-Reset-Packet 
1.18.2 and 1.19 fork:
https://github.com/adde0109/Forge-Client-Reset-Packet

## Stuck on "Negotiating":
Why: This is happening because the client finishes the reset after the reset-timeout time has passed and thus, the proxy disconnects the client and the screen. Usually because to the client took too long to reset the forge registries.

Fix: Increase the "reset-timeout" in the config. 
If you know that everyone who is connecting to the server is using the packet client reset packet mod (Pixelmon include this mod), you can set this value to the max (Velocity's timout found in Velocity.toml).
This timout is meant to allow for people that doesn't have the mod to get disconnected more quickly.

## Discord
https://discord.gg/Vusz9pBNyJ
