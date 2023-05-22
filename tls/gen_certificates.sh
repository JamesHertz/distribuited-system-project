set -e

KEY_STORES=keystore
CERTS=certs
USERS_KSTORE=users-keystore.jks
LOG_FILE=out.txt

rm -f ./**/*.jks "$LOG_FILE"


[ -d $KEY_STORES ] || mkdir $KEY_STORES
[ -d $CERTS ] || mkdir $CERTS

servers=("users0-ourorg0" "feeds0-ourorg0" "feeds1-ourorg0" "feeds2-ourorg0"
"users0-ourorg1" "feeds0-ourorg1" "feeds1-ourorg1" "feeds2-ourorg1" "users0-ourorg2"
"feeds0-ourorg2" "feeds1-ourorg2" "feeds2-ourorg2")

echo
echo "generating everything at once :)"
echo

cp cacerts "$USERS_KSTORE"

i=0
for server in "${servers[@]}" ; do
  pass="password-$i"
  ksfile="./$KEY_STORES/$server.jks"
  crtfile="./$CERTS/$server.cert"

  # generate keystore (private+public key)
  keytool -alias "$server" -dname "cn=60198-61177, ou=TP2, o=SD2223, c=PT" \
      -genkeypair -storetype pkcs12 -storepass "$pass" -keyalg RSA \
      -validity 365 -keystore "$ksfile" -ext SAN="dns:$server"

  # export certificate
  keytool -exportcert -alias "$server" -keystore "$ksfile" -file "$crtfile" \
      -storepass "$pass"

  # import the certificate above the a keystore that will contain all the certificates of the server
  keytool -importcert -file "$crtfile" -alias "$server" -keystore "$USERS_KSTORE" \
      -storepass changeit -noprompt

  echo "$server,$ksfile,$pass" >> "$LOG_FILE"
  ((++i))
done

aux=$(sed -e "s/\.\/$KEY_STORES/keystore/g" "$LOG_FILE")
users=$(echo "$aux" | sed -e "/feeds/d" | tr '\n' ' ')
feeds=$(echo "$aux" | sed -e "/users/d" | tr '\n' ' ')

echo "users: $users" >> "$LOG_FILE"
echo "feeds: $feeds" >> "$LOG_FILE"



echo
echo "Done!! Some metada:"
echo

cat "$LOG_FILE"
