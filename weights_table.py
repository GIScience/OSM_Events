import csv


def create_weights_table(df, totals, label_field, data_fields, out_dir):
    events_sum = df.groupby(label_field)[data_fields].sum()
    total_sum = events_sum.sum()
    weights = (events_sum * 100 / totals).round(2)
    total_weights = (total_sum * 100 / totals).round(2)
    
    with open(out_dir + '/events_weights.csv', 'w', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(['Cluster'] + data_fields)
        writer.writerow(['All events'] + ['{x}%'.format(x=x) for x in total_weights])
        for idx, val in weights.iterrows():
            writer.writerow([idx] + ['{x}%'.format(x=x) for x in val])
        writer.writerow(['All contributions (mil.)'] + (totals / 1000000).round(2).to_list())
        
        
    