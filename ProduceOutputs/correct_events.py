import scipy.stats as st

def correct(df, event_c, data_c):
    # set threshold below which an event is considered a false positive, using boxcox transformation
    event_bc, event_lamb = st.boxcox(df[df[event_c]==1][data_c])
    event_mean, event_std = event_bc.mean(), event_bc.std(ddof=1)
    lower_th = (event_lamb * (event_mean - 2 * event_std) + 1) ** (1 / event_lamb)
    
    # set threshold above which a non-event is considered a false negative, using boxcox transformation
    non_event_bc, non_event_lamb = st.boxcox(df[(df[event_c]==0) & (df[data_c]>0)][data_c])
    non_event_mean, non_event_std = non_event_bc.mean(), non_event_bc.std(ddof=1)
    upper_th = (non_event_lamb * (non_event_mean + 2 * non_event_std) + 1) ** (1 / non_event_lamb)
    
    # false positives to negatives; false negatives to positives
    df.loc[(df[event_c]==1) & (df[data_c] < lower_th), event_c] = 0
    df.loc[(df[event_c]==0) & (df[data_c] >= upper_th), event_c] = 1
    print(lower_th, upper_th)
    return df