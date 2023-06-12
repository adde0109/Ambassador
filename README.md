# Ambassador

This is a Velocity plugin that makes it possible to host a modern Forge server behind a Velocity proxy!

Unlike other solutions, this plugin does not require any special modifications to the backend server nor the client. (The player doesn't need to do anything)

## How to get started:
1. Download and install this plugin to your proxy.
2. After starting the server, configure the plugin it to your liking using the config file found in the folder "Ambassador".
3. If you want to use player-information forwarding you can use any of these mods on the 1.13+ forge server:
- https://github.com/adde0109/Proxy-Compatible-Forge (Modern forwarding) (Magma 1.18.2 and higher includes this)

- https://github.com/caunt/BungeeForge (Legacy forwarding)

## Features
* Server switching using kick to reset the client with configureble message and switch timeout.
* ServerRedirect support for auto-reconnecting during switch.
* Server switching using client mod for instant server switching: 
https://github.com/Just-Chaldea/Forge-Client-Reset-Packet 
1.18.2 and 1.19 fork:
https://github.com/adde0109/Forge-Client-Reset-Packet

## Stuck on "Negotiating":
This is an issue with Client Reset Packet Mod being partly incompatible with certain mods on the client. Please remove incompatible mods on the client if you have this issue. (Yes, this also includes client-side only mods.)

## Discord
https://discord.gg/Vusz9pBNyJ
