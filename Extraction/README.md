# Data Collection

The EventFinder script: 
1. Configures a connection to an oshdb data file (local or on a cluster).
2. Executes a query used for collecting the data required for the event identification procedure, which include for each polygon and month:
    a. ID of the cell
    b. timestamp
    c. number of edit operations*
    d. number of active mappers
    e. number of contributions by type
    f. number of geometry operations
    g. number of tagging operationns
    h. number of operations by the most active mapper (maximal number of operations by one user)
3. Writes the output to a file

The process requires a file named oshdb.properties.bin to be placed in the target/classes folder (see example in the repository).
This file should include the following entries:
1. oshdb - path to the oshdb data file
2. keytables - path to the keytables file (probably the same as 1)
3. type - type of query (H2 for local/other for cluster)
4. bbox - bounding box for queries (list of four coordinates seperated by commas)
5. months_file - path to intermediate results file for query 1 (to be created/updated, see 6)
6. produce - boolean value specificing whether previous results exist (false) or whether they should be produced (true)
7. end_date - a timestamp entry for the database queries, specifying the end of the time range

* Edit operations count the number of estimated actions included in each contribution. For creations, this is no. nodes added + no. tags added. For deletions, this is just 1. For (tag/geometry) edits this is the no. tags added + no. tags removed + no. node added + np. nodes deleted. 
