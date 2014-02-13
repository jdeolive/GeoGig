
.. _geogit-geojson-import:

geogit-geojson-import documentation
################################



SYNOPSIS
********
geogit geojson import <geojson> [<geojson>]... [--path <path>] [--fid-attrib <attrib_name>] [--add] [--alter]


DESCRIPTION
***********

This command imports features from one or more GeoJSON files into the GeoGit working tree.


OPTIONS
********

--path <path>                   The path to import to. If not specified, it uses the filename of the GeoJSON file.

--fid-attrib <attrib_name>      Uses the specified attribute as the feature id of each feature to import. If not used, a nummber indicating the position in the GeoJSON file is used

--add                           Adds the imported feature to the corresponding tree without removing previous features in case the tree already exists

--alter                         Same as the ``--add`` switch, but if the feature type of the imported features is different to that of the destination tree, the default feature type is changed and all previous features are modified to use that feature type


BUGS
****

Discussion is still open.

