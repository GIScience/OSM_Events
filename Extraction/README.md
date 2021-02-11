# Data Collection

The EventFinder script: 
1. Configures a connection to an oshdb data file (local or on a cluster).
2. Executes a query used for collecting the data required for the event identification procedure, which include for each polygon and month:
    1. ID of the cell
    2. timestamp
    3. number of edit operations<sup>[1](#myfootnote1)</sup>
    4. number of active mappers
    5. number of contributions by type
    6. number of geometry operations
    7. number of tagging operationns
    8. number of operations by the most active mapper (maximal number of operations by one user)
3. Writes the output to a file

The process requires a file named oshdb.properties.bin to be placed in the src/main/resources folder at build time (see example-oshdb.properties in the repository).
This file should include the following entries:
1. oshdb - path to the oshdb data file
2. keytables - path to the keytables file (probably the same as 1)
3. type - type of query (H2 for local/other for cluster)
4. bbox - bounding box for queries (list of four coordinates seperated by commas)
5. months_file - path to intermediate results file for query 1 (to be created/updated, see 6)
6. end_date - a timestamp entry for the database queries, specifying the end of the time range



<a name="myfootnote1">1</a>: *Edit operations count the number of estimated actions included in each contribution. For creations, this is no. nodes added + no. tags added. For deletions, this is just 1. For (tag/geometry) edits this is the no. tags added + no. tags removed + no. node added + np. nodes deleted.*
