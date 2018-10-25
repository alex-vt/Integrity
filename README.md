# Integrity

![Logo](app/src/main/res/mipmap-xxxhdpi/ic_launcher.png)

Personal data archiving, search & analysis software.

An Android app concept.

### Idea

Information we care about comes from various websites,
services or other sources that can add, update or even
lose some of data.
Bringing diverse personal data together to preserve,
view and manage fluently can be a non trivial problem.

The proposed solution is software for maintaining
heterogeneous user data integrity by
automated periodic acquiring and archiving,
providing tools for viewing, searching and
analysing, and managing metadata.

Building the solution implies implementation of
the following **principles**:

* The entirety of user's collectible data from
a source corresponding to a topic or domain forms an
`artifact`. *Examples: a blog, a social media account.*
* Data from a source and collected at a given time is
a `snapshot`. *Example: a blog as it was at 1:20 PM.*
* Every artifact is the collection of all its
snapshots.
* Snapshots consist of `snapshot data` that is a
collection of files containing the collected user
data *(such as web archives of blog front page and
article pages linked from it)*,
and `snapshot metadata` (ID, name, artifact type,
timestamp, snapshot data location, and type specific
data *(such as blog URL)*).
* Snapshot data is stored in archives in local
and / or remote filesystem. Metadata is included in
snapshots for redundancy, verification
and recoverability.
* Snapshot metadata is stored in a database for
integrity maintenance and for enabling `actions`
on data.
* Basic actions on any type of artifact are
automated and / or manual `acquiring` from source,
and `viewing` the stored snapshots.
* A universal, type independent action is `searching`.
It can be also implemented additionally (more options)
for a type.
* An action that gives user various
insights of their data
(independently or dependently on type) is `analysis`.
* Actions on some standard data types,
as well as type independent search and analysis,
are modules within the base software.
* A basic, versatile artifact type can be considered
a `web blog type`: a web page and the pages linked on it,
within the same domain
(these contain blog articles). Regular snapshots of
such a blog can ensure all its content is saved
over time.
* Other type dependent actions corresponding with 3rd
party services, apps, etc. are implemented
separately as 3rd party modules.
* The base software has open source code in order to
ensure transparency of managing user data.
* Integrity of data takes advantage of being
non stop, user friendly, on the go - just joyful.
Therefore this software is (primarily) a mobile app.


### App structure summary

Data structure:

```
artifact
|
+--snapshot
   |
   +--metadata
   |  |
   |  +--id
   |  |
   |  +--name
   |  |
   |  +--description
   |  |
   |  +--type
   |  |
   |  +--timestamp
   |  |
   |  +--data_paths
   |  |
   |  +--type_specific
   |
   +--data
```


Actions on data:

* acquire
* view
* search
* analysis


Modular structure:

* App basics: main app installer (APK)
* Web blog module: built in main app
* 3rd party service acquiring / viewing / search /
analysis modules: separate app installers (APK)


Web blog module data actions:

* Acquire: web page and its linked pages within the
same domain.
* View: saved web pages and linked pages.
* Search: not included.
* Analysis: not included.


Storage destinations:

* Local filesystem
* Local network (Samba)
* Dropbox


### License

*Not defined yet. TODO*
