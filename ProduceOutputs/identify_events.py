from os import listdir
import numpy as np
import scipy.stats as st
import pandas as pd
import matplotlib.pyplot as plt
from matplotlib import rcParams

def identify(in_dir, out_dir):
    outlier_threshold = st.norm.ppf(0.99) # threshold for events - 99% significance
    fs = listdir(in_dir) # directory containing files for each cell with the predictions of the curve fitting procedure
    events = pd.DataFrame(columns=['GeomID', 'date', 'event']) # dataframe to contain events
    rmses = []
    for fi in fs:
        df = pd.read_csv(in_dir + '/' + fi)
        df.sort_values(by=['time_double'])
        # convert timestamp to datetime
        df['date'] = pd.to_datetime(df.time_double, unit='s').apply(lambda x:x.replace(hour=0)) + pd.Timedelta(days=1) 
        df['error'] = df.contributions - df.pred # compute prediction errors
        df['squared_error'] = df.error**2
        df['lagged_error'] = df.error - df.error.shift() # compute lagged errors
        
        df['norm_lagged_error'] = (df.lagged_error - df.lagged_error.mean()) / df.lagged_error.std(ddof=1) # normalize errors
    
        df['GeomID'] = int(float(fi[1:-4]))
        df['event'] = 0
        df.loc[df.norm_lagged_error > outlier_threshold, 'event'] = 1
        events = events.append(df[df.event == 1][['GeomID', 'date', 'event']]) # add to events dataframe
        
        mean_se = df.squared_error.mean()
        rmse = mean_se**0.5
        nrmse = rmse / df.contributions.median()
        rmses.append(nrmse)
    
    rcParams['font.size'] = 12
    rcParams['font.family'] = 'Times New Roman'
    fig, ax = plt.subplots()
    ax.set_yscale('log')
    ax.hist(rmses, bins=100)
    ax.set_ylabel('Frequency')
    ax.set_xlabel('NRMSE')
    fig.savefig(out_dir +'/rmse.svg', dpi=300)
    fig.savefig(out_dir +'/rmse.png', dpi=300)
    return events, rmses