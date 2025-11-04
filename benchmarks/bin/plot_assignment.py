#!/usr/bin/env python3
import os
import math
import pandas as pd
import matplotlib.pyplot as plt

# Paths
ROOT = "benchmarks"
RES_DIR = os.path.join(ROOT, "results")
PLOTS = os.path.join(RES_DIR, "plots")
IM_CSV = os.path.join(RES_DIR, "integration_metrics.csv")   # from IntegrationLoadTest
RES_CSV = os.path.join(RES_DIR, "resources.csv")            # from sample_resources.sh

os.makedirs(PLOTS, exist_ok=True)

# Duration (seconds) of a single IntegrationLoadTest run
DURATION_SEC = int(os.environ.get("DURATION_SEC", "10"))

# === 1 & 2 & 5: Load integration_metrics.csv ===
im = pd.read_csv(IM_CSV)

# Ensure we have required columns in CSV
needed_cols = {"endpoint","concurrency","requests","p50_ms","p95_ms"}
missing = needed_cols - set(im.columns)
if missing:
    raise SystemExit(f"integration_metrics.csv is missing required columns: {missing}")

# Convert 'concurrency' to int (may come as string)
im["concurrency"] = pd.to_numeric(im["concurrency"], errors="coerce")
im = im.dropna(subset=["concurrency"])
im["concurrency"] = im["concurrency"].astype(int)

# 1) Indexing throughput (books/sec) — take POST /index/update/{id}
idx = im[im["endpoint"].str.contains(r"POST\s+/index/update", regex=True)].copy()
if not idx.empty:
    idx = idx.sort_values("concurrency")
    idx["throughput_bps"] = idx["requests"] / float(DURATION_SEC)

    plt.figure()
    plt.plot(idx["concurrency"], idx["throughput_bps"], marker="o")
    plt.title("Indexing throughput vs concurrency (books/sec)")
    plt.xlabel("Concurrency")
    plt.ylabel("Throughput (books/sec)")
    plt.grid(True, alpha=0.3)
    out1 = os.path.join(PLOTS, "1_indexing_throughput.png")
    plt.tight_layout()
    plt.savefig(out1, dpi=160)
    print("Saved:", out1)
    plt.close()
else:
    print("WARN: no rows found for POST /index/update — skipping plot (1).")

# 2) Query latency & concurrency — GET /search (p50, p95)
q = im[im["endpoint"].str.contains(r"GET\s+/search", regex=True)].copy()
if not q.empty:
    q = q.sort_values("concurrency")

    plt.figure()
    plt.plot(q["concurrency"], q["p50_ms"], marker="o", label="p50")
    plt.plot(q["concurrency"], q["p95_ms"], marker="o", label="p95")
    plt.title("Query latency vs concurrency (GET /search)")
    plt.xlabel("Concurrency")
    plt.ylabel("Latency (ms)")
    plt.grid(True, alpha=0.3)
    plt.legend(loc="best", frameon=False)
    out2 = os.path.join(PLOTS, "2_query_latency_vs_concurrency.png")
    plt.tight_layout()
    plt.savefig(out2, dpi=160)
    print("Saved:", out2)
    plt.close()
else:
    print("WARN: no rows found for GET /search — skipping plot (2).")

# 5) Scalability: GET /search throughput vs concurrency
if not q.empty:
    q_tp = q.copy()
    q_tp["throughput_rps"] = q_tp["requests"] / float(DURATION_SEC)

    plt.figure()
    plt.plot(q_tp["concurrency"], q_tp["throughput_rps"], marker="o")
    plt.title("Scalability: search throughput vs concurrency (GET /search)")
    plt.xlabel("Concurrency")
    plt.ylabel("Throughput (requests/sec)")
    plt.grid(True, alpha=0.3)
    out5 = os.path.join(PLOTS, "5_scalability_search_throughput.png")
    plt.tight_layout()
    plt.savefig(out5, dpi=160)
    print("Saved:", out5)
    plt.close()

# === 3 & 4: CPU/MEM from resources.csv ===
if os.path.exists(RES_CSV):
    res = pd.read_csv(RES_CSV)

    # Expected columns (same format as sample_resources.sh):
    # timestamp,pid,pcpu,pmem,rss_kb,command
    # Filter by 'java' to combine summary for all Java services (ingest/index/search)
    if "pcpu" in res.columns and "pmem" in res.columns:
        # Aggregation: sum CPU% and avg MEM% sequentially in time (if multiple PIDs at once)
        res["t"] = range(len(res))  # sample every 1s
        cpu_series = res.groupby("t")["pcpu"].sum()
        mem_series = res.groupby("t")["pmem"].mean()

        # 3) CPU over time
        plt.figure()
        plt.plot(cpu_series.index, cpu_series.values)
        plt.title("CPU usage over time")
        plt.xlabel("Sample (1s)")
        plt.ylabel("CPU %")
        plt.grid(True, alpha=0.3)
        out3 = os.path.join(PLOTS, "3_cpu_over_time.png")
        plt.tight_layout()
        plt.savefig(out3, dpi=160)
        print("Saved:", out3)
        plt.close()

        # 4) Memory over time
        plt.figure()
        plt.plot(mem_series.index, mem_series.values)
        plt.title("Memory usage over time")
        plt.xlabel("Sample (1s)")
        plt.ylabel("Memory %")
        plt.grid(True, alpha=0.3)
        out4 = os.path.join(PLOTS, "4_memory_over_time.png")
        plt.tight_layout()
        plt.savefig(out4, dpi=160)
        print("Saved:", out4)
        plt.close()
    else:
        print("WARN: resources.csv missing required columns 'pcpu'/'pmem' — skipping plots (3) and (4).")
else:
    print("WARN: benchmarks/results/resources.csv not found — skipping plots (3) and (4).")
