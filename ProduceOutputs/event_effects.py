import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from matplotlib import rcParams
import matplotlib.ticker as mticker


def compute_change(rows_mat, begin_months, end_months, time_diff, T):
    # keep only months up to T months before eaach row
    bef_mask = np.repeat(((time_diff[begin_months] >= 0) | (time_diff[begin_months] < -T))[:, :, np.newaxis], rows_mat.shape[2], 
                             axis=2)
    # keep only months up to T months after each row
    aft_mask = np.repeat(((time_diff[end_months] <= 0) | (time_diff[end_months] > T))[:, :, np.newaxis], rows_mat.shape[2], 
                         axis=2)
    bef_data = np.nan_to_num(np.ma.masked_where(bef_mask, rows_mat[begin_months])[:, :, 2:])
    aft_data = np.nan_to_num(np.ma.masked_where(aft_mask, rows_mat[begin_months])[:, :, 2:])
    # compute sum over months for the data - before and after
    bef_sum = np.ma.median(bef_data, axis=1)
    bef_sum[bef_sum[:]==0] = 1
    aft_sum = np.ma.median(aft_data, axis=1)
    change = (aft_sum / bef_sum - 1) * 100 # change in totals
    return change


def compute_effects(df, cell_field, time_field, event_field, cluster_field, data_fields, labels, Ts, out_dir, titles, colors):
    control_data = [] #
    event_data = [] # store changes for event months
    for n in range(len(Ts)):
        T = Ts[n]
        control_data.append([]) #
        event_data.append([])
        for geom, rows in df.groupby(cell_field):
            if geom % 1000 == 0:
                print(geom, end=' ')
            # replicate rows for each row
            rows_mat = np.repeat(rows[[time_field, event_field] + data_fields].to_numpy()[np.newaxis, :, :], rows.shape[0], axis=0)
            # compute row level time difference
            time_diff = rows_mat[:, :, 0] - rows[time_field].to_numpy().reshape((rows.shape[0], 1))
            control = np.array(np.where((time_diff <= T) & (time_diff >= -T) & (time_diff != 0) & (rows_mat[:, :, 1] == 0))) #
            months_count = np.unique(control[0], return_counts=True) # 
            control_months = np.where((months_count[1] == T*2) & (rows.iloc[months_count[0]][event_field] == 0)) #
            change = compute_change(rows_mat, control_months, control_months, time_diff, T) # 
            change = np.append(rows.iloc[control_months[0]][time_field].to_numpy().reshape(len(control_months[0]), 1), change, axis=1).tolist() #
            control_data[-1].extend(change) #
            
            g_events = []
            init_idx = rows.index.min()
            # merge events of the same type taking place on consecutive months
            for idx, row in rows[rows[event_field]==1].iterrows():
                if g_events != [] and row[cluster_field] == g_events[-1][0] and row[time_field] - g_events[-1][2] == 1:
                    g_events[-1][2] = row[time_field]
                    g_events[-1][4] = idx - init_idx
                elif len(g_events) == 0 or row[time_field] - g_events[-1][2] > T:
                    g_events.append([row[cluster_field], row[time_field], row[time_field], idx - init_idx, idx - init_idx, 1])
                else:
                    g_events[-1][5] = 0
                    g_events.append([row[cluster_field], row[time_field], row[time_field], idx - init_idx, idx - init_idx, 0])
            
            for i in range(len(g_events)):
                if g_events[i][1] < rows[time_field].min()+T or g_events[i][2] > rows[time_field].max()-T:
                    g_events[i][5] = 0
                
            event_start = [e[3] for e in g_events if e[5]==1]
            if len(event_start) == 0:
                continue
            event_end = [e[4] for e in g_events if e[5]==1]
            event_types = [e[0] for e in g_events if e[5]==1]
            change = compute_change(rows_mat, event_start, event_end, time_diff, T)
            change = np.append(rows.iloc[event_start][time_field].to_numpy().reshape(len(event_start), 1), change, axis=1).tolist()
            change = [[event_types[i]] + change[i] for i in range(len(change))]
            event_data[-1].extend(change)
        print()
    
    
    rcParams['font.size'] = 12
    rcParams['font.family'] = 'Times New Roman'
    fig, axs = plt.subplots(figsize=(10, 10), nrows=int(len(data_fields)/2), ncols=2, sharex=True)
    for n in range(len(Ts)):
        control_df = pd.DataFrame(control_data[n], columns=[time_field] + data_fields).groupby(time_field).median() #
        event_df = pd.DataFrame(event_data[n], columns=[cluster_field, time_field] + data_fields)    
        change = (event_df.set_index([cluster_field, time_field]) - control_df).reset_index() #
        for i in range(len(data_fields)):
            row = int(i/2)
            col = i - row*2
            d = [change[change[cluster_field]==l][data_fields[i]] for l in labels]
            # d = [event_df[event_df[cluster_field]==l][data_fields[i]] for l in labels]
            bp = axs[row, col].boxplot(d, sym='', boxprops=dict(color=colors[n]), capprops=dict(color=colors[n]), 
                                       whiskerprops=dict(color=colors[n]), 
                                       positions=[k+n*2 for k in range(2, len(labels)*(len(Ts)+3), len(Ts)+3)],
                                       widths=1.7)
            axs[row, col].set_ylabel(titles[i])
            axs[row, col].set_xticklabels([l.replace(' ', '\n') for l in labels], rotation=-45, fontsize=10)
            axs[row, col].set_xticks([k+(len(Ts))/2 for k in range(2, len(labels)*(len(Ts)+3), len(Ts)+3)])
            axs[row, col].set_xlim(0, (len(Ts)-1)+len(labels)*(len(Ts)+3))
            axs[row, col].yaxis.set_major_formatter(mticker.PercentFormatter())
            if max(axs[row, col].get_yticks()) > 500:
                shift = 30
            else:
                shift = 10
            for line in bp['medians']:
                xy = line.get_xydata()
                axs[row, col].text((xy[1,0]+xy[0,0])/2, xy[0, 1]+shift, '%.1f' % xy[0, 1], horizontalalignment='center', fontsize=8)
            # for line in bp['boxes']:
            #     xy = line.get_xydata()
            #     axs[row, col].text((xy[1,0]+xy[0,0])/2-0.1, xy[0, 1]-shift*1.25, '%.1f' % xy[0, 1], horizontalalignment='right', fontsize=7)
            #     x,y = line.get_xydata()[3]
            #     axs[row, col].text((xy[1,0]+xy[0,0])/2-0.1, xy[2, 1]+shift, '%.1f' % xy[2, 1], horizontalalignment='right', fontsize=7)
            
    fig.tight_layout()
    fig.savefig(out_dir + '/event_effects.png', dpi=300)
    print()
