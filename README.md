# DbSigner
Java addin that signs databases

Register DbSigner Java Addin by adding it to notes.ini

```
JAVAUSERCLASSES=.\JavaAddin\DbSigner.jar
```

Make sure there is a file dbsigner.json in Domino executable folder 

```
{
"databases": [{"filepath": "test1.nsf"}, {"filepath": "test2.nsf"}, {"filepath": "C:\IBM\Domino\data\A55F0D\wysiwyg.nsf"}]
}
```

Run Java Addin

```
load runjava net.prominic.DbSigner.App
```
