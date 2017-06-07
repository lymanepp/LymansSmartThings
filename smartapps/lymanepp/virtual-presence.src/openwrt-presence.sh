#!/bin/sh

# Credit
# Idea from fuzzysb. Cleaned up and modified for OpenWRT
# https://github.com/fuzzysb/SmartThings/blob/master/smartapps/fuzzysb/asuswrt-wifi-presence.src/CheckIfHome

URI="https://graph-na02-useast1.api.smartthings.com"
APPL_ID="YOUR-APPL-ID-GOES-HERE"
TOKEN="YOUR-TOKEN-GOES-HERE"
STATE=/tmp/Presence

update_smartthings()
{
  input=$1
  user=$2
  mac=$3

  file=${STATE}/${user}-Home

  case "$input" in
    *$mac*)
      if [ ! -f $file ]
      then
        touch $file
        curl -k "${URI}/api/smartapps/installations/${APPL_ID}/presence/${user}/present?access_token=${TOKEN}"
      fi
      ;;
    *)
      if [ -f $file ]
      then
        rm -f $file
        curl -k "${URI}/api/smartapps/installations/${APPL_ID}/presence/${user}/not+present?access_token=${TOKEN}"
      fi
      ;;
  esac
}

if [ ! -d $STATE ]
 then
  mkdir $STATE
fi

stations="`iw dev wlan0 station dump; iw dev wlan1 station dump; iw dev wlan2 station dump`"

update_smartthings "$stations" John "aa:aa:aa:aa:aa:aa"
update_smartthings "$stations" Jane "bb:bb:bb:bb:bb:bb"
update_smartthings "$stations" Bobby "cc:cc:cc:cc:cc:cc"
update_smartthings "$stations" Sue "dd:dd:dd:dd:dd:dd"
