#!/usr/bin/env bash

userName="jenkins"
password="o2on5\$nqOPAx&Q7\$E4Rk"
debianPackage=$1
timeStamp=$(date +"%s")
snapshotVar="coreAgent-"$timeStamp

# download the cert
mkdir -p ~/cert
curl -k -o ~/cert/ca.crt -O https://repo.corp.evernym.com/ca.crt

#Upload to Kraken service (which takes care of uploading to aptly repo on repo.corp.evernym.com, snapshoting, cleanup(if any) and publishing):
echo "POST-ing artifacts to kraken endpoint..."
echo "debianPackage=$debianPackage"
echo "curl -u $userName:redacted --fail -X POST https://kraken.corp.evernym.com/repo/core_agent_dev/upload -F file=@$debianPackage --cacert ~/cert/ca.crt"
curl -u $userName:$password --fail -X POST https://kraken.corp.evernym.com/repo/core_agent_dev/upload -F file=@$debianPackage --cacert ~/cert/ca.crt
code=$?
if [ $code -ne 0 ]; then
    exit 1
fi
