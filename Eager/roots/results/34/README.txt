Distributed AppScale setup
 * 6 app servers, 3 database nodes, 1 master node

8 instances of Javabook app
/etc/haproxy/sites-enabled/gae_j1.cfg:  server gae_j1-0 10.2.122.204:20000 maxconn 7 check
/etc/haproxy/sites-enabled/gae_j2.cfg:  server gae_j2-0 10.2.122.208:20001 maxconn 7 check
/etc/haproxy/sites-enabled/gae_j3.cfg:  server gae_j3-0 10.2.122.217:20002 maxconn 7 check
/etc/haproxy/sites-enabled/gae_j4.cfg:  server gae_j4-0 10.2.122.213:20000 maxconn 7 check
/etc/haproxy/sites-enabled/gae_j5.cfg:  server gae_j5-0 10.2.122.202:20000 maxconn 7 check
/etc/haproxy/sites-enabled/gae_j6.cfg:  server gae_j6-0 10.2.122.213:20001 maxconn 7 check
/etc/haproxy/sites-enabled/gae_j7.cfg:  server gae_j7-0 10.2.122.213:20002 maxconn 7 check
/etc/haproxy/sites-enabled/gae_j8.cfg:  server gae_j8-0 10.2.122.202:20002 maxconn 7 check

SLO 75ms at 95% (1 hour history)

Fault injection:
All API calls from 10.2.122.213 slowed down by 100ms between 0-6min of every second hour
