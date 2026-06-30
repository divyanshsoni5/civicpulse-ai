🏙️ CivicPulse AI

AI-powered hyperlocal civic issue reporting platform

CivicPulse AI lets citizens report local civic issues — potholes, broken streetlights, garbage, water leaks, drainage problems — in seconds. Every report is automatically analyzed by Google Gemini AI, which categorizes the issue and assigns a severity level without any manual tagging. Citizens can track issue status, upvote pressing problems, and view an AI-generated summary of civic health across their city.


Built for the Vibe2Ship Hackathon 2026 — Problem Statement 2: Community Hero - Hyperlocal Problem Solver




🚀 Features


🔐 Secure Authentication — JWT-based register/login with BCrypt password hashing
🤖 AI Issue Categorization — Gemini AI auto-classifies issues (Pothole, Streetlight, Water Supply, Garbage, Drainage, Noise, Other)
⚠️ AI Severity Detection — Gemini assigns severity (Low / Medium / High / Critical) to help prioritize urgent issues
📍 Hyperlocal Reporting — Issues are tagged with location and geo-coordinates
👍 Community Upvoting — Citizens upvote issues that matter most to them
📊 Impact Dashboard — Live stats: total/open/in-progress/resolved issues, critical count, total upvotes, category breakdown
🧠 AI City Summary — One-click Gemini-generated summary of citywide civic health
🔄 Status Tracking — Issues move through Open → In Progress → Resolved → Closed
🎯 Filterable Feed — Browse issues by status



🛠️ Tech Stack

LayerTechnologyBackendJava 21, Spring Boot 4.1.0PersistenceSpring Data JPA, Hibernate ORMDatabaseMySQL 8.0SecuritySpring Security, JWT (JJWT), BCryptFrontendHTML5, CSS3, Vanilla JavaScriptAIGoogle Gemini API (via Google AI Studio)Build ToolMaven


📂 Project Structure

com.civicpulse
├── config         # Security & CORS configuration
├── controller      # REST API endpoints
├── dto             # Request/response objects
├── model           # JPA entities (User, Issue)
├── repository      # Spring Data JPA repositories
├── security        # JWT utilities & filters
├── service         # Business logic + Gemini AI integration
└── CivicpulseAiApplication.java


⚙️ Setup & Installation

Prerequisites


Java 21
Maven
MySQL 8.0
A Gemini API key from Google AI Studio


1. Clone the repository

bashgit clone https://github.com/divyanshsoni5/civicpulse-ai.git
cd civicpulse-ai

2. Create the database

sqlCREATE DATABASE civicpulse_db;

3. Configure application properties

Copy the example file and fill in your own values:

bashcp src/main/resources/application.properties.example src/main/resources/application.properties

Update with your MySQL credentials, JWT secret, and Gemini API key:

propertiesspring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
jwt.secret=YOUR_LONG_RANDOM_SECRET_KEY
gemini.api.key=YOUR_GEMINI_API_KEY

4. Run the application

bashmvn spring-boot:run

The app starts on http://localhost:8082

5. Open the frontend

Visit http://localhost:8082 in your browser to use the app directly (served from src/main/resources/static).


📡 API Endpoints

Auth

MethodEndpointDescriptionPOST/api/auth/registerRegister a new citizenPOST/api/auth/loginLogin and receive JWT token

Issues

MethodEndpointDescriptionPOST/api/issuesReport a new issue (AI auto-categorizes it)GET/api/issuesGet all issuesGET/api/issues/{id}Get a single issueGET/api/issues/status/{status}Filter issues by statusGET/api/issues/location?q=Search issues by locationGET/api/issues/category/{category}Filter issues by categoryPUT/api/issues/{id}/statusUpdate issue statusPUT/api/issues/{id}/upvoteUpvote an issueGET/api/issues/myGet current user's reported issuesGET/api/issues/statsGet dashboard statisticsGET/api/issues/summaryGet AI-generated city summary


🤖 How the AI Works

When a citizen submits an issue, the backend sends the title and description to the Gemini API with a structured prompt asking it to classify the issue and assess severity. The response is parsed and stored alongside the issue, so every report is intelligently tagged the moment it's created — no manual labeling required.

A separate Gemini-powered endpoint aggregates all open issues and asks Gemini to produce a short, readable summary of citywide civic health, surfacing the most urgent areas for administrators.


👤 Author

Divyansh Soni
SGSITS, Indore


📄 License

This project was built for hackathon submission purposes.
