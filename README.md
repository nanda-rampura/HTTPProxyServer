# HTTPProxyServer
Multithreaded HTTP Proxy Server

HTTP proxy server is implemented in Java. It provides features like page caching, site blocking, content filtering, and logging. Proxy server is designed to handle multiple clients by using multithreading.

When the proxy server receives an HTTP request for an URL from the client, it generates a new HTTP request for the same URL and sends it to a remote  server that is hosting the requested URL. When the proxy server receives the corresponding HTTP response from the remote server, it creates a new HTTP response and sends it to the client.

lang.txt is used to list inappropriate words that needs to be filtered. blacklist.txt is used to list URLs that need to be blocked, along the time interval during which they need to be blocked. log.txt is generated when the server runs and starts accepting client request. It will log the traffic on proxy server. testedURLs.txt contains a list of URLs that have been tested with the proxy server implmentation.

Usage:
Start proxy server - "java ProxyServer <portNumber>", start client - "java Client <server hostname/IP Address> <portNumber>"
