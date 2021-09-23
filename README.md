# DbSigner
Java addin that signs databases

# Build DbSigner

1) Install Maven
2) Add Notes.jar to your maven repo, that will make it accessible to Maven

```
mvn install:install-file -Dfile=path\to\Notes.jar
```

3) Build DbSigner.jar

```
mvn package
```

This should create a DbSinger.jar for you. You need to use build with dependecies (i.e. DbSigner-0.1.0-jar-with-dependencies.jar).
It's because our project uses JSON.simple to parse config file and JSON.simple is not available to Domino Java environment.

# How to register DbSigner on Domino server

1) Upload file to Domino server (on Windows it's in the Domino executable folder).

JavaAddin\DbSigner.jar

2) Upload DbSigner.json 

Make sure there is a file dbsigner.json in Domino executable folder.

```
{
"databases": [{"filepath": "test1.nsf"}, {"filepath": "test2.nsf"}, {"filepath": "C:\IBM\Domino\data\A55F0D\wysiwyg.nsf"}]
}
```

3) Register Java addin in Domino environment (if you already have Addins there, keep in mind that separator is semicolon on Windows and colon on Linux) 

```
JAVAUSERCLASSES=.\JavaAddin\DbSigner.jar
```

# How to run DbSigner

```
load runjava net.prominic.DbSigner.App
```

