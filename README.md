# Ambassador

This is a Velocity plugin that makes it possible to host a modern forge server behind a Velocity proxy!

Unlike other solutions, this plugin does not require any special modifications to the client. (The player doesn't need to do anything)

This plugin is right now in it's alpha stage and should not be used in production, use it at your own risk, you have been warned.
## How to get started:
### On the Velocity proxy side:
1. Download and install this plugin to your proxy.
2. Start the proxy, then close it.
3. Go to "plugins/ambassador" and open "forgeServer.toml"
4. In the "Forge Server" field, put the name of the forge server. The name must be the same as that you put in "Velocity.toml" config.
5. Now you are done on the proxy side!

### On the Forge server side:
1. Download and install "Ambassador-Forge" as a mod to your forge server. (Found at https://github.com/adde0109/Ambassador-Forge)
2. Start the server.
3. If you wish to use modern forwarding, close the server and open "ambassador-common.toml" in the config folder and put your forwarding secret in the "forwardingSecret" field.
4. You are now ready to start the server and connect to it with Velocity!

## Limitations
* You can't switch servers when connecting with a forge client, yet...
* This plugin can't yet distinguish between forge and vanilla clients, so you either one must use forced hosts or other means.

## Planned features
* Ping Forwarding
* Distinguish between forge and vanilla client, and connect to the most suitalbe server. (Like VersionConnector)
* Server switching using kick to reset the client.
* Smart handshaking-data caching system to reduce load/network-traffic.
