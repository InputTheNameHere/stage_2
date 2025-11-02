#!/usr/bin/env python3
import os, pandas as pd, matplotlib.pyplot as plt

CSV = "benchmarks/results/resources.csv"   # zmień ścieżkę, jeśli masz inną
OUT = "benchmarks/results/plots"
os.makedirs(OUT, exist_ok=True)

df = pd.read_csv(CSV)  # oczekiwane kolumny: timestamp,pid,pcpu,pmem,rss_kb,command

# Jeśli znasz PID-y usług, wpisz je tutaj (albo zostaw None, a skrypt weźmie top 3 "command"):
PID_INGEST = None
PID_INDEX  = None
PID_SEARCH = None

groups, labels = [], []
if all(x is not None for x in (PID_INGEST, PID_INDEX, PID_SEARCH)):
    for pid, label in [(PID_INGEST,"ingest"), (PID_INDEX,"index"), (PID_SEARCH,"search")]:
        g = df[df['pid']==pid].copy()
        if not g.empty:
            groups.append(g)
            labels.append(label)
else:
    # fallback: wybierz 3 najczęstsze procesy po 'command'
    for cmd in df['command'].value_counts().head(3).index.tolist():
        g = df[df['command']==cmd].copy()
        if not g.empty:
            groups.append(g)
            labels.append(cmd)

# CPU over time
plt.figure()
for g,lab in zip(groups, labels):
    plt.plot(range(len(g)), g['pcpu'], label=lab)
plt.xlabel("Sample (1s)")
plt.ylabel("CPU %")
plt.title("CPU usage over time")
plt.legend(); plt.grid(True, alpha=0.3)
plt.savefig(os.path.join(OUT,"cpu_over_time.png"), dpi=160, bbox_inches="tight")

# MEM over time (proc %)
plt.figure()
for g,lab in zip(groups, labels):
    plt.plot(range(len(g)), g['pmem'], label=lab)
plt.xlabel("Sample (1s)")
plt.ylabel("Memory %")
plt.title("Memory usage over time")
plt.legend(); plt.grid(True, alpha=0.3)
plt.savefig(os.path.join(OUT,"mem_over_time.png"), dpi=160, bbox_inches="tight")

print("Saved plots to", OUT)
