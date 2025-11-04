#!/bin/bash

apt-get update && apt-get install -y tcpdump
tcpdump udp -U -i lo -w udp_traffic.pcap &
sleep 1