import matplotlib.pyplot as plt
from matplotlib import rcParams
import numpy as np


def boxplot(df, group_c, data_c):
    rcParams['font.family'] = 'Times New Roman'
    rcParams['font.size'] = 12
    
    events_per_cell = df.groupby(group_c).agg({group_c:'size'}).rename(columns={group_c:'count'})
    df = df.join(events_per_cell, on=[group_c])
    fig, axs = plt.subplots(1, 2, figsize=(10,5))
    axs[1].boxplot([df[df['count']==e][data_c] for e in np.arange(df.min()['count'], df.max()['count']+1)], sym='')
    ax1 = axs[1].twinx()
    ax1.plot(df.groupby('count').size(), c='blue', label='Cells frequency')
    ax1.set_ylabel('Number of cells')
    axs[1].set_xlabel('Number of events per cell')
    axs[1].set_ylabel('Event size (contribution operations)')
    axs[1].set_title('(b)')
    axs[1].set_xticklabels([i if i%3==0 else '' for i in range(1,47)])
    ax1.legend(loc=1)
    
    total_contributions = df.groupby(group_c)[data_c].sum()
    df = df.join(total_contributions, on=group_c, rsuffix='_total')
    axs[0].boxplot([np.array(df[df['count']==e].groupby(group_c).agg({data_c+'_total':'mean'})) 
                    for e in np.arange(df.min()['count'], df.max()['count']+1)], sym='')
    ax0 = axs[0].twinx()
    l = ax0.plot(df.groupby('count').size(), c='blue', label='Cells frequency')
    ax0.set_ylabel('Number of cells')
    axs[0].set_xlabel('Number of events per cell')
    axs[0].set_ylabel('Data size per cell (contribution operations)')
    axs[0].set_title('(a)')
    ax0.legend(loc=1)
    axs[0].set_xticklabels([i if i%3==0 else '' for i in range(1,47)])

    fig.tight_layout()
    fig.savefig('outputs/events_numbers_means.svg', dpi=300)
    fig.savefig('outputs/events_numbers_means.png', dpi=300)
    del fig, axs