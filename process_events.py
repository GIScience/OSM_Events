import pandas as pd
import numpy as np
from identify_events import identify
from correct_events import correct
from boxplot_figure import boxplot
from cluster import cluster_label
from events_table import create_events_table
from weights_table import create_weights_table
from time_figure import draw_time_figure
from event_effects import compute_effects
from next_events import compute_event_prob

print('Identifying events...')
events, rmses = identify('data/predictions', 'outputs') # identify events and produce NRMSE (Figure 2)

print('Processing data...')
months_df = pd.read_csv('data/eventident.output.csv') # OSHDB output - data for each cell and month
months_df['date'] = pd.to_datetime(months_df['date']) # convert date to datetime
months_df = pd.merge(months_df, events, on=['GeomID', 'date'], how='outer') # join events
months_df.event = months_df.event.replace(np.nan, 0) # set non events to 0
# find for each cell the first row in which contributions were recorded and delete all previous rows
first_t = months_df.contributions.ne(0).groupby(months_df.GeomID).idxmax()
months_df = months_df.merge(first_t, on='GeomID', how='left', suffixes=('', '_y'))
months_df = months_df[months_df.index.values>=months_df.contributions_y]
months_df = months_df.drop('contributions_y', axis=1)
# compute timestep - months since first contribution to each cell
first_d = months_df.groupby('GeomID').date.min()
months_df = months_df.merge(first_d, on='GeomID', how='left', suffixes=('', '_y'))
months_df['timestep'] = ((months_df.date  - months_df.date_y) / np.timedelta64(1, 'M')).round(0) + 1
months_df = months_df.drop('date_y', axis=1)
months_df = months_df[months_df.date>pd.to_datetime('2007-10-01')] # remove data prior to the October 2007 introduction of v0.5 of the API
del first_t, first_d

print('Correcting for false negatives/positives...')
months_df = correct(months_df, 'event', 'contributions') # correct for false negative and false positives

# produce data for clustering
print('Processing data for clustering...')
tot_cont_types = months_df.groupby(
    ['GeomID', 'date'])[['creations', 'deletions', 'tagChanges', 'GeometryChanges']].sum().sum(axis=1).reset_index(name='Total')
months_df = pd.merge(months_df, tot_cont_types, on=['GeomID', 'date'])
del tot_cont_types
months_df['Creation_ratio'] = months_df.creations / months_df.Total
months_df['Deletion_ratio'] = months_df.deletions / months_df.Total
months_df['Tag_ratio'] = months_df.tagChanges / months_df.Total
months_df['Geom_ratio'] = months_df.GeometryChanges / months_df.Total
months_df['Max_ratio'] = months_df.max_cont / months_df.contributions

events = months_df[months_df.event==1]

print('Drawing event frequencies figure...')
boxplot(events, 'GeomID', 'contributions') # produce boxplot figure for number of events per cell (Figure 3)

# cluster events
print('Clustering events...')
months_df, labels = cluster_label(months_df, 6, 'event', ['Creation_ratio', 'Deletion_ratio', 'Tag_ratio', 'Geom_ratio', 'Max_ratio'])
labels.sort(key = lambda x:x[-6:] + x[0])
events = months_df[months_df.event==1]

# create table with clusters' means and stds (Table 1)
print('Creating cluster charateristics table...')
create_events_table(events, 'label', ['Creation_ratio', 'Deletion_ratio', 'Tag_ratio', 'Geom_ratio', 
                                        'Max_ratio', 'users', 'timestep'], ['timestep', 'users'], 'outputs')

# create table of events clusters' weights out of all contributions (Table 2)
print('Creating event weights table...')
totals = months_df[['contributions', 'creations', 'deletions', 'tagChanges', 'GeometryChanges']].sum()
create_weights_table(events, totals, 'label', ['contributions', 'creations', 'deletions', 'tagChanges', 'GeometryChanges'], 'outputs')
del totals

# create figure of event weights by month (Figure 4)
print('Creating cluster weights over time figure...')
month_totals = months_df.groupby('date').contributions.sum()
event_totals = events.groupby(['label', 'date']).contributions.sum()
draw_time_figure(month_totals, event_totals, labels, 'outputs')
del month_totals, event_totals

# create data for event weight maps (Figure 5)
print('Creating cluster weights by cell table...')
events_by_cell = events.groupby(['GeomID', 'label']).contributions.sum().unstack(level=1)
events_by_cell[pd.isnull(events_by_cell)] = 0
cont_by_cell = months_df.groupby('GeomID').contributions.sum()
events_by_cell = events_by_cell.merge(cont_by_cell, on='GeomID')
event_weights_by_cell = (events_by_cell[labels].div(events_by_cell.contributions, axis=0) * 100).round(2)
event_weights_by_cell.to_csv('outputs/event_weights_by_cell.csv')
del events_by_cell, cont_by_cell, event_weights_by_cell

# event effects
print('Computing event effects...')
compute_effects(months_df, 'GeomID', 'timestep', 'event', 'label',
                ['contributions', 'creations', 'deletions', 'tagChanges', 'GeometryChanges', 'users'],
                labels, [6, 12], 'outputs', ['Operations', 'Creations', 'Deletions', 'Tag Changes', 'Geom. Changes', 'Users'],
                ['k', 'grey'])

# event effects on subsequent events
print('Computing initial event effects on the probability for subsequent events...')
compute_event_prob(events, 'GeomID', 'label', 'timestep', labels, 'outputs')

print('Done.')