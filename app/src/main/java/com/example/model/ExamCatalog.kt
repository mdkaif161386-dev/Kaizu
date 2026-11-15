package com.example.model

data class CompetitiveExam(
    val id: String,
    val name: String,
    val subtitle: String,
    val category: String,
    val subjects: List<ExamSubject>
)

data class ExamSubject(
    val id: String,
    val name: String,
    val tagline: String,
    val chapters: List<String>
)

enum class QuizDifficulty(
    val id: String,
    val label: String,
    val description: String,
    val scoreMultiplier: Double
) {
    EASY("EASY", "Foundation", "Core concepts & direct NCERT/syllabus MCQs", 1.0),
    MODERATE("MODERATE", "Standard Exam", "Actual exam-pattern conceptual & numerical MCQs", 1.35),
    HARD("HARD", "Ranker Level", "Multi-concept, tricky assertion & trap questions", 1.75),
    EXTREME("EXTREME", "Extreme / Olympiad", "Top-1% rank-decider hard problems with deep traps", 2.25);

    companion object {
        fun fromId(id: String): QuizDifficulty =
            entries.firstOrNull { it.id == id } ?: MODERATE
    }
}

object ExamCatalog {
    val exams: List<CompetitiveExam> = listOf(
        CompetitiveExam(
            id = "NEET",
            name = "NEET UG",
            subtitle = "National Eligibility cum Entrance Test (Medical)",
            category = "Medical",
            subjects = listOf(
                ExamSubject(
                    id = "neet_physics",
                    name = "Physics",
                    tagline = "Mechanics, Electrodynamics, Optics & Modern Physics",
                    chapters = listOf(
                        "Units, Measurements & Vectors",
                        "Kinematics & Motion in a Plane",
                        "Laws of Motion & Friction",
                        "Work, Energy & Power",
                        "System of Particles & Rotational Motion",
                        "Gravitation",
                        "Properties of Bulk Matter & Fluids",
                        "Thermodynamics & Kinetic Theory",
                        "Oscillations & Waves",
                        "Electrostatics & Capacitance",
                        "Current Electricity",
                        "Moving Charges, Magnetism & EMI",
                        "Alternating Current & EM Waves",
                        "Ray & Wave Optics",
                        "Dual Nature, Atoms & Nuclei",
                        "Semiconductor Electronics"
                    )
                ),
                ExamSubject(
                    id = "neet_chemistry",
                    name = "Chemistry",
                    tagline = "Physical, Inorganic & Organic Chemistry",
                    chapters = listOf(
                        "Some Basic Concepts & Mole Concept",
                        "Atomic Structure",
                        "Classification & Periodicity",
                        "Chemical Bonding & Molecular Structure",
                        "Chemical Thermodynamics",
                        "Chemical & Ionic Equilibrium",
                        "Redox Reactions & Electrochemistry",
                        "Solutions & Chemical Kinetics",
                        "Coordination Compounds",
                        "p-Block, d & f-Block Elements",
                        "GOC, Isomerism & Hydrocarbons",
                        "Haloalkanes, Alcohols, Phenols & Ethers",
                        "Aldehydes, Ketones & Carboxylic Acids",
                        "Amines & Biomolecules"
                    )
                ),
                ExamSubject(
                    id = "neet_biology",
                    name = "Biology",
                    tagline = "Botany & Zoology (NCERT Mastery)",
                    chapters = listOf(
                        "Biological Classification & Plant Kingdom",
                        "Animal Kingdom",
                        "Morphology & Anatomy of Flowering Plants",
                        "Cell: The Unit of Life & Cell Division",
                        "Biomolecules",
                        "Photosynthesis & Respiration in Plants",
                        "Plant Growth & Regulators",
                        "Breathing, Body Fluids & Circulation",
                        "Excretory, Locomotion & Neural Control",
                        "Chemical Coordination & Integration",
                        "Human Reproduction & Reproductive Health",
                        "Principles of Inheritance & Molecular Basis",
                        "Evolution & Human Health and Disease",
                        "Biotechnology: Principles & Applications",
                        "Organisms, Populations & Ecosystem"
                    )
                )
            )
        ),
        CompetitiveExam(
            id = "JEE",
            name = "JEE Main & Adv",
            subtitle = "Joint Entrance Examination (Engineering)",
            category = "Engineering",
            subjects = listOf(
                ExamSubject(
                    id = "jee_physics",
                    name = "Physics",
                    tagline = "Analytical Mechanics, Electromagnetism & Modern Physics",
                    chapters = listOf(
                        "Kinematics & Relative Motion",
                        "Newton's Laws & Circular Dynamics",
                        "Work, Energy & Conservation Laws",
                        "Center of Mass & Rotational Dynamics",
                        "Gravitation & Fluid Mechanics",
                        "Heat, Thermodynamics & KTG",
                        "SHM, Waves & Sound",
                        "Electrostatics & Gauss's Law",
                        "Current Electricity & Circuits",
                        "Magnetism, EMI & AC Circuits",
                        "Geometrical & Wave Optics",
                        "Modern Physics & Nuclear Reactions"
                    )
                ),
                ExamSubject(
                    id = "jee_chemistry",
                    name = "Chemistry",
                    tagline = "Physical Numericals, Mechanisms & Inorganic",
                    chapters = listOf(
                        "Mole Concept & Stoichiometry",
                        "Atomic Structure & Quantum Numbers",
                        "Chemical Bonding & VSEPR/MOT",
                        "Thermodynamics & Thermochemistry",
                        "Chemical & Ionic Equilibrium",
                        "Electrochemistry & Chemical Kinetics",
                        "Solutions & Colligative Properties",
                        "Coordination Chemistry & Metallurgy",
                        "d & f-Block & Qualitative Analysis",
                        "General Organic Chemistry & Stereochemistry",
                        "Hydrocarbons, Carbonyls & Named Reactions",
                        "Amines, Biomolecules & Polymers"
                    )
                ),
                ExamSubject(
                    id = "jee_maths",
                    name = "Mathematics",
                    tagline = "Calculus, Algebra, Coordinate Geometry & Vectors",
                    chapters = listOf(
                        "Quadratic Equations & Complex Numbers",
                        "Sequences, Series & Binomial Theorem",
                        "Permutations, Combinations & Probability",
                        "Matrices & Determinants",
                        "Trigonometry & Inverse Trig Functions",
                        "Straight Lines & Circles",
                        "Conic Sections (Parabola, Ellipse, Hyperbola)",
                        "Limits, Continuity & Differentiability",
                        "Application of Derivatives",
                        "Indefinite & Definite Integration",
                        "Differential Equations",
                        "Vectors & 3D Geometry"
                    )
                )
            )
        ),
        CompetitiveExam(
            id = "UPSC",
            name = "UPSC CSE",
            subtitle = "Civil Services Prelims (GS + CSAT)",
            category = "Civil Services",
            subjects = listOf(
                ExamSubject(
                    id = "upsc_polity",
                    name = "Indian Polity & Governance",
                    tagline = "Constitution, Parliament, Judiciary & Federalism",
                    chapters = listOf(
                        "Historical Underpinnings & Preamble",
                        "Fundamental Rights, DPSP & Duties",
                        "Parliament & State Legislatures",
                        "President, PM, Governor & Council",
                        "Supreme Court, High Courts & Judicial Review",
                        "Panchayati Raj & Urban Local Bodies",
                        "Constitutional & Statutory Bodies"
                    )
                ),
                ExamSubject(
                    id = "upsc_history",
                    name = "History & Culture",
                    tagline = "Ancient, Medieval, Modern Freedom Struggle & Art",
                    chapters = listOf(
                        "Indus Valley, Vedic & Mauryan Empire",
                        "Gupta Period, Bhakti & Sufi Movements",
                        "Indian Architecture, Dance & Painting",
                        "1857 Revolt & Social Reform Movements",
                        "Gandhian Era & Indian National Movement",
                        "Post-Independence Consolidation"
                    )
                ),
                ExamSubject(
                    id = "upsc_geo_eco",
                    name = "Geography, Ecology & Economy",
                    tagline = "Physical Geo, Climate, Biodiversity & Macroeconomics",
                    chapters = listOf(
                        "Geomorphology, Climatology & Oceanography",
                        "Indian Monsoon, Rivers & Soils",
                        "Biodiversity, Protected Areas & Climate Treaties",
                        "Monetary Policy, RBI & Banking Reforms",
                        "Fiscal Policy, Budget & Taxation",
                        "External Sector, BoP & International Orgs"
                    )
                ),
                ExamSubject(
                    id = "upsc_csat",
                    name = "CSAT & Science Tech",
                    tagline = "Logical Reasoning, Comprehension & Emerging Tech",
                    chapters = listOf(
                        "Reading Comprehension & Inference",
                        "Quantitative Numeracy & Data Sufficiency",
                        "Logical & Analytical Reasoning",
                        "Space Tech, Defence & Biotechnology",
                        "AI, Quantum Computing & Current Sci-Tech"
                    )
                )
            )
        ),
        CompetitiveExam(
            id = "SSC_CGL",
            name = "SSC CGL",
            subtitle = "Staff Selection Commission Tier I & II",
            category = "Government",
            subjects = listOf(
                ExamSubject(
                    id = "ssc_quant",
                    name = "Quantitative Aptitude",
                    tagline = "Arithmetic, Advance Maths & Speed Calculation",
                    chapters = listOf(
                        "Percentage, Profit & Loss & Discount",
                        "Ratio, Proportion, Partnership & Mixture",
                        "Time, Work, Pipes & Cisterns",
                        "Time, Speed, Distance & Boats",
                        "Simple & Compound Interest",
                        "Number System, HCF & LCM",
                        "Algebra & Surds/Indices",
                        "Geometry, Mensuration & Trigonometry"
                    )
                ),
                ExamSubject(
                    id = "ssc_reasoning",
                    name = "General Intelligence & Reasoning",
                    tagline = "Verbal, Non-Verbal, Puzzles & Series",
                    chapters = listOf(
                        "Analogy, Classification & Coding-Decoding",
                        "Number & Alphabetical Series",
                        "Syllogism & Venn Diagrams",
                        "Blood Relations & Direction Sense",
                        "Sitting Arrangement & Ranking",
                        "Mirror/Water Images, Dice & Paper Folding"
                    )
                ),
                ExamSubject(
                    id = "ssc_english",
                    name = "English Comprehension",
                    tagline = "Grammar, Vocabulary, Cloze Test & Idioms",
                    chapters = listOf(
                        "Spot the Error & Sentence Improvement",
                        "Active-Passive Voice & Direct-Indirect",
                        "Synonyms, Antonyms & One-Word Substitution",
                        "Idioms & Phrases",
                        "Cloze Test & Para Jumbles"
                    )
                ),
                ExamSubject(
                    id = "ssc_ga",
                    name = "General Awareness",
                    tagline = "Static GK, Polity, History, Science & Current Affairs",
                    chapters = listOf(
                        "Indian History & Culture",
                        "Indian Polity & Constitution",
                        "Geography & Static GK",
                        "Everyday Physics, Chemistry & Biology",
                        "Economics & Recent Awards/Sports"
                    )
                )
            )
        ),
        CompetitiveExam(
            id = "GATE",
            name = "GATE CS & IT",
            subtitle = "Graduate Aptitude Test in Engineering",
            category = "Postgraduate Tech",
            subjects = listOf(
                ExamSubject(
                    id = "gate_math",
                    name = "Engineering & Discrete Maths",
                    tagline = "Linear Algebra, Calculus, Probability & Logic",
                    chapters = listOf(
                        "Propositional & First-Order Logic",
                        "Sets, Relations, Functions & Lattices",
                        "Graph Theory & Combinatorics",
                        "Linear Algebra (Eigenvalues & Matrices)",
                        "Calculus & Probability Distributions"
                    )
                ),
                ExamSubject(
                    id = "gate_algo",
                    name = "Algorithms & Data Structures",
                    tagline = "Asymptotic Analysis, Trees, Graphs & DP",
                    chapters = listOf(
                        "Asymptotic Complexity & Recurrences",
                        "Stacks, Queues, Linked Lists & Hashing",
                        "Trees, BST, AVL & Heaps",
                        "Graph Traversals, Shortest Paths & MST",
                        "Greedy Algorithms & Dynamic Programming"
                    )
                ),
                ExamSubject(
                    id = "gate_sys",
                    name = "OS, DBMS & Computer Networks",
                    tagline = "Core Systems, Concurrency, SQL & TCP/IP",
                    chapters = listOf(
                        "Process Scheduling, Deadlocks & Semaphores",
                        "Memory Management, Paging & Virtual Memory",
                        "ER Model, Normalization & Transactions",
                        "SQL, Relational Algebra & B+ Trees",
                        "IPv4/Subnetting, TCP/UDP & Congestion Control"
                    )
                ),
                ExamSubject(
                    id = "gate_toc_coa",
                    name = "TOC, Compilers & Digital Logic",
                    tagline = "Automata, Parsing, Pipelining & Boolean Algebra",
                    chapters = listOf(
                        "Digital Logic, K-Maps & Sequential Circuits",
                        "COA: Pipelining, Cache & Memory Hierarchy",
                        "DFA, NFA, Regular Languages & CFG",
                        "Turing Machines & Undecidability",
                        "Lexical Analysis, Parsing & Code Optimization"
                    )
                )
            )
        ),
        CompetitiveExam(
            id = "CAT",
            name = "CAT",
            subtitle = "Common Admission Test (IIM / MBA)",
            category = "Management",
            subjects = listOf(
                ExamSubject(
                    id = "cat_qa",
                    name = "Quantitative Ability",
                    tagline = "Arithmetic, Algebra, Number Theory & Geometry",
                    chapters = listOf(
                        "Percentages, Profit-Loss & Averages",
                        "Time-Speed-Distance & Time-Work",
                        "Linear & Quadratic Equations, Inequalities",
                        "Logarithms, Functions & Progressions",
                        "Number System, Remainders & Divisibility",
                        "Geometry, Mensuration & Coordinate Geo",
                        "Permutation, Combination & Probability"
                    )
                ),
                ExamSubject(
                    id = "cat_dilr",
                    name = "Data Interpretation & LR",
                    tagline = "Caselets, Games, Tournaments & Seating Logic",
                    chapters = listOf(
                        "Charts, Tables & Data Sufficiency",
                        "Games & Tournaments Scheduling",
                        "Linear & Circular Arrangements",
                        "Set Theory & Multi-variable Venn Diagrams",
                        "Routes, Networks & Binary Logic"
                    )
                ),
                ExamSubject(
                    id = "cat_varc",
                    name = "Verbal Ability & RC",
                    tagline = "Critical Reading, Para-Summary & Odd-One-Out",
                    chapters = listOf(
                        "Reading Comprehension (Philosophy & Economics)",
                        "Para Jumbles (TITA & MCQ)",
                        "Para Summary & Essence",
                        "Odd Sentence Out & Inference"
                    )
                )
            )
        ),
        CompetitiveExam(
            id = "CLAT",
            name = "CLAT",
            subtitle = "Common Law Admission Test (NLU)",
            category = "Law",
            subjects = listOf(
                ExamSubject(
                    id = "clat_legal",
                    name = "Legal Reasoning",
                    tagline = "Torts, Contracts, Criminal & Constitutional Law",
                    chapters = listOf(
                        "Law of Torts & Strict Liability",
                        "Indian Contract Act Principles",
                        "Criminal Law (BNS / IPC Principles)",
                        "Constitutional Law & Landmark Judgments",
                        "Intellectual Property & Family Law"
                    )
                ),
                ExamSubject(
                    id = "clat_logical",
                    name = "Logical & Quantitative",
                    tagline = "Critical Reasoning, Syllogisms & Legal Math",
                    chapters = listOf(
                        "Strengthening & Weakening Arguments",
                        "Assumptions, Inferences & Paradoxes",
                        "Analytical Puzzles & Syllogisms",
                        "Ratio, Percentage & Data Caselets"
                    )
                ),
                ExamSubject(
                    id = "clat_gk",
                    name = "Current Affairs & English",
                    tagline = "National/Global Policy, Legal Updates & RC",
                    chapters = listOf(
                        "Constitutional Amendments & Bills",
                        "International Organizations & Summits",
                        "Passage-based Reading Comprehension",
                        "Legal Vocabulary & Contextual Usage"
                    )
                )
            )
        ),
        CompetitiveExam(
            id = "BANKING",
            name = "IBPS / SBI PO",
            subtitle = "Banking Probationary Officer Prelims & Mains",
            category = "Banking",
            subjects = listOf(
                ExamSubject(
                    id = "bank_quant",
                    name = "Quantitative Aptitude",
                    tagline = "DI Caselets, Simplification & Arithmetic",
                    chapters = listOf(
                        "Approximation, Simplification & Number Series",
                        "Quadratic Inequalities",
                        "Tabular, Pie & Radar Data Interpretation",
                        "Arithmetic Word Problems (Profit, SI/CI, Boats)",
                        "Data Sufficiency & Quantity Comparison"
                    )
                ),
                ExamSubject(
                    id = "bank_reasoning",
                    name = "Reasoning Ability",
                    tagline = "Floor Puzzles, Inequalities, Input-Output & Syllogism",
                    chapters = listOf(
                        "Floor, Box & Scheduling Puzzles",
                        "Circular & Parallel Row Seating",
                        "Coded Inequalities & Syllogisms",
                        "Machine Input-Output & Coding",
                        "Course of Action & Critical Reasoning"
                    )
                ),
                ExamSubject(
                    id = "bank_awareness",
                    name = "English & Banking Awareness",
                    tagline = "RBI Policy, Financial Markets & RC",
                    chapters = listOf(
                        "RBI Monetary Policy, Repo/CRR & Basel Norms",
                        "Digital Banking, UPI & Financial Inclusion",
                        "Reading Comprehension & Error Spotting",
                        "Cloze Test, Fillers & Connectors"
                    )
                )
            )
        )
    )

    fun getExamById(examId: String): CompetitiveExam =
        exams.firstOrNull { it.id == examId } ?: exams.first()
}
