#!/usr/bin/env bash
__BASEDIR="$(readlink -f "$(dirname "$0")")";if [[ -z "$__BASEDIR" ]]; then echo "__BASEDIR: undefined";exit 1;fi

_trustStoreLocations=(
  # Debian/Ubuntu/Gentoo etc.
  "/etc/ssl/certs/ca-certificates.crt"
  # Fedora/RHEL 6
  "/etc/pki/tls/certs/ca-bundle.crt"
  # OpenSUSE
  "/etc/ssl/ca-bundle.pem"
  # OpenELEC
  "/etc/pki/tls/cacert.pem"
  # CentOS/RHEL 7
  "/etc/pki/ca-trust/extracted/pem/tls-ca-bundle.pem"
  # SLES10/SLES11, https://golang.org/issue/12139
  "/etc/ssl/certs"
  # Android
  "/system/etc/security/cacerts"
  # FreeBSD
  "/usr/local/share/certs"
  # Fedora/RHEL
  "/etc/pki/tls/certs"
  # NetBSD
  "/etc/openssl/certs"
)
systemTrustore() {
  for _trustStoreLocationToCheck in "${_trustStoreLocations[@]}"
  do
    if [[ -r "$_trustStoreLocationToCheck" ]]; then
      echo "$_trustStoreLocationToCheck"
    fi
  done
}


main(){
  # Setup with system properties for proxy and trust/key store
  local jcurlOpts="$JCURL_OPTS -Djava.net.useSystemProxies=true"

  local systemTustStore=`systemTrustore`
  if [[ "$OS"  == Windows*  ]];then
    jcurlOpts="$jcurlOpts
    	-Djava.net.useSystemProxies=true
    	-Djavax.net.ssl.trustStoreType=Windows-ROOT
    	-Djavax.net.ssl.trustStore=NONE
    	-Djavax.net.ssl.keyStoreType=Windows-MY
    	-Djavax.net.ssl.keyStore=NONE
	"
  elif [[ ! -f "$systemTustStore" ]]; then
    jcurlOpts="$jcurlOpts -Djavax.net.ssl.trustStoreType=PKCS12 -Djavax.net.ssl.trustStore=$systemTustStore"
  fi

  if [[ ! -x "${JAVA_HOME}\bin\java" ]]; then
    echo "invalid JAVA_HOME"
    return 1
  fi

  local jarFile=`ls "$__BASEDIR/target/"jcurl-*-BUNDLE.jar | sort | tail -1`
  "${JAVA_HOME}\bin\java" ${jcurlOpts} -jar "$jarFile" "$@"
  return $?
}

main "$@"
exit $?
