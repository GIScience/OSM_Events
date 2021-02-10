# Event Identification

Identifying large scale events in OSM

Procedure:
1. Query 1: Using a custom grid, count the monthly total number of contribution actions (node + tags changes) for each cell
2. Event identification:
    a. Create an accumulative time series data for each polygon
    b. Fit a logistic curve to each time series
    c. Compute errors and normalize
    d. Identify positive and significant errors as events
    e. For each event save output (number of users, contributions, contributions by types, etc.)
3. Query 2: for each event, count the number of edited entities during that month
4. Produce outputs:
    a. Compute the number of geometry and tag actions per entity per event and add to output
    b. Write output to file

The process requires a file named oshdb.properties.bin to be placed in the target/classes folder.
This file should include the following entries:
1. oshdb - path to the oshdb data file
2. keytables - path to the keytables file (probably the same as 1)
3. type - type of query (H2 for local/other for cluster)
4. bbox - bounding box for queries (list of four coordinates seperated by commas)
5. months_file - path to intermediate results file for query 1 (to be created/updated, see 6)
6. produce - boolean value specificing whether previous results exist (false) or whether they should be produced (true)
7. end_date - a timestamp entry for the database queries, specifying the end of the time range
8. follow_up - path to intermediate results file for query 2 