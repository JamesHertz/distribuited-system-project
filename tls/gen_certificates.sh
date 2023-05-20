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
for server in ${servers[@]} ; do
  pass="password-$i"
  ksfile="./$KEY_STORES/$server.jks"
  crtfile="./$CERTS/$server.cert"
  keytool -alias "$server" -dname "cn=60198-61177, ou=TP2, o=SD2223, c=PT" \
      -genkeypair -storetype pkcs12 -storepass "$pass" -keyalg RSA \
      -validity 365 -keystore "$ksfile" -ext SAN="dns:$server"

  keytool -exportcert -alias "$server" -keystore "$ksfile" -file "$crtfile" \
      -storepass "$pass" 

  keytool -importcert -file "$crtfile" -alias "$server" -keystore "$USERS_KSTORE" \
      -storepass changeit -noprompt

  echo "$server,$ksfile,$pass" >> "$LOG_FILE"
  ((++i))
done

aux=$(sed -e "s/\.\/$KEY_STORES/keystore/g" "$LOG_FILE")
users=$(echo "$aux" | sed -e "/feeds/d" | tr '\n' ' ')
feeds=$(echo "$aux" | sed -e "/users/d" | tr '\n' ' ')

echo >> "$LOG_FILE"
echo "users: $users" >> "$LOG_FILE"
echo "feeds: $feeds" >> "$LOG_FILE"



echo
echo "Done!! Some metada:"
echo

cat "$LOG_FILE"

# rm -f *.jks
#
# CN=Users.Users, OU=TP2, O=SD2223, L=LX, ST=LX, C=PT
# keytool -alias root -dname "cn=60198-61177, ou=TP2, o=SD2223, c=PT" -genkeypair -storepass password -keyalg RSA
# keytool -alias root -dname "cn=60198-61177, ou=TP2, o=SD2223, c=PT" -genkeypair -storepass password  -alias users -keyalg RSA -validity 365 -keystore ./users.jks -storetype pkcs12 
# keytool -alias root -dname "cn=RootCA, ou=Root_CertificateAuthority, o=CertificateAuthority, c=IN" -genkeypair -storepass password -keyalg RSA
# keytool -genkey -alias users -keyalg RSA -validity 365 -keystore ./users.jks -storetype pkcs12 << EOF
# 123users
# 123users
# Users.Users
# TP2
# SD2223
# LX
# LX
# PT
# yes
# EOF
# # sig = asyncEnc( priv-key, hash(data) )
# # data || sig
# #123users
# #123users
#
# echo
# echo
# echo "Exporting Certificates"
# echo
# echo
#
# keytool -exportcert -alias users -keystore users.jks -file users.cert << EOF
# 123users
# EOF
#
# echo "Creating Client Truststore"
# cp cacerts client-ts.jks
# keytool -importcert -file users.cert -alias users -keystore client-ts.jks << EOF
# changeit
# yes
# EOF
