# Publishing to Maven Central

This project is configured for the [Central Publisher Portal](https://central.sonatype.org/publish/publish-portal-maven/).
Daily builds (`mvn verify`) do not publish. Release uses `-P release`.

## Coordinates

| Item | Value |
|------|--------|
| Domain | [sinaq.uz](https://sinaq.uz) |
| Maven `groupId` | `uz.sinaq` |
| Java packages | `io.sinaq.api.*` (unchanged) |

## 1. Verify namespace `uz.sinaq`

1. Log in at [https://central.sonatype.com/](https://central.sonatype.com/)
2. **View Namespaces** → **Add Namespace** → `uz.sinaq`
3. Add the **TXT** DNS record Portal shows on `sinaq.uz` (registrar DNS panel)
4. Click **Verify** — wait until status is **Verified** (can take minutes to hours)

Until verified, `mvn deploy` is rejected.

## 2. GPG key (required)

```bash
brew install gnupg
gpg --full-generate-key
# RSA 4096, your name + email
gpg --list-secret-keys --keyid-format LONG
gpg --keyserver keys.openpgp.org --send-keys YOUR_KEY_ID
```

## 3. Portal token

1. [central.sonatype.com](https://central.sonatype.com/) → **Account** → **Generate User Token**
2. `~/.m2/settings.xml` (never commit):

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

`<id>` must be `central`.

## 4. Deploy

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn -P release clean deploy
```

`autoPublish` is **false**: upload → validate → click **Publish** in the Portal.

After it is live:

```xml
<dependency>
  <groupId>uz.sinaq</groupId>
  <artifactId>sinaq-api-starter</artifactId>
  <version>1.2.1</version>
  <scope>test</scope>
</dependency>
```

`examples` and `integration-tests` are not published.

## 5. GitHub release (optional)

```bash
git tag v1.2.1
git push origin v1.2.1
```
