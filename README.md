# OSM Events

Procedures for data extraction, analysis, and visualization for the paper 
“An Analysis of the Spatial and Temporal Distribution of Large-Scale Data Production Events in OpenStreetMap” (in press).

This repository consists of three sections:
1. A Java [OSHDB](https://heigit.org/big-spatial-data-analytics-en/ohsome/) query which extracts various data regarding OpenStreetMap edits at a monthly temporal resolution for each cell within a custom grid. This can be found under the [Extraction](https://github.com/GIScience/OSM_Events/tree/main/Extraction) folder. The output of this procedure is a csv file named 'months_results', stored under Extraction\Target, which includes the data for each cell and month combination (a zipped version of this file is stored under [Outputs](https://github.com/GIScience/OSM_Events/tree/main/Outputs)). For more details see the [README](https://github.com/GIScience/OSM_Events/blob/main/Extraction/README.md) file for this section.
2. A procedure, written in R (see [fit_curve.R](https://github.com/GIScience/OSM_Events/blob/main/CurveFitting/fit_curve.R) stored within the CurveFitting folder), for fitting logistic curves to the data available for each cell within the 'months_results' file. This procedure produces a unique file for each cell within the custom grid, stored within the Predictions subdirectory of the Outputs folder. These files contain the observed (number of contribution operations) and expected values for each month, later used to analyze patterns and produce outputs.
3. A collection of Python scripts using the 'months_results' file and the files in the predictions folder to produce the outputs and visualizations used in the paper (stored under [ProduceOutputs](https://github.com/GIScience/OSM_Events/blob/main/ProduceOutputs)). The main script is [process_events.py](https://github.com/GIScience/OSM_Events/blob/main/ProduceOutputs/process_events.py), which calls functions for:
    1. Identifying events, computing normalized RMSE values per cell, and visualizing these ([identify_events.py](https://github.com/GIScience/OSM_Events/blob/main/ProduceOutputs/identify_events.py))
    2. Correcting the product of the above by identifying outliers ([correct_events.py](https://github.com/GIScience/OSM_Events/blob/main/ProduceOutputs/correct_events.py))
    3. Creating a boxplot figure describing the distribution of cells by events frequencies and sizes ([boxplot_figure.py](https://github.com/GIScience/OSM_Events/blob/main/ProduceOutputs/boxplot_figure.py))
    4. Identifying clusters of events and labeling them ([cluster.py](https://github.com/GIScience/OSM_Events/blob/main/ProduceOutputs/cluster.py))
    5. Creating a table characterizing each cluster ([events_table.py](https://github.com/GIScience/OSM_Events/blob/main/ProduceOutputs/events_table.py))
    6. Creating a table analyzing the share of all contribution operations attributed to each type of event ([weights_table.py](https://github.com/GIScience/OSM_Events/blob/main/ProduceOutputs/weights_table.py))
    7. Creating a figure depicting the change over time in the share of operations attributed to each event type out of all operations ([time_figure.py](https://github.com/GIScience/OSM_Events/blob/main/ProduceOutputs/time_figure.py))
    8. Creating a figure depicting the effects of events - the change in activity following in event relative to non-event periods ([event_effects.py](https://github.com/GIScience/OSM_Events/blob/main/ProduceOutputs/event_effects.py))
    9. Creating a table depicting the frequency with which the first event in a cell is a followed by events of all type, by type of the first event ([next_events.py](https://github.com/GIScience/OSM_Events/blob/main/ProduceOutputs/next_events.py))

    This main script also processes and cleans the data, and [produces a file containing the weight of each event type for each cell](https://github.com/GIScience/OSM_Events/blob/main/ProduceOutputs/process_events.py#L82-L90), later used to produce static and dynamic online maps. The outputs of all of the above are stored under the Outputs folder.

The [branch `gh-pages`](https://github.com/GIScience/OSM_Events/tree/gh-pages) contains an interactive visualization of the detected events, which can be accessed at the following link: https://giscience.github.io/OSM_Events/
