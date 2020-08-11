import pandas as pd
import csv


def compute_event_prob(df, cell_field, cluster_field, time_field, labels, out_dir):
    merged_events = []
    for geom, rows in df.groupby(cell_field):
        g_events = []
        # merge events of the same type taking place on consecutive months
        for idx, row in rows.iterrows():
            if g_events != [] and row[cluster_field] == g_events[-1][0] and row[time_field] - g_events[-1][2] == 1:
                g_events[-1][2] = row[time_field]
            else:
                g_events.append([row[cluster_field], row[time_field], row[time_field]])
        merged_events.append(g_events)
    
    next_e = {g: {g2: 0 for g2 in ['count', 'No event'] + labels} for g in labels}
    for e in merged_events:
        first_event = e[0][0]
        next_e[first_event]['count'] += 1
        events_set = set([e[i][0] for i in range(1, len(e))])
        for l in events_set:
            next_e[first_event][l] += 1
    
    for e in merged_events:
        if len(e) == 1:
            next_e[e[0][0]]['No event'] += 1
    
    next_event = pd.DataFrame(next_e).transpose()
    next_event[labels + ['No event']] = (next_event[labels + ['No event']].divide(next_event['count'], axis=0) * 100).round(2)
    
    with open(out_dir+'/first_event_probabilities.csv', 'w', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(['First event type', '# events'] + labels + ['No event'])
        for l in labels:
            writer.writerow([l, next_event.loc[l, 'count']] + ['{per}%'.format(per=next_event.loc[l, l1]) for l1 in labels + ['No event']])
