import os
import pandas as pd
import matplotlib.pyplot as plt

df = pd.read_csv("benchmarks/results/metrics.csv")
os.makedirs("benchmarks/results/plots", exist_ok=True)

# Indexing throughput
plt.figure()
plt.plot(df["threads"], df["throughput"], marker="o")
plt.title("1. Indexing Throughput vs Concurrency")
plt.xlabel("Threads")
plt.ylabel("Books per second")
plt.grid(True, alpha=0.3)
plt.savefig("benchmarks/results/plots/1_indexing_throughput.png", dpi=160)

# Query latency
plt.figure()
plt.plot(df["threads"], df["latency_avg"], marker="o", label="Average")
plt.plot(df["threads"], df["latency_p95"], marker="^", label="p95")
plt.title("2. Query Latency vs Concurrency")
plt.xlabel("Threads")
plt.ylabel("Latency (ms)")
plt.legend()
plt.grid(True, alpha=0.3)
plt.savefig("benchmarks/results/plots/2_query_latency.png", dpi=160)

# CPU usage
plt.figure()
plt.plot(df["threads"], df["cpu"], marker="o", color="r")
plt.title("3. CPU Usage vs Concurrency")
plt.xlabel("Threads")
plt.ylabel("CPU (%)")
plt.grid(True, alpha=0.3)
plt.savefig("benchmarks/results/plots/3_cpu_usage.png", dpi=160)

# Memory usage as %
TOTAL_MEM_MB = 32768
df["mem_pct"] = (df["mem_mb"] / TOTAL_MEM_MB) * 100
plt.figure()
plt.plot(df["threads"], df["mem_pct"], marker="o", color="g")
plt.title("4. Memory Usage vs Concurrency")
plt.xlabel("Threads")
plt.ylabel("Memory (%) of 32 GB")
plt.grid(True, alpha=0.3)
plt.savefig("benchmarks/results/plots/4_memory_usage.png", dpi=160)


