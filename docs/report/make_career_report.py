"""Generate the Java Backend Career Research Report (India, October 2026) as a .docx file.

Creates only its own output file; does not modify any existing project files.
Run:  python make_career_report.py
"""
import os

BASE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(BASE, "Java_Backend_Career_Report_India_Oct2026.docx")

from docx import Document
from docx.shared import Pt, Inches, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT

NAVY = RGBColor(0x23, 0x2F, 0x3E)
ORANGE = RGBColor(0xED, 0x71, 0x00)
GREEN = RGBColor(0x2E, 0x7D, 0x32)

doc = Document()

style = doc.styles["Normal"]
style.font.name = "Calibri"
style.font.size = Pt(10.5)


def h1(text):
    p = doc.add_heading(text, level=1)
    for r in p.runs:
        r.font.color.rgb = NAVY
        r.font.size = Pt(15)
    return p


def h2(text):
    p = doc.add_heading(text, level=2)
    for r in p.runs:
        r.font.color.rgb = ORANGE
        r.font.size = Pt(12.5)
    return p


def h3(text):
    p = doc.add_heading(text, level=3)
    for r in p.runs:
        r.font.color.rgb = NAVY
        r.font.size = Pt(11.5)
    return p


def body(text, bold=False, italic=False, size=10.5, space_after=6):
    p = doc.add_paragraph()
    r = p.add_run(text)
    r.bold = bold
    r.italic = italic
    r.font.size = Pt(size)
    p.paragraph_format.space_after = Pt(space_after)
    return p


def bullets(items):
    for it in items:
        p = doc.add_paragraph(style="List Bullet")
        if isinstance(it, tuple):
            r = p.add_run(it[0])
            r.bold = True
            p.add_run(it[1])
        else:
            p.add_run(it)
        p.paragraph_format.space_after = Pt(2)


def table(headers, rows, widths=None, font_size=9):
    t = doc.add_table(rows=1, cols=len(headers))
    t.style = "Table Grid"
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    hdr = t.rows[0].cells
    for i, h in enumerate(headers):
        hdr[i].text = ""
        r = hdr[i].paragraphs[0].add_run(h)
        r.bold = True
        r.font.size = Pt(font_size)
    for row in rows:
        cells = t.add_row().cells
        for i, val in enumerate(row):
            cells[i].text = ""
            r = cells[i].paragraphs[0].add_run(str(val))
            r.font.size = Pt(font_size)
    if widths:
        for i, w in enumerate(widths):
            for row in t.rows:
                row.cells[i].width = Inches(w)
    return t


# ================= TITLE =================
for _ in range(3):
    doc.add_paragraph()
t = doc.add_paragraph()
t.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = t.add_run("JAVA BACKEND CAREER RESEARCH REPORT — INDIA")
r.bold = True
r.font.size = Pt(22)
r.font.color.rgb = NAVY

sub = doc.add_paragraph()
sub.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = sub.add_run("Targeting first Java Backend / SDE-1 role | Off-campus | CGPA ~8 | Target ≥ ₹12 LPA")
r.font.size = Pt(12)
r.font.color.rgb = ORANGE

date = doc.add_paragraph()
date.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = date.add_run("Research current as of October 5, 2026")
r.italic = True
r.font.size = Pt(10)
doc.add_page_break()

# ================= 1. EXECUTIVE SUMMARY =================
h1("1. Executive Summary")
bullets([
    ("Target (≥ ₹12 LPA off-campus as fresher) is achievable but competitive. ",
     "October 2026 market data shows IT services pay freshers ₹3.4–6.5 LPA while product companies and GCCs pay ₹12–30 LPA."),
    ("The ₹12+ LPA fresher band comes from three buckets: ",
     "bank GCCs (Goldman Sachs, JPMorgan, Deutsche Bank, Amex), product MNCs (Amazon, Microsoft, Walmart, Cisco, Oracle, PayPal, Salesforce), and funded Indian product/fintech firms (PhonePe, Flipkart, Zepto, Razorpay, Dream11, Meesho, Games24x7)."),
    ("Java is the best positioning. ",
     "Java + Spring Boot remains the most common backend stack in Indian hiring: bank GCCs, fintech, e-commerce and IT services all run Java-first backends."),
    ("Realistic strategy: ",
     "~60% of applications to Tier-2 realistic targets (Walmart, JPMorgan, Deutsche Bank, Amex, Oracle, Cisco, Razorpay, Zepto) and ~40% to stretch targets (Amazon, Microsoft, Goldman Sachs, Flipkart, Atlassian, Uber)."),
    ("CGPA 8 clears academic cutoffs ",
     "at nearly every product company (7.0–7.5 typical). HFT/quant firms (DE Shaw, Graviton, Tower) mostly want 9+ and are not realistic primary targets."),
    ("Internship → PPO is the highest-probability backdoor ",
     "into ₹20+ LPA firms (Microsoft, Atlassian, Amazon, Flipkart, Goldman Sachs) — pursue in parallel with off-campus applications."),
    ("A deployed backend project (e.g., ThrottleGate rate limiter) ",
     "is genuinely strong resume material for this exact market."),
])
body("Salary figures are labelled [R] = reported (Levels.fyi / AmbitionBox / Glassdoor / posted offers) or [E] = estimated. "
     "Where reliable fresher data is unavailable, this is stated explicitly.", italic=True, size=9.5)

# ================= 2. TOP 30 =================
h1("2. Top 30 Target Companies (Ranked)")
body("Scoring: Compensation 25 · Java/backend relevance 20 · Fresher accessibility 15 · Engineering quality 15 · "
     "Career growth 10 · Brand 5 · Learning 5 · Hiring frequency 5 = /100.", italic=True, size=9.5)

top30 = [
    (1, "Amazon", "SDE-1", "24–30 [R]", "30–45", "Yes", "High", "Med", "High", "AWS", "Very High", 88),
    (2, "PhonePe", "SDE-1", "20–28 [R]", "28–40", "Yes", "High", "High", "High", "AWS", "High", 86),
    (3, "Microsoft", "SWE (L59)", "23–28 [R]", "30–45", "Yes", "Med", "Med", "High", "Azure", "Very High", 84),
    (4, "Atlassian", "Graduate SWE", "~30–45 [E]", "40–60", "Yes", "Very High", "High", "High", "AWS/GCP", "Very High", 83),
    (5, "Goldman Sachs", "SW Engineering Analyst", "20–28 [R]", "25–40", "Yes", "Very High", "High", "High", "AWS", "High", 82),
    (6, "Flipkart", "SDE-1", "20–25 [R]", "28–45", "Yes", "Very High", "High", "High", "AWS", "High", 80),
    (7, "Uber", "SWE-1", "28–40 [E]", "35–55", "Yes", "High", "Med", "High", "GCP", "Very High", 79),
    (8, "Walmart Global Tech", "SDE / Assoc. SWE", "18–25 [R/E]", "24–35", "Yes", "Very High", "High", "High", "Azure/AWS", "Medium", 78),
    (9, "JPMorgan Chase", "Software Engineer Program", "12–18 [E]", "18–28", "Yes", "Very High", "High", "High", "AWS", "Medium", 76),
    (10, "Deutsche Bank", "Graduate Analyst / Assoc. Eng.", "14–21 [R]", "20–30", "Yes", "Very High", "High", "High", "AWS", "Medium", 75),
    (11, "American Express", "Engineer I (Band 30)", "16–24 [R]", "22–32", "Yes", "High", "High", "High", "AWS", "Medium", 74),
    (12, "Cisco", "Software Engineer I", "16–24 [E]", "22–35", "Yes", "High", "Med", "High", "AWS", "Medium", 73),
    (13, "Oracle", "SDE / Member Tech Staff", "15–25 [E/R]", "25–40", "Yes", "Very High", "Med", "High", "OCI", "Medium", 73),
    (14, "PayPal", "SDE-1", "18–28 [E]", "25–40", "Yes", "Very High", "High", "High", "AWS", "High", 72),
    (15, "Salesforce", "AMTS", "18–28 [E]", "28–45", "Yes", "High", "Med", "High", "AWS", "High", 71),
    (16, "ServiceNow", "Software Engineer", "20–28 [E]", "25–40", "Yes", "Very High", "High", "High", "GCP/AWS", "High", 70),
    (17, "Adobe", "MTS-1", "22–32 [E]", "30–45", "Yes", "Med", "Low", "Med", "AWS/Azure", "Very High", 70),
    (18, "Nutanix", "MTS-1", "25–32 [E]", "30–45", "Yes", "High", "Med", "High", "AWS", "High", 69),
    (19, "Mastercard", "Software Engineer", "12–18 [E]", "18–28", "Yes", "High", "High", "High", "GCP", "Medium", 68),
    (20, "Zepto", "SDE-1", "22–34 [R]", "28–45", "Yes", "High", "High", "High", "AWS", "Medium", 67),
    (21, "Meesho", "SDE-1", "20–30 [R]", "28–45", "Yes", "High", "High", "High", "AWS", "High", 66),
    (22, "Razorpay", "SDE-1", "18–25 [R]", "25–40", "Yes", "Very High", "High", "High", "AWS", "High", 66),
    (23, "Media.net", "SWE-1", "18–22 [R]", "25–40", "Yes", "High", "Med", "High", "AWS", "High", 65),
    (24, "Sprinklr", "SSE / Engineer", "18–28 [E]", "25–40", "Yes", "Very High", "High", "High", "AWS", "High", 64),
    (25, "Optum (UHG)", "Associate Software Engineer", "8–13 [E]", "15–25", "Yes", "High", "High", "High", "Azure", "Medium", 64),
    (26, "Dream11", "SDE-1", "22–30 [R]", "30–45", "Occasional", "Very High", "High", "High", "AWS", "High", 63),
    (27, "Navi", "SDE-1", "18–25 [E]", "25–40", "Occasional", "Very High", "High", "High", "AWS", "High", 62),
    (28, "Games24x7", "SDE-1", "20–25 [R]", "25–35", "Yes", "Very High", "High", "High", "AWS", "Medium", 61),
    (29, "Swiggy", "SDE-1", "18–25 [E]", "25–40", "Yes", "High", "High", "High", "AWS", "High", 61),
    (30, "Zoho", "Member Tech Staff", "4–10 [R]", "10–18", "Yes", "Very High", "High", "High", "Own DC", "Low", 60),
]
table(
    ["#", "Company", "Entry Role", "Fresher TC (LPA)", "1–3 YOE", "Intern", "Java", "Spring", "SQL", "Cloud", "Difficulty", "Score"],
    [list(map(str, row)) for row in top30],
    widths=[0.3, 1.15, 1.0, 0.85, 0.6, 0.55, 0.55, 0.55, 0.4, 0.7, 0.75, 0.45],
)
body("Note: Zoho scores high on accessibility (off-campus friendly, Java-first) but its fresher pay (₹4–10 LPA) misses the "
     "₹12 LPA objective — treat it as a high-probability safety, not a target.", italic=True, size=9.5)

h2("Why the Top 10")
bullets([
    ("1. Amazon (88): ", "Highest fresher TC in the mainstream list (Levels.fyi Bengaluru SDE-1 median ₹27.97L [R]; Dec 2025 offer: 19L base + 6.5L joining + 1.5L relocation ≈ 27L year-1 [R]). Java runs through many core services. Off-campus hiring is real but needs strong DSA (medium-hard, 2 rounds)."),
    ("2. PhonePe (86): ", "Best blend of pay and Java relevance reachable from tier-2/3 colleges. AmbitionBox SWE ₹24.7–28.5L [R]. Backend is Java/Spring + AWS at payments scale — exactly this profile."),
    ("3. Microsoft (84): ", "Reported 2026 new-grad offer: ₹18.05L base + ₹4.5L signing + ~$35K stock [R] → ~₹23–27L year-1. Java isn't its primary stack (C#); DSA + OOP + SQL transfer."),
    ("4. Atlassian (83): ", "Most Java-first top payer in India (Jira/Confluence/Bitbucket = Java). Reliable fresher TC data unavailable for India; est. ₹35–45L [E]. Very low off-campus intake — target via internship or referrals."),
    ("5. Goldman Sachs (82): ", "Engineering-heavy India centres with Java/Spring core. AmbitionBox Software Analyst ₹24.9–28.8L [R] vs older estimates ₹11–21L [conflicting — treat ₹18–25L realistic]. India internships since 2013."),
    ("6. Flipkart (80): ", "India's classic Java/Spring shop; Levels.fyi SDE-1 median ₹22.57L [R]. Off-campus exists (Runway/GT programs + direct reqs) but very competitive."),
    ("7. Uber (79): ", "Top-tier comp [E ₹28–40L; reliable fresher data unavailable], Java used in many backend services. Small fresher intake; apply via referrals."),
    ("8. Walmart Global Tech (78): ", "Most accessible high-payer: large India org (Bengaluru/Chennai), Java/Spring heavy, regular 0–2 YOE reqs, ₹18–25L [R/E]. Best score-to-difficulty ratio."),
    ("9. JPMorgan Chase (76): ", "Huge Java backend org (Bengaluru/Hyderabad/Mumbai). Entry ~₹12–18L [E]. Structured Software Engineer Program with a defined off-campus route."),
    ("10. Deutsche Bank (75): ", "Graduate Programme (technology) is a genuine off-campus route; Engineer ₹14.4–21L [R]. Java/Spring dominant. Apply via careers.db.com."),
])

# ================= 3. CLASSIFICATION =================
h1("3. Fresher Classification")
h2("A. Realistic for freshers (apply aggressively)")
body("Regular graduate/0–1 YOE hiring, defined entry programs, or volume hiring; off-campus friendly.")
bullets(["Zoho (highest probability), Optum, JPMorgan, Deutsche Bank, Walmart Global Tech, Oracle, Cisco, American Express, "
         "Mastercard, Games24x7, Zepto, Navi, Razorpay, Media.net, TCS / Infosys / Cognizant / Accenture / LTIMindtree (safety net, ₹3.4–6.5 LPA)."])
h2("B. Competitive but worth applying")
body("Freshers do get in (including off-campus), but intake is small, the bar is high, and roles open irregularly.")
bullets(["Amazon, Microsoft, Goldman Sachs, Flipkart, PhonePe, Meesho, PayPal, Salesforce, ServiceNow, Sprinklr, Dream11, Swiggy, "
         "Atlassian, Nutanix, Uber, Groww, Freshworks, BrowserStack, Postman, Zerodha, CRED, Juspay, Arcesium, DE Shaw, Media.net."])
h2("C. Mostly experienced (1–3+ YOE)")
body("Primarily lateral hiring; entry only via internship or rare grad programs.")
bullets(["DE Shaw, Graviton, Quadeye, Tower Research (quant, 9+ CGPA + competitive programming); Databricks, Stripe, Rubrik, Confluent, "
         "Snowflake (India GCCs, mostly experienced); Zerodha (tiny team); ZS Associates, Fractal, Tiger Analytics, EPAM, Publicis Sapient, "
         "Thoughtworks (some fresher intakes, mostly 1–3 YOE)."])

# ================= 4. COMPENSATION =================
h1("4. Compensation Research (realistic, not advertised max)")
comp = [
    ("Amazon SDE-1", "~₹19L", "~₹6.5L joining + ~₹1.5L relocation", "~₹1.5–2L/yr (backloaded RSUs)", "~₹27L yr1, ~₹24L yr2", "[R]"),
    ("Microsoft L59", "~₹18L", "~₹4.5L signing", "~$35K/4yr", "~₹23–27L", "[R]"),
    ("PhonePe", "~₹20–22L", "~₹2L", "ESOPs small", "~₹21–28L", "[R]"),
    ("Goldman Sachs", "~₹18–22L", "performance bonus", "minimal at entry", "~₹20–28L", "[R, conflicting]"),
    ("Flipkart SDE-1", "~₹19L", "~₹1.5L", "~₹2L", "~₹20–25L", "[R]"),
    ("Dream11 SDE-1", "~₹28L", "~₹5.6L joining", "₹10L+", "~₹30–43L", "[R]"),
    ("Zepto SDE-1", "~₹22–28L", "~₹1–2L", "ESOPs", "~₹22–34L", "[R]"),
    ("Games24x7 SDE-1", "~₹19–22L", "~₹1–2L", "ESOPs", "~₹20–25L", "[R]"),
    ("Deutsche Bank", "~₹14–18L", "bonus", "minimal", "~₹14–21L", "[R]"),
    ("American Express", "~₹15–20L", "bonus", "ESPP", "~₹16–24L", "[R]"),
    ("Walmart Global Tech", "~₹16–20L", "~₹1–2L", "RSUs", "~₹18–25L", "[R/E]"),
    ("Razorpay", "~₹16–20L", "~₹1L", "ESOPs", "~₹18–25L", "[R]"),
    ("Meesho", "~₹22–28L", "bonus", "ESOPs", "~₹20–30L", "[R, wide spread]"),
    ("Media.net SWE-1", "~₹18L", "bonus", "minimal", "~₹18–22L", "[R]"),
    ("Oracle", "~₹14–20L", "bonus", "RSU", "~₹15–25L", "[E/R]"),
    ("JPMorgan", "~₹11–15L", "~₹1L", "minimal", "~₹12–18L", "[E]"),
    ("PayPal", "~₹15–20L", "~₹1–2L", "RSU", "~₹18–28L", "[E]"),
    ("Salesforce AMTS", "~₹15L", "~₹1.5L", "~$13K RSU", "~₹20–28L", "[R/E]"),
    ("Atlassian", "—", "—", "RSUs heavy", "Reliable fresher data unavailable (est. ₹35–45L)", "[E]"),
    ("Nutanix", "—", "—", "—", "Reliable fresher data unavailable (est. ₹25–32L)", "[E]"),
    ("Uber", "—", "—", "—", "Reliable fresher data unavailable (est. ₹28–40L)", "[E]"),
    ("Zoho", "~₹4–8L", "~₹1–2L", "none", "~₹4–10L", "[R]"),
    ("TCS / Infosys / LTIMindtree", "₹3.4–4.5L", "~₹0.5L", "none", "~₹3.4–6.5L", "[R]"),
]
table(["Company / Role", "Base", "Variable/Bonus", "Stock", "Approx. Year-1 TC", "Basis"],
      [list(row) for row in comp], widths=[1.3, 0.9, 1.4, 1.0, 1.6, 0.7])
body("Honesty notes: Instagram/YouTube fresher-package reels frequently inflate CRED/PayPal/Adobe numbers — un-cross-checkable "
     "claims were excluded. Bank GCC bonuses vary year to year. 1–3 YOE figures are estimates from Levels.fyi/AmbitionBox bands.", italic=True, size=9.5)

# ================= 5. CURRENT OPENINGS =================
h1("5. Current Openings (verified live October 2026)")
body("Openings close fast. These were live at research time (Oct 5, 2026); always confirm on the official page before applying.", italic=True, size=9.5)
openings = [
    ("Binocs.co", "SDE-I", "Bengaluru", "0 YOE", "Backend, APIs", "₹10–15L", "wellfound.com/jobs (Binocs) — live 1 week ago [R]"),
    ("RD&X Network", "Backend Engineer (Java)", "Gurgaon", "~1 YOE", "Java, backend", "₹10–15L", "wellfound.com/jobs — live 5 days ago [R]"),
    ("PhonePe", "Engineering (multiple, incl. backend)", "Bengaluru/Pune", "0–2 YOE", "Java, Spring, AWS", "n/a", "phonepe.com/careers → Current Openings"),
    ("Razorpay", "Engineering roles (fresher-friendly)", "Bengaluru", "Fresher+", "Java/Spring", "n/a", "razorpay.com/careers"),
    ("Goldman Sachs", "Engineering + India internship program", "Bengaluru/Hyderabad", "Students/Freshers", "Java, CS fundamentals", "n/a", "goldmansachs.com/worldwide/india/careers"),
    ("JPMorgan Chase", "Software Engineer Program (full-time)", "India hubs", "Freshers", "Java, OOP, SQL", "n/a", "jpmorganchase.com/careers → SWE Program"),
    ("Deutsche Bank", "Graduate Programme (Technology)", "Pune/Bengaluru", "Freshers", "Java, SQL", "n/a", "careers.db.com/students-graduates/graduate-programme"),
    ("Wells Fargo", "India Early Careers (intern + FT)", "Bengaluru/Hyderabad", "Freshers", "Java, SQL", "n/a", "wellsfargojobs.com → India Programs"),
    ("Walmart Global Tech", "SDE roles India", "Bengaluru/Chennai", "Fresher–2 YOE", "Java, Spring", "n/a", "tech.walmart.com/careers"),
    ("American Express", "Technology roles (incl. SE-1)", "Gurugram/Bengaluru", "Fresher+", "Java, REST", "n/a", "careers.americanexpress.com"),
    ("TCS", "NQT / entry-level cycle (2026 grads)", "Pan-India", "Freshers", "Java, coding", "₹3.36–7L by track", "tcs.com/careers/india/entry-level"),
    ("Cognizant", "GenC / GenC Next", "Pan-India", "Freshers", "Java, SQL", "₹4–6.75L", "careers.cognizant.com → GenC Program"),
    ("Infosys", "Graduate hiring (SE / Power Programmer)", "Pan-India", "Freshers", "Java, DSA", "₹3.6–9L", "infosys.com/careers/graduates.html"),
    ("Qualcomm", "Internships & early career India", "Bengaluru/Hyderabad/Chennai", "Students", "Java, CS", "stipend", "qualcomm.com/careers → India internships"),
    ("Zoho", "Software Developer (live req IDs)", "Chennai/Tenkasi", "Fresher–3 YOE", "Java, C/C++", "₹4–10L", "zoho.com/careers"),
]
table(["Company", "Role", "Location", "Experience", "Key Skills", "Salary", "Official Application"],
      [list(row) for row in openings], widths=[0.95, 1.15, 1.0, 0.75, 0.9, 0.7, 1.75])
body("Also monitor weekly: naukri.com/java-fresher-jobs (11,399 Java fresher listings live in Oct 2026), "
     "wellfound.com/role/l/java-backend-developer/india (64 live), internshala.com/fresher-jobs/java-jobs, "
     "linkedin.com/jobs (filter: Java + 0–1 years + your cities), productbased.in/jobs/backend.", italic=True, size=9.5)

# ================= 6. TECH ANALYSIS =================
h1("6. Technology Analysis (frequency in recent JDs)")
h2("Must Have (70–95% of Java backend fresher JDs)")
bullets(["Java (OOP, Collections, Exception Handling) — universal", "SQL (MySQL/PostgreSQL) — universal",
         "DSA / problem solving — screens every product/GCC application", "Git/GitHub — universal",
         "REST APIs — near-universal", "Spring Boot — dominant framework in fintech/banking/e-commerce JDs"])
h2("Important (30–60% of JDs; learn months 3–6)")
bullets(["Spring (Core, MVC, Security basics)", "JPA/Hibernate", "JDBC (fundamentals under ORM)",
         "Multithreading / concurrency (asked in interviews even for freshers)", "Linux basics + shell", "Docker",
         "CI/CD concepts (Jenkins/GitHub Actions)", "AWS basics (EC2/S3/RDS) — Azure at Microsoft/Optum/Mastercard, OCI at Oracle"])
h2("Nice to Have (10–30%; learn month 6+)")
bullets(["Microservices architecture concepts", "Redis (caching)", "Kafka (event streaming — big at fintech/e-comm but rarely tested on freshers)",
         "MongoDB", "Kubernetes"])
h2("Learn Later (do not touch before month 8)")
bullets(["Advanced Kubernetes ops, Istio / service mesh", "Kafka internals, stream processing",
         "Observability stacks (Prometheus/Grafana), advanced system design patterns", "Azure/GCP depth (pick one cloud; AWS preferred in India)"])

# ================= 7. CAREER CATEGORIES =================
h1("7. Career Categories")
h2("Highest realistic compensation (₹15+ LPA achievable early)")
body("Atlassian, Uber, Amazon, Microsoft, Adobe, Nutanix, Dream11, Zepto, Meesho, PhonePe, Goldman Sachs, Flipkart, ServiceNow, "
     "Salesforce, PayPal. (HFT/quant pays ₹50L+ but is not realistic for this profile.)")
h2("Strong ₹8–15 LPA opportunities with realistic entry routes")
body("Walmart Global Tech, JPMorgan, Deutsche Bank, Amex, Mastercard, Oracle, Cisco, Optum (upper band), Razorpay (lower band), "
     "Media.net, Games24x7, Freshworks, Zoho (top performers), ZS/Tiger/Fractal (analytics-leaning), EPAM, Publicis Sapient.")
h2("Strong ₹5–10 LPA starting points with good backend growth")
body("Zoho, Freshworks, Optum (lower band), Persistent, Nagarro, Cybage, Mphasis, Virtusa, HCLTech, Capgemini (Analyst track), "
     "Cognizant GenC Next. Growth path: 3–4 yrs in → switch to product companies / GCCs at ₹20–35 LPA.")
h2("Best by category")
bullets([
    ("Best product companies: ", "Flipkart, Zepto, Meesho, Swiggy, Dream11, PhonePe, Navi, Groww."),
    ("Best fintech/banking: ", "PhonePe, Razorpay, Groww, Navi (fintech) · Goldman Sachs, JPMorgan, Deutsche Bank, Amex, Mastercard, Wells Fargo (banking GCC)."),
    ("Best SaaS: ", "Zoho, Freshworks, Postman, BrowserStack, Sprinklr, Chargebee, Whatfix, MoEngage."),
    ("Best Java + Spring Boot companies: ", "PhonePe, Razorpay, Flipkart, Goldman Sachs, JPMorgan, Deutsche Bank, Walmart, Atlassian, Optum, Games24x7, Dream11, Zoho."),
    ("Best internship targets: ", "Microsoft, Amazon, Atlassian, Flipkart, Goldman Sachs, Qualcomm, Walmart, Adobe (apply Aug–Dec 2026 for Summer 2027)."),
    ("Best for backend engineering growth: ", "PhonePe, Flipkart, Amazon, Walmart, Uber, Goldman Sachs, Zepto."),
])

# ================= 8. INTERVIEW PATTERNS =================
h1("8. Hiring & Interview Patterns")
bullets([
    ("Campus: ", "Goldman Sachs, Microsoft, Amazon, Flipkart, Adobe run structured campus programs (mostly IIT/NIT/top private). As an off-campus candidate you compete via open drives and direct reqs."),
    ("Off-campus: ", "Product companies (Amazon, Walmart, Oracle, Cisco, PhonePe, Zepto, Razorpay, Meesho, JPMorgan, Deutsche Bank, Amex) post 0–1 YOE reqs year-round on official portals; startups hire via LinkedIn/Wellfound/naukri constantly."),
    ("Referrals: ", "Highest-leverage for Amazon, Microsoft, Uber, Atlassian, Flipkart, Adobe, Goldman Sachs, PhonePe. Get referrals from alumni on LinkedIn — with 8 CGPA and no brand name, a referral is often the difference."),
    ("Internships: ", "Goldman Sachs India (12-week, since 2013), Microsoft (SWE intern), Amazon, Flipkart (intern→PPO common), Qualcomm, Walmart. PPO conversion at Flipkart/Atlassian/Amazon is well-documented."),
    ("Typical rounds (product/GCC): ", "1) Online assessment (2–3 DSA problems, 90 min); 2) 1–2 DSA rounds (arrays/strings/DP/graphs); 3) technical round on Java internals (HashMap, concurrent collections), OOP, SQL, Spring Boot basics, project deep-dive; 4) (bank GCCs) cognitive/behavioral round; 5) HR/compensation."),
    ("DSA difficulty: ", "Amazon/Microsoft/Flipkart/Uber/Atlassian: LeetCode medium-heavy. Bank GCCs/Walmart/Oracle/Cisco: medium. Startups: mediums + strong Spring Boot discussion. Zoho: low-level programming (pattern/number problems, no IDE)."),
    ("Java/backend depth for freshers: ", "Collections internals, equals/hashCode, concurrency basics (synchronized, ExecutorService), JDBC vs JPA, transaction management, @Transactional pitfalls, REST design, SQL joins/indexes. System design is light for freshers (URL shortener / rate limiter / booking API)."),
    ("IT services (TCS NQT / Infosys / GenC / Capgemini): ", "aptitude + basic coding; cycles announced on official career pages. These are the safety net, not the goal."),
])

# ================= 9. STRATEGY =================
h1("9. Job Search Strategy")
h2("Apply immediately (Oct 2026)")
body("Zoho, Cognizant GenC, TCS NQT (next cycle), Infosys, Accenture, Optum, Mastercard, Oracle, Cisco, Amex, JPMorgan, "
     "Deutsche Bank, Wells Fargo, Walmart Global Tech, Games24x7, Razorpay, Zepto, Media.net, Binocs (live SDE-I ₹10–15L), "
     "plus every 0–1 YOE Java req on naukri/Wellfound matching the stack.")
h2("Target after ~1 year experience")
body("Swiggy, Dream11 (mostly 1–3 YOE intake), Adobe, Uber, Atlassian, ServiceNow, Salesforce, PayPal, Sprinklr, CRED, Postman, "
     "BrowserStack, EPAM, Publicis Sapient, Thoughtworks — at 1–2 YOE with strong Java/Spring + DSA these open up dramatically at ₹25–40L.")
h2("Internship targets (before graduation)")
body("Microsoft, Amazon, Goldman Sachs, Flipkart, Qualcomm, Walmart, Atlassian, Adobe. Apply Aug–Dec for Summer 2027.")
h2("Referral priority")
body("Amazon, Microsoft, Uber, Atlassian, Flipkart, Adobe, Goldman Sachs, PhonePe. Start LinkedIn outreach in Month 4 (once projects exist).")
h2("DSA-heavy targets")
body("Amazon, Microsoft, Uber, Atlassian, Flipkart, Adobe, Goldman Sachs (moderate), Media.net, Arcesium/DE Shaw (extreme — skip unless competitive programming is already strong).")
h2("Java/Spring-heavy targets")
body("PhonePe, Razorpay, Flipkart, Goldman Sachs, JPMorgan, Deutsche Bank, Walmart, Optum, Games24x7, Dream11, Navi, Zoho, Sprinklr, Mastercard.")
h2("Off-campus targets (interviews genuinely obtainable without campus)")
body("Walmart, JPMorgan, Deutsche Bank, Amex, Oracle, Cisco, Optum, Mastercard, Razorpay, Zepto, Games24x7, Media.net, Zoho, "
     "plus startup reqs on Wellfound/naukri. Big tech off-campus (Amazon/Microsoft) is possible but needs referral + strong OA scores.")

# ================= 10. SKILL MATRIX =================
h1("10. Skill Gap Analysis (Java backend specific)")
skills = [
    ("Java (core + OOP)", "Must Have", "Every JD; every interview", "Expert: collections internals, generics, exceptions, streams"),
    ("DSA", "Must Have", "OA screens at all product/GCC", "250–350 LeetCode; mediums timed ≤25 min"),
    ("SQL", "Must Have", "Universal; asked with joins/indexes", "Query design, joins, indexes, transactions"),
    ("Spring Boot", "Must Have", "Default backend framework in Indian JDs", "Build 2–3 real REST APIs with auth, validation, tests"),
    ("REST APIs", "Must Have", "In nearly every backend JD", "Design versioned, paginated, documented APIs"),
    ("Git/GitHub", "Must Have", "Table stakes", "Branching, PRs, conflicts"),
    ("JPA/Hibernate", "Must Have", "Default persistence in Spring JDs", "Entities, relations, N+1, lazy loading"),
    ("Collections (deep)", "Must Have", "#1 interview topic in Java", "HashMap/ConcurrentHashMap internals"),
    ("Exception handling", "Must Have", "Asked everywhere", "Custom exceptions, global handlers"),
    ("Multithreading", "Important", "Asked even for freshers at product cos", "Executors, synchronized, volatile, ConcurrentHashMap"),
    ("JDBC", "Important", "Foundation + asked at banks/legacy", "CRUD + connection pooling"),
    ("MySQL/PostgreSQL", "Must Have", "Default relational DBs", "Schema design, indexing, EXPLAIN"),
    ("Docker", "Important", "In ~30–50% of backend JDs", "Containerize the Spring Boot app"),
    ("Linux", "Important", "Dev environments everywhere", "Shell, processes, logs, SSH"),
    ("AWS", "Important", "Default cloud in India", "EC2/S3/RDS — deploy one project"),
    ("CI/CD", "Nice to Have", "Mentioned increasingly", "GitHub Actions pipeline for a project"),
    ("MongoDB", "Nice to Have", "~15–25% of JDs", "Basic CRUD via Spring Data"),
    ("Microservices", "Nice to Have", "Concept questions for freshers", "Monolith vs MS; build a 2-service demo"),
    ("Redis", "Nice to Have", "Caching questions at fintech", "Cache-aside pattern in a project"),
    ("Kafka", "Learn Later", "Rarely tested on freshers", "Basics after placement or on the job"),
    ("System Design", "Learn Later (until M6)", "Light at fresher level", "HLD basics in months 7–9"),
]
table(["Skill", "Priority", "Why", "Target Level"], [list(s) for s in skills],
      widths=[1.35, 1.0, 2.0, 2.4])
h2("Minimum employable stack (month 6 checkpoint)")
body("Java core + DSA (150+ problems) + SQL + Spring Boot + JPA + REST + Git + one deployed project + Docker basics. "
     "That is genuinely enough for Zoho-tier, IT-digital, Optum, Mastercard, Games24x7 and startup roles. "
     "Everything else raises the ceiling toward PhonePe / Amazon / Walmart.")

# ================= 11. ROADMAP =================
h1("11. 6–12 Month Roadmap")
h2("Months 1–2 — Foundations")
bullets(["Learn: Core Java (OOP, Collections, Exceptions, Strings, Generics), DSA basics (arrays, strings, hashing, two pointers, stacks/queues), SQL (joins, group by, subqueries).",
         "Build: 3 SQL practice sets/week; 80–100 easy LeetCode.",
         "Practice: 2 DSA problems/day minimum.",
         "Apply: Nothing yet — but register for TCS NQT / Cognizant GenC cycles so eligibility windows aren't missed."])
h2("Months 3–4 — Backend + first proof of work")
bullets(["Learn: Spring Boot, REST, JPA/Hibernate, validation, Spring Security basics, unit testing (JUnit/Mockito), Git workflow.",
         "Build: Project 1 — e.g., a bookings/payments API; a ThrottleGate-style rate limiter is a great resume project for backend roles (polish, test, deploy). MySQL or PostgreSQL.",
         "Practice: 150 problems total; start mediums.",
         "Apply: Start applying (Months 3–4): Zoho, IT services cycles, Optum, Mastercard, startup fresher reqs on naukri/Wellfound/Internshala.",
         "Referrals: Set up LinkedIn (project links + clean profile); don't mass-DM yet."])
h2("Months 5–6 — Advanced backend + cloud")
bullets(["Learn: Multithreading/concurrency, Docker, AWS basics (deploy Project 1 on EC2+RDS), Redis caching, CI via GitHub Actions.",
         "Build: Project 2 — microservices-flavored (2 services + API gateway + Redis queue) or extend Project 1 with caching + auth + Docker Compose.",
         "Practice: 250 problems total; mock interviews (Pramp/peers).",
         "Apply: Full off-campus push — Walmart, JPMorgan, Deutsche Bank, Amex, Oracle, Cisco, Razorpay, Zepto, Games24x7, Media.net. Expect 100–200 applications across months 4–9; conversion is 1–3%.",
         "Referrals: Start referrals (Month 5+): 5–10 polite alumni asks/week for Amazon/Microsoft/Flipkart/Goldman Sachs/PhonePe/Atlassian."])
h2("Months 7–12 — Interview prep + premium targets")
bullets(["Learn: System design basics (scaling, caching, queues, sharding — fresher depth only), Kafka basics, optional Kubernetes exposure.",
         "Practice: 300–350 problems; company-tagged lists (Amazon/Microsoft/Flipkart); 1 mock/week; Java interview question banks (collections/concurrency/Spring).",
         "Apply: Premium targets (Months 7–12): Amazon, Microsoft, Goldman Sachs, Flipkart, Atlassian, Uber, Adobe — once OA confidence is proven.",
         "Milestones: aim for first offer by month 8–10. If below 20% OA-pass rate at month 6, extend DSA by 6 weeks before adding premium targets."])

# ================= 12. TOP 15 SHORTLIST =================
h1("12. Top 15 Personal Target Companies")
body("Balanced across compensation + realistic fresher access + Java relevance + engineering quality + growth — not pure salary.", italic=True, size=9.5)
shortlist = [
    (1, "PhonePe", "Java/Spring at real payments scale; ₹20–28L [R]; genuine fresher/off-campus intake", "Medium", "20–28", "Java, Spring, SQL, AWS, DSA mediums", "Very High"),
    (2, "Amazon", "Highest mainstream fresher TC (~₹27L yr1 [R]); Java services; real off-campus SDE-1", "Low-Med", "24–30", "DSA (medium-hard), Java, SQL", "Very High"),
    (3, "Walmart Global Tech", "Best access-to-pay ratio; Java-heavy; large India org; regular 0–2 YOE reqs", "Medium-High", "18–25", "Java, Spring, SQL, DSA mediums", "Very High"),
    (4, "Goldman Sachs", "Java/Spring core; ₹20–28L [R]; India internships + off-campus engineering hiring", "Medium", "18–28", "Java, DSA, SQL, CS fundamentals", "High"),
    (5, "Flipkart", "India's flagship Java product shop; ₹22L median [R]; brand + growth", "Low-Med", "20–25", "DSA, Java, Spring, SQL", "High"),
    (6, "JPMorgan Chase", "Massive Java org; structured SWE Program; ₹12–18L [E]; stable, accessible", "Medium-High", "12–18", "Java, OOP, SQL, basic DSA", "High"),
    (7, "Deutsche Bank", "Graduate Programme = clean off-campus route; ₹14–21L [R]", "Medium", "14–21", "Java, Spring, SQL", "High"),
    (8, "American Express", "₹16–24L [R]; SE-1 reqs appear regularly; Gurugram/Bengaluru", "Medium", "16–24", "Java, REST, SQL", "Medium-High"),
    (9, "Oracle", "Java's home; large fresher intake; ₹15–25L", "Medium", "15–25", "Core Java, SQL, DSA mediums", "Medium-High"),
    (10, "Cisco", "Strong pay + historically large fresher classes; ₹16–24L [E]", "Medium", "16–24", "Java/C++, DSA, networking basics", "Medium"),
    (11, "Razorpay", "Fintech, Java/Spring-heavy, ₹18–25L [R]; active careers page", "Low-Med", "15–25", "Java, Spring, SQL, Redis", "Medium"),
    (12, "Zepto", "Fastest fresher hiring loop in e-comm now; ₹22–34L [R]", "Medium", "22–34", "Java, Spring, SQL", "Medium"),
    (13, "Meesho", "Strong backend eng, ₹20–30L [R]; fresher roles drop periodically", "Low-Med", "20–30", "Java, DSA, SQL", "Medium"),
    (14, "Microsoft", "Top comp; internship→PPO is the highest-probability door; Java not core but DSA/OOP transfer", "Low", "23–28", "DSA, OOP, SQL, one more language", "Medium"),
    (15, "Zoho", "Highest-probability offer in India for Java freshers (safety floor); ₹4–10L [R]", "High", "4–10", "Core Java, low-level programming, SQL", "Safety"),
]
table(["#", "Company", "Why Target", "Fresher Probability", "Expected LPA", "Skills Needed", "Priority"],
      [list(map(str, row)) for row in shortlist],
      widths=[0.3, 1.15, 2.1, 0.85, 0.8, 1.5, 0.85])

# ================= 13. HIGHEST-SALARY LIST (new) =================
doc.add_page_break()
h1("13. Separate List — Highest-Salary Companies")
body("Companies where a fresher can realistically land India's top pays. Entry is hardest here; these are stretch/ambition "
     "targets, ordered by realistic fresher total compensation.", italic=True, size=9.5)
high_salary = [
    (1, "Atlassian", "~35–45 [E]", "Graduate SWE / intern→PPO only", "Java-first (Jira/Confluence); DSA very heavy"),
    (2, "Uber", "~28–40 [E]", "SWE-1, mostly 1–3 YOE; fresher via referral", "DSA heavy; Java backend services"),
    (3, "Dream11", "22–30 [R] (up to ~43 yr-1 with bonus/stock)", "SDE-1, occasional fresher reqs", "Java/Spring, DSA, SQL"),
    (4, "Zepto", "22–34 [R]", "SDE-1, active fresher hiring", "Java/Spring, SQL, speed"),
    (5, "Amazon", "24–30 [R]", "SDE-1 via off-campus OA", "DSA medium-hard, Java, SQL"),
    (6, "Microsoft", "23–28 [R]", "SWE via campus/intern + rare off-campus", "DSA, OOP, SQL"),
    (7, "Nutanix", "25–32 [E]", "MTS-1, rare fresher intake", "Java/C++, systems, DSA"),
    (8, "Adobe", "22–32 [E]", "MTS-1 via intern→PPO mostly", "DSA, CS fundamentals"),
    (9, "Meesho", "20–30 [R]", "SDE-1, periodic fresher reqs", "Java, DSA, SQL"),
    (10, "PhonePe", "20–28 [R]", "SDE-1, genuine off-campus intake", "Java, Spring, SQL, AWS"),
    (11, "Goldman Sachs", "20–28 [R]", "Engineering Analyst, intern + off-campus", "Java, DSA, SQL, CS fundamentals"),
    (12, "Flipkart", "20–25 [R]", "SDE-1, campus-heavy + Runway/GT", "DSA, Java, Spring, SQL"),
    (13, "Salesforce", "18–28 [E]", "AMTS, intern→PPO", "Java/OOP, DSA"),
    (14, "ServiceNow", "20–28 [E]", "SWE, selective fresher intake", "Java, DSA, SQL"),
    (15, "PayPal", "18–28 [E]", "SDE-1, periodic fresher reqs", "Java, Spring, SQL, DSA"),
]
table(["Rank", "Company", "Realistic Fresher TC (LPA)", "Entry Route", "Skills Needed"],
      [list(map(str, row)) for row in high_salary],
      widths=[0.45, 1.3, 1.75, 2.2, 2.1])
body("Note: HFT/quant firms (DE Shaw, Graviton, Tower Research, Quadeye) pay ₹50L+ but typically require 9+ CGPA plus strong "
     "competitive programming — excluded as unrealistic primary targets for this profile.", italic=True, size=9.5)

# ================= 14. WATCHLIST (new) =================
doc.add_page_break()
h1("14. Separate List — Watchlist Category")
body("Companies worth monitoring weekly: pay is near (but often below) the ₹12 LPA bar, OR Java hiring is strong but fresher "
     "windows open irregularly. Set alerts on official career pages; apply within days of a matching req.", italic=True, size=9.5)
watchlist = [
    (1, "Boeing (BIETC / Digital Services)", "₹9–13 [R: AmbitionBox ASE ₹11–12.7L]", "Associate Software Engineer – Java (Bengaluru); live reqs on jobs.boeing.com (one posted Oct 2, 2026)", "Watch: small, infrequent intake; ~half of India work is C/C++ avionics; move fast when a Java ASE req opens"),
    (2, "Accenture", "₹4.5–6.5 [R: ASE / AASE]", "ASE / AASE mass hiring year-round (indiacampus.accenture.com)", "Safety net only: pay far below target; use as fallback offer or 2–3 yr pitstop before switching at ₹18–30L"),
    (3, "Wells Fargo", "₹12–18 [E]", "India Early Careers (Bengaluru/Hyderabad)", "Watch: structured grad program; Java backend teams exist"),
    (4, "Mastercard", "₹12–18 [E]", "Software Engineer / tech grad roles", "Watch: frequent India reqs, fresher-friendly; Pune hub"),
    (5, "Optum (UHG)", "₹8–13 [E]", "Associate Software Engineer (Java/Spring)", "Watch: upper band crosses ₹12L; large Java org, Hyderabad/Gurgaon"),
    (6, "Media.net", "₹18–22 [R]", "SWE-1, periodic drives", "Watch: high pay but DSA-hard screens; infrequent windows"),
    (7, "Games24x7", "₹20–25 [R]", "SDE-1 (Mumbai), periodic", "Watch: Java/Spring-heavy; strong pay; occasional drives"),
    (8, "Navi", "₹18–25 [E]", "SDE-1, irregular", "Watch: fintech, Java-first; monitor LinkedIn/careers page"),
    (9, "Groww", "₹15–25 [E]", "SDE / backend, periodic", "Watch: fintech scale-up; fresher reqs appear in bursts"),
    (10, "Freshworks", "₹8–14 [E]", "SDE-1 / graduate roles (Chennai)", "Watch: upper band near target; SaaS, decent Java exposure"),
    (11, "Postman / BrowserStack", "₹15–25 [E]", "SDE-1, rare", "Watch: excellent SaaS engineering; mostly 1–3 YOE"),
    (12, "Publicis Sapient / EPAM / Thoughtworks", "₹6–12 [E]", "ASE / graduate engineer tracks", "Watch: digital consultancies with real Java/Spring work; 2026 fresher drives reported"),
]
table(["#", "Company", "Realistic Fresher TC (LPA)", "Entry Route", "Why Watchlist / Action"],
      [list(map(str, row)) for row in watchlist],
      widths=[0.35, 1.5, 1.35, 2.0, 2.6])
body("Watchlist operating rule: create weekly alerts on each company's official careers page (jobs.boeing.com, "
     "careers.americanexpress.com, etc.) plus naukri/LinkedIn saved searches for the company name + 'Java' + 'fresher'. "
     "Boeing and Media.net move the fastest once a req goes live.", italic=True, size=9.5)

# ================= 15. SOURCES =================
h1("15. Sources")
h3("Salary / compensation")
body("levels.fyi (Amazon SDE-I Bengaluru ₹27.97L median; Flipkart SDE-1 ₹22.57L; Dream11 SDE-1 ₹24.65L; Media.net; Microsoft India; "
     "Walmart India; Amex India; Deutsche Bank India; PhonePe; Accenture ASE ₹6.3L median; Boeing) · ambitionbox.com (PhonePe ₹24.7–28.5L; "
     "Zepto SDE-1 ₹22.6–33.8L; Goldman Software Analyst ₹24.9–28.8L; Deutsche Engineer ₹14.4–21L; Amex ₹22–24.3L; Games24x7 ₹20.4–25L; "
     "Zoho ₹9.2–10.1L; Boeing ASE ₹11–12.7L; Accenture ASE 12,000+ reports) · glassdoor.co.in (Deutsche Bank, Wells Fargo, Dream11, Media.net, "
     "Zoho, Boeing ASE) · LeetCode Discuss offer posts (Microsoft L59 Hyderabad 2026 new grad; Dream11 SDE-1) · Reddit r/developersIndia "
     "(Amazon SDE-1 Dec 2025 offer; Accenture ASE 2026 threads) · upgrad.com 2026 salary guides · efinancialcareers.com (Feb 2026 Goldman pay) · "
     "joinsaarthi.com, resumevera.com, instahyre.com, simpliaxis, futurense, scaler 2026 guides · 6figr.com (Boeing engineer pay, Sep 2026).")
h3("Hiring / openings")
body("phonepe.com/careers · razorpay.com/careers · goldmansachs.com/worldwide/india/careers · jpmorganchase.com/careers (SWE Program) · "
     "careers.db.com (Graduate Programme) · wellsfargojobs.com (India Early Careers) · tech.walmart.com/careers · careers.americanexpress.com · "
     "jobs.boeing.com (India IT jobs: Associate Software Engineer + Java, Sep–Oct 2026) · indiacampus.accenture.com (ASE, 0–11 months) · "
     "tcs.com/careers/india/entry-level · careers.cognizant.com (GenC) · infosys.com/careers/graduates.html · qualcomm.com (India internships) · "
     "zoho.com/careers · wellfound.com/role/l/java-backend-developer/india (64 live, Oct 2026) · naukri.com/java-fresher-jobs (11,399 listings) · "
     "internshala.com/fresher-jobs/java-jobs · productbased.in.")
h3("Interview process")
body("LeetCode Discuss company tags · r/developersIndia interview threads (2025–26) · ambitionbox interview sections · "
     "interviewbit / interviewera 2026 Java question banks.")
h3("Caveats")
body("Salary bands marked [E] are estimates triangulated from adjacent data; social-media package claims were excluded unless "
     "cross-checkable. Openings verified live on October 5, 2026 may close at any time — re-confirm on the official page before applying.")

body("")
p = body("Key takeaway: with 8 CGPA and Java + a deployed backend project (ThrottleGate rate limiter is genuinely strong resume material "
         "for this market), the highest-EV path is: Walmart / JPMorgan / Deutsche Bank / Amex / Oracle / Cisco / Razorpay / Zepto volume now "
         "+ Amazon / Microsoft / Goldman Sachs / PhonePe via referrals and DSA depth over the next 6–8 months + an internship application "
         "this season as the PPO backdoor. Track Boeing weekly; treat Accenture as fallback only.", bold=True)

doc.save(OUT)
print("Saved:", OUT)
