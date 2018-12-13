# Debian Package for JsonPath Project

[![TravisCI Status][travis-image]][travis-url]

**Build dependencies**

- debhelper (>= 9)
- cdbs
- default-jdk
- maven-debian-helper (>= 1.5)
- libmaven-bundle-plugin-java
- libslf4j-java
- libjackson-json-java
- [libjson-smart-java](https://github.com/yadickson/json-smart-debs)

**Download source code**

- unzip
- wget
- libc-bin
- dos2unix 

```
$ debian/rules get-orig-source
$ debian/rules publish-source
```

**Build project**

```
$ dpkg-buildpackage -rfakeroot -D -us -uc -i -I -sa
```
or
```
$ QUILT_PATCHES=debian/patches quilt push -a
$ fakeroot debian/rules clean binary
```

**Tested**

- Debian wheezy

**Repositories**

[Debian repository](https://bintray.com/yadickson/debian)

```
$ wget -qO - https://bintray.com/user/downloadSubjectPublicKey?username=bintray | sudo apt-key add -
```
```
$ echo "deb https://dl.bintray.com/yadickson/debian [distribution] main" | sudo tee -a /etc/apt/sources.list.d/bintray.list
```
```
$ sudo apt-get update
$ sudo apt-get upgrade -y
$ sudo apt-get install libjsonpath-java
```

## License

GPL-3.0 © [Yadickson Soto](https://github.com/yadickson)

Apache-2.0 © [JsonPath](https://github.com/json-path/JsonPath)

[travis-image]: https://api.travis-ci.org/yadickson/jsonpath-debs.svg?branch=wheezy
[travis-url]: https://travis-ci.org/yadickson/jsonpath-debs

