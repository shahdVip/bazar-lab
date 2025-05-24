import matplotlib.pyplot as plt

labels = ["No-cache GET", "With-cache GET", "POST + invalidation"]
values = [6.97, 4.27, 13.75]

plt.bar(labels, values)
plt.ylabel("Avg Latency (ms)")
plt.xticks(rotation=20, ha="right")
plt.tight_layout()
plt.savefig("docs/results/latency_plot.png")
print("Saved plot to docs/results/latency_plot.png")
