# Publishing to Maven Central

This project is configured for the [Central Publisher Portal](https://central.sonatype.org/publish/publish-portal-maven/).
Daily builds (`mvn verify`) do not publish. Release uses `-P release`.

## 1. Namespace (`groupId`)

Artifacts use `io.sinaq`. Central only allows that namespace if you **prove you own it**.

| If you have… | Register this namespace |
|--------------|-------------------------|
| Domain `sinaq.io` | `io.sinaq` (TXT DNS record in the Portal) |
| Only this GitHub account | `io.github.xumoyun005` (then we must change every `groupId`) |

Create a publisher account: [https://central.sonatype.com/](https://central.sonatype.com/)  
Then **View Namespaces** → **Add Namespace**.

Until the namespace is **Verified**, `mvn deploy` will be rejected.

## 2. GPG key (required)

macOS:

```bash
brew install gnupg
gpg --full-generate-key
# RSA, 4096 bits, your name + email
gpg --list-secret-keys --keyid-format LONG
gpg --keyserver keys.openpgp.org --send-keys YOUR_KEY_ID
```

Remember the passphrase. Maven will ask for it when signing.

## 3. Portal token

1. Log in at [https://central.sonatype.com/](https://central.sonatype.com/)
2. **Account** → **Generate User Token**
3. Put it in `~/.m2/settings.xml` (never commit this file):

```xml
<settings>
  <servers>
    <server>
      <id>central</id>
      <username>TOKEN_USERNAME</username>
      <password>TOKEN_PASSWORD</password>
    </server>
  </servers>
</settings>
```

The `<id>` must be `central` (matches `publishingServerId` in the parent POM).

## 4. Deploy

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
cd /path/to/sinaq-api-java
mvn -P release clean deploy
```

`autoPublish` is **false**: the bundle is uploaded and validated, then you click **Publish** in the Portal.

After it is live (often 10–30 minutes):

```xml
<dependency>
  <groupId>io.sinaq</groupId>
  <artifactId>sinaq-api-starter</artifactId>
  <version>1.0.0</version>
  <scope>test</scope>
</dependency>
```

`examples` and `integration-tests` are not published.

## 5. GitHub release (optional)

```bash
git tag v1.0.0
git push origin v1.0.0
```

Then GitHub → **Releases** → **Create a new release** from `v1.0.0`.
