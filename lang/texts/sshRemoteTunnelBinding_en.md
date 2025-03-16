## Binding

The binding information you provide is passed straight to the `ssh` client as follows: `-R [remote_source_address:]remote_source_port:origin_destination_address:origin_destination_port`.

By default, the remote source address will bind to the loopback interface. You can also make use of any address wildcards, e.g. setting the address to `0.0.0.0` in order to bind to all network interfaces accessible via IPv4. When you completely omit the address, the wildcard `*`, which allows connections on all network interfaces, will be used. Note that some network interfaces notation might not be supported on all operating systems. Windows servers for example don't support the wildcard `*`.
