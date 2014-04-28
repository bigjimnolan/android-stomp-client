android-stomp-client
====================

This is barebones STOMP client with SSL

I created it to communicate with an ActiveMQ server via andoid devices.  When I made this, there were no simple solutions available.  If you have any quuestions or concersns, please write me at jiim@electroopticalvisions.com

Some known gotchas:  With ActiveMQ, you have to set up a security plugin if you want basic authentication to work
http://activemq.apache.org/security.html covers everything

Also, this program assumes a BKS style trust store to give to the constructor.  I know search engines helped with that part.  

For those of you wishing for clear comms, that shuould be a fairly easy change to the socket buidling methods.

Additional messages can be added, I merely had a requirement for these ones.

Thanks,

Jim Nolan
