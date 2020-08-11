from sklearn.cluster import KMeans
import numpy as np


def cluster_label(df, k, event_field, data_fields):
    cluster = KMeans(k).fit(df[df[event_field]==1][data_fields])
    print(data_fields)
    print(cluster.cluster_centers_)
    
    # name clusters
    g_labels = {}
    for i in range(k):
        g_labels[i] = input('Please enter group '+str(i)+' label ')
    
    # add labels and group names to dataframe
    df['cluster'] = np.nan
    df['label'] = np.nan
    df.loc[df[event_field]==1, 'cluster'] = cluster.labels_
    df.loc[df[event_field]==1, 'label'] = df[df[event_field]==1].cluster.map(g_labels)
    return df, list(g_labels.values())
    