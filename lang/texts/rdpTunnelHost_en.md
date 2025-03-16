## RDP Tunnel Host

You can connect to a remote RDP host via an SSH tunnel. This gives you the ability to use the more advanced SSH authentication features with RDP out of the box.

Upon first connection, an SSH tunnel will be established and the RDP client will connect to the tunneled connection via localhost instead. It will use the credentials of the SSH connection user for RDP authentication.