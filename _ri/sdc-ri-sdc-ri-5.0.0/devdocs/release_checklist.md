# 1 Release checklist

This release checklist helps during the preparation and deployment of releases.
It gives instructions for use to bump a version number, built the project, create proper branches and upload artifacts 
to Maven Central.

# 2 Major and minor release

This chapter explains how to release a major or minor version.
See section 3 below to learn more about bugfix releases.

## 2.1 Check dependencies for updates

Walk through all Maven project dependencies and update to the latest version.
Ship your changes to develop by means of a merge request.

## 2.1.1 Remove snapshot repository

Ensure the snapshot repository is removed from the POM.
```xml
<repositories>
    <repository>
        <id>sonatype</id>
        <name>OSS Sonatype Snapshots</name>
        <url>https://oss.sonatype.org/content/repositories/snapshots</url>
    </repository>
</repositories>
```

## 2.2 Setup release branch

Create a new release branch from latest develop named `release/X.Y` with `X.Y` being the major and minor version of the
release.

> Note that bug fix versions are not part of the release branch name as those are supposed to be merged into the release 
branch without changing the branch name.

## 2.3 Double-check release notes for completeness

Typically the changelog is supposed to be fostered during development on develop.
Nevertheless, a final look to the changelog can help finding inconsistencies with the actual implementation.
Once done, bump the version number to the CHANGELOG.md file by moving the _[Unreleased]_ contents to a new section 
labelled with the upcoming version and release date.

Example: 

```
## [Unreleased]
    
### Added

- Foo
- Bar
```

is changed to
    
```
## [Unreleased]
    
## [2.2.1] - 2020-02-29
    
### Added
        
- Foo
- Bar
```

## 2.4 Call Maven's release preparation to bump version numbers on the parent module and all submodules

```
$ mvn release:prepare
```

Always type in the three-fielded semantic version number when asked.
The requested Git tag name shall have the format `sdc-ri-X.Y.Z` with `X.Y.Z` being the version number of the release.

The Maven release plugin creates two different commits:

1. `[maven-release-plugin] prepare for next development iteration`, in the following `[develop-commit]`
2. `[maven-release-plugin] prepare release sdc-ri-X.Y.Z`, in the following `[release-commit]`

The first includes POMs with version numbers targeted to the next developed version, e.g. X.(Y+1).0-SNAPSHOT).
The second includes POMs with version numbers targeted to the actual release, i.e. X.Y.Z.

## 2.5 Call Maven's project deployment command

Make sure `[release-commit]` is checked out and call:

```
$ mvn -DskipTests=true install deploy -Pdeploy 
```

To deploy the project, Maven requires the signing process to be configured in the local _.m2/settings.xml_.
See the following template:

```xml
<servers>
    <server>
        <id>ossrh</id>
        <username>SONATYPE_USERNAME</username>
        <password>SONATYPE_PASSWORD</password>
    </server>
</servers>
<profiles>
    <profile>
        <id>ossrh</id>
        <activation>
            <activeByDefault>true</activeByDefault>
        </activation>
        <properties>
            <!-- note the exclamation mark at the end, which is intentional -->
            <gpg.keyname>GPG_KEYNAME!</gpg.keyname>
            <gpg.passphrase>GPG_PASSPHRASE</gpg.passphrase>
        </properties>
    </profile>
</profiles>
```

## 2.6 Final verification prior to Maven Central deployment

As an additional verification step before delivering to Maven Central, go to http://oss.sonatype.org, login and click
 _Staging Repositories_ from the left menu. 
Double-check that everything is as expected, _Close_ the staged artifacts, which will take a while. 
Once the _Release_ button is enabled, click to finally release.

## 2.7 Fast-forward to master and push to release, develop and master

Checkout `[develop-commit]`, push to `origin/develop` and create a merge request for the next development iteration.

Checkout `[release-commit]` and push changes to `origin/release`.
Fast-forward changes to master and push by calling:

```
$ git checkout master
$ git merge release/X.X
$ git push origin master
```

Do not forget to push the locally generated version tag:

````
$ git push origin sdc-ri-X.X.X
````

> In order to successfully conduct all push commands you need Gitlab _Maintainer_ rights.

## 2.8 Publish release description

Add a Gitlab release description that includes the changelog of the preceding release.
Feel free to add further notes if they are of any public interest.
See https://gitlab.com/help/user/project/releases/index to find more information on Gitlab release descriptions.

## 2.9 Update develop changelog (_Bugfix release only_)

When doing a bugfix release, the changelog in `develop` must be updated to include the new version as well. Add the entry
you previously created in step 3 and remove changes included in your bugfix release from unreleased changes if applicable.

# 3 Bugfix release

A bugfix release differs from the usual major/minor version release in the way that some steps can be omitted and some
need extra attention.
Please make yourself confident with the major/minor release instructions before continuing on the bugfix release steps.

## 3.1 Create Gitlab issue that addresses the bugfix release

Go to Gitlab and create a ticket in order to get an issue number for a bugfix release.
The issue serves two purposes:

1. Somebody can be set in charge of conducting the bugfix release
2. The number can be used for release commits and branch names

## 3.2 Create a feature branch and cherry-pick fixes

For example:

```
$ git checkout -b feature/#42_release_X.Y.Z
$ git cherry-pick <commit-id-with-fix>
``` 

## 3.3 Add release notes

Only add the new version and its changes to the changelog. 
Example given version 2.2.2 is the bugfix release version made on 2020-03-01 with two fixes and a security patch:

```
## [2.2.2] - 2020-03-01

### Fixed

- Jaba
- Daba

### Security

- Dooo
```

## 3.4 Bump temporary bugfix release version number

The Maven release plugin cannot process the prepare step on versions that are not tagged with a trailing 
`-SNAPSHOT`, hence the POM versions need to be set to a proper value before running the plugin:

```
$ mvn versions:set -DgenerateBackupPoms=false -DnewVersion=X.Y.Z-SNAPSHOT
```

## 3.5 Create merge request on the release branch

Push your changes and create a merge request to the release branch.
This allows a reviewer to double-check if fixes have been applied correctly and the changelog was altered appropriately.

### 3.6 Release preparation and actual deployment

After the merge request is delivered, checkout the release branch and start the Maven release preparation and 
deployment as described in sections 2.4, 2.5 and 2.6.

As `[develop-commit]` is not used after preparation, it can safely be removed from the repository:

```
$ git reset --hard HEAD^
``` 

### 3.7 Fast-forward to master and push to release and master

Do the steps from section 2.7, but rather than pushing the next development version to `origin/develop`, update the 
changelog in the develop branch such that it fits the previous release delivery, i.e. move the fixes and patches to an 
appropriate section and create a merge request.

```
## [Unreleased]

### Added

- Some stuff

### Fixed

- Jaba
- Daba

### Security

- Dooo
```

devolves into

```
## [Unreleased]

### Added

- Some stuff

## [2.2.2] - 2020-03-01

### Fixed

- Jaba
- Daba

### Security

- Dooo
```

### 3.8 Final steps

Finish the bugfix release by following instructions from section 2.8 and 2.9.