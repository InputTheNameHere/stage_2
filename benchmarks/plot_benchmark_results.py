import os
import pandas as pd
import matplotlib.pyplot as plt

CSV = "benchmarks/results/metrics.csv"
OUT = "benchmarks/results/plots"
os.makedirs(OUT, exist_ok=True)

df = pd.read_csv(CSV)
df["endpoint"] = df["endpoint"].str.strip()

index_df = df[df["endpoint"].str.contains("index")]
search_df = df[df["endpoint"].str.contains("search")]

# Indexing throughput
plt.figure()
plt.plot(index_df["threads"], index_df["throughput"], marker="o")
plt.xlabel("Threads")
plt.ylabel("Books per second")
plt.title("1. Indexing Throughput vs Concurrency")
plt.grid(True, alpha=0.3)
plt.savefig(os.path.join(OUT, "1_indexing_throughput.png"), dpi=160)

# Query latency
plt.figure()
plt.plot(search_df["threads"], search_df["avg_ms"], marker="o", label="Average")
plt.plot(search_df["threads"], search_df["p95_ms"], marker="^", label="p95")
plt.xlabel("Threads")
plt.ylabel("Latency (ms)")
plt.title("2. Query Latency vs Concurrency")
plt.legend()
plt.grid(True, alpha=0.3)
plt.savefig(os.path.join(OUT, "2_query_latency.png"), dpi=160)

# CPU usage
plt.figure()
plt.plot(df["threads"], df["cpu_pct"], marker="o", color="tab:red")
plt.xlabel("Threads")
plt.ylabel("CPU Usage (%)")
plt.title("3. CPU Utilization vs Concurrency")
plt.grid(True, alpha=0.3)
plt.savefig(os.path.join(OUT, "3_cpu_usage.png"), dpi=160)

# Memory usage
TOTAL_MEMORY_MB = 32768  # 32 GB reference server
df["mem_pct"] = (df["mem_mb"] / TOTAL_MEMORY_MB) * 100

plt.figure()
plt.plot(df["threads"], df["mem_pct"], marker="o", color="tab:green")
plt.xlabel("Threads")
plt.ylabel("Memory Usage (%) of 32 GB")
plt.title("4. Memory Utilization vs Concurrency")
plt.grid(True, alpha=0.3)
plt.savefig(os.path.join(OUT, "4_memory_usage_pct.png"), dpi=160)


# Scalability efficiency
base = index_df[index_df["threads"] == 1]["throughput"].values[0]
index_df["efficiency"] = (index_df["throughput"] / (base * index_df["threads"])) * 100

plt.figure()
plt.plot(index_df["threads"], index_df["efficiency"], marker="o", color="cornflowerblue")
plt.xlabel("Threads")
plt.ylabel("Efficiency (%)")
plt.title("5. Scalability Efficiency per Concurrency Level")
plt.grid(True, alpha=0.3)
plt.savefig(os.path.join(OUT, "5_scalability_efficiency_per_level.png"), dpi=160)

print(f"5 plots saved in {OUT}")
