import csv


def create_events_table(df, cluster_c, data_c, non_p, out_dir):
    measures = df[[cluster_c] + data_c].groupby(cluster_c).agg(['mean', 'std'])
    measures = measures.join(df.groupby(cluster_c)[cluster_c].count(), on=cluster_c)
    
    with open(out_dir + '/events_characteristics.csv', 'w', newline='') as f:
        writer = csv.writer(f)
        row = ['Cluster', 'Number of events'] + data_c + ['Name']
        writer.writerow(row)
        i = 1
        for idx, val in measures.iterrows():
            row = [i, int(val[cluster_c])]
            for d in data_c:
                if d not in non_p:
                    row.append('{mean}%\n({std}%)'.format(mean=(val[(d, 'mean')]*100).round(2), std=(val[(d, 'std')]*100).round(2)))
                else:
                    row.append('{mean}\n({std})'.format(mean=(val[(d, 'mean')]).round(2), std=(val[(d, 'std')]).round(2)))
            row += [idx]
            writer.writerow(row)
            i += 1