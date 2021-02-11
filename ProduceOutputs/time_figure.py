import matplotlib.pyplot as plt
from matplotlib import rcParams
from pandas import isnull


def draw_time_figure(month_totals, event_totals, labels, out_dir):
    rcParams['font.size'] = 12
    rcParams['font.family'] = 'Times New Roman'
    
    event_weights = (event_totals * 100 / month_totals).round(2)
    event_weights = event_weights.unstack(level=1).transpose()
    event_weights[isnull(event_weights)]=0
    x = event_weights.index.date
    
    fig, ax = plt.subplots()
    ax.xaxis.set_major_locator(plt.MaxNLocator(5))
    ax.stackplot(x, [event_weights[label] for label in labels], labels=labels)
    ax.set_yticklabels([str(i)+'%' for i in ax.get_yticks().tolist()])
    ax.set_ylabel('% of all operations')
    ax.legend(ncol=2, fontsize=10)
    fig.savefig(out_dir + '/event_weights_share_over_time.svg', dpi=300)
    fig.savefig(out_dir + '/event_weights_share_over_time.png', dpi=300)
    