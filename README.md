# Ambassador

This is a Velocity plugin that makes it possible to host a modern Forge server behind a Velocity proxy!

Unlike other solutions, this plugin does not require any special modifications to the backend server nor the client. (The player doesn't need to do anything)

This plugin is right now in its alpha stage and should not be used in production, use it at your own risk, you have been warned.
## How to get started:
### On the Velocity proxy side:
1. Download and install this plugin to your proxy.

### On the Forge server side (Only required if you want modern forwarding):
1. Download and install "Ambassador-Forge" as a mod to your Forge server. (Found at https://github.com/adde0109/Ambassador-Forge)
2. Start the server.
3. If you wish to use modern forwarding, close the server and open "ambassador-common.toml" in the config folder and put your forwarding secret in the "forwardingSecret" field.
4. In "server.properties" make sure online-mode is set to false.
5. You are now ready to start the server and connect to it with Velocity!

## Features
* Server switching.
* Server switching using kick to reset the client.
* Server switching using client mod: https://github.com/Just-Chaldea/Forge-Client-Reset-Packet
