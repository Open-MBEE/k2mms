# k2mms
connects kservices with mms

The util, bae, and kservices projects should checked out in the same directory
as this project. 

In addition, set up the MMS Java Client in the same parent directory as those above.
A zip file may be provided in this repository.  While the MMS API does not change
often, it is possible that this included java client may not match the API of your 
MMS server.

If necessary, a matching java client can be generated from the mms repository 
from the swagger API.  The mms readme may give brief instructions near the end.

A link to the client may still be on the MMS coverpage in the OpenCAE View Editor.

If a zip file is included in this repository (such as mms-java-client-3.3.0.zip),
unzip it in the parent directory of this project and from a terminal inside the
new mms-java-client project directory, build with
    mvn clean package

Then, from this project's directory also build with 
    mvn clean package
