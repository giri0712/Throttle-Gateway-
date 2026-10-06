"""Generate ThrottleGate preliminary AWS architecture diagram (progress review)."""
import os

BASE = os.path.dirname(os.path.abspath(__file__))
os.chdir(BASE)

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch

fig, ax = plt.subplots(figsize=(14.6, 8.4))
ax.set_xlim(0, 14.6)
ax.set_ylim(0, 8.45)
ax.axis("off")


def box(x, y, w, h, title, lines, face, edge, fs=8.0):
    ax.add_patch(FancyBboxPatch((x, y), w, h,
                                boxstyle="round,pad=0.02,rounding_size=0.07",
                                linewidth=1.5, edgecolor=edge, facecolor=face, zorder=3))
    ax.text(x + w / 2, y + h - 0.28, title, ha="center", va="top",
            fontsize=fs + 1.2, fontweight="bold", color=edge, zorder=4)
    for i, ln in enumerate(lines):
        ax.text(x + w / 2, y + h - 0.62 - i * 0.27, ln, ha="center", va="top",
                fontsize=fs - 0.7, color="#263238", zorder=4)


def arrow(p1, p2, label=None, lx=0, ly=0, rad=0.0, dashed=False, fs=6.5, color="#455A64"):
    ax.add_patch(FancyArrowPatch(p1, p2, arrowstyle="-|>", mutation_scale=15,
                                 linewidth=1.3, color=color, zorder=2,
                                 linestyle="--" if dashed else "-",
                                 connectionstyle=f"arc3,rad={rad}"))
    if label:
        mx, my = (p1[0] + p2[0]) / 2 + lx, (p1[1] + p2[1]) / 2 + ly
        ax.text(mx, my, label, ha="center", va="center", fontsize=fs,
                style="italic", color="#37474F", zorder=5)


# ---------------- outer boundaries ----------------
ax.add_patch(FancyBboxPatch((2.7, 0.35), 11.6, 7.45,
                            boxstyle="round,pad=0.02,rounding_size=0.1",
                            linewidth=1.8, edgecolor="#232F3E", facecolor="#FAFBFC", zorder=1))
ax.text(2.95, 7.62, "AWS Cloud  (us-east-1)", fontsize=9.5, fontweight="bold", color="#232F3E")

ax.add_patch(FancyBboxPatch((3.0, 0.75), 6.0, 5.35,
                            boxstyle="round,pad=0.02,rounding_size=0.1",
                            linewidth=1.5, edgecolor="#8C4FFF", facecolor="#F7F3FF", zorder=1))
ax.text(3.15, 5.9, "VPC  10.0.0.0/16  +  Security Groups", fontsize=8.5,
        fontweight="bold", color="#6A2FD0")

# security-group rules caption inside the VPC
ax.text(6.0, 1.06, "Security groups:  22 = admin IP only  ·  80/443 = public (nginx)  ·  8080 = optional (raw API)",
        ha="center", fontsize=6.8, style="italic", color="#4A148C")
ax.text(6.0, 0.88, "5432 = EC2 → RDS only",
        ha="center", fontsize=6.8, style="italic", color="#4A148C")

# ---------------- service boxes ----------------
box(0.25, 3.1, 1.95, 2.0, "Clients",
    ["API consumers", "Dashboard users", "(browsers /", "downstream services)"],
    "#ECEFF1", "#546E7A")

box(3.3, 1.15, 2.6, 3.3, "Amazon EC2",
    ["t3.micro · AL2023", "",
     "nginx :80/:443", "→ React dashboard (static)",
     "Spring Boot API :8080", "(/v1/check, /api/metrics)",
     "Valkey :6379", "(atomic Lua scripts)"],
    "#FFF3E0", "#ED7100")

box(6.35, 1.45, 2.35, 2.0, "Amazon RDS",
    ["PostgreSQL", "db.t3.micro", "clients · endpoints ·", "tiers · rate-limit rules"],
    "#E8F0FE", "#3B5BDB")

box(9.45, 6.5, 4.6, 1.05, "AWS IAM",
    ["EC2 instance role (CloudWatch agent) · least-privilege policies · deployment user"],
    "#FDEBEE", "#DD344C")

box(9.45, 4.6, 4.6, 1.5, "Amazon S3",
    ["Dashboard build artifacts & bundles", "Static-hosting fallback (React build)"],
    "#EDF7EA", "#3F8624")

box(9.45, 2.6, 4.6, 1.8, "Amazon CloudWatch",
    ["Custom metrics: allowed / denied / RPS", "EC2 status checks", "Billing alarm ($10 budget)"],
    "#FDECF4", "#E7157B")

box(9.45, 0.95, 4.6, 1.4, "Amazon SNS",
    ["Email notifications for alarms", "(billing · instance health)"],
    "#FFF8E1", "#B8860B")

# ---------------- flows ----------------
arrow((2.2, 4.1), (3.3, 4.1))
ax.text(2.72, 4.46, "dashboard", ha="center", fontsize=6.2, style="italic", color="#37474F")
ax.text(2.72, 4.28, ":80/:443 · API :8080", ha="center", fontsize=6.2, style="italic", color="#37474F")

arrow((9.45, 5.35), (5.9, 4.2), rad=-0.1)
ax.text(7.7, 4.55, "deploy build artifacts", ha="center", fontsize=6.2, style="italic", color="#37474F")

arrow((5.9, 2.6), (6.35, 2.6))
ax.text(6.02, 2.6, "JDBC :5432", rotation=90, ha="center", va="center",
        fontsize=5.8, style="italic", color="#37474F")

arrow((5.9, 3.75), (9.45, 3.55), rad=-0.06)
ax.text(7.6, 3.9, "custom metrics (agent)", ha="center", fontsize=6.5, style="italic", color="#37474F")

arrow((11.75, 2.6), (11.75, 2.35))
ax.text(11.95, 2.44, "alarm → email", ha="left", fontsize=6.0, style="italic", color="#37474F")

arrow((9.45, 6.9), (5.9, 4.35), rad=0.15, dashed=True)
ax.text(8.0, 6.0, "instance profile (permissions)", ha="center", fontsize=6.5,
        style="italic", color="#37474F")

ax.set_title("ThrottleGate — Preliminary AWS Architecture (Progress Review)",
             fontsize=12, fontweight="bold", color="#232F3E", pad=12)

plt.savefig("aws_architecture_diagram.png", dpi=200, bbox_inches="tight",
            facecolor="white")
print("saved aws_architecture_diagram.png")
