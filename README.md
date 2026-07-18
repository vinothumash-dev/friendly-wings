# Friendly Wings Booking Automation

A Java Spring Boot service that watches a Gmail inbox for flight e-ticket emails (any provider — MakeMyTrip, Cleartrip, IndiGo, etc.), parses the attached PDF with OpenAI, and generates company-branded PDF vouchers.

## What it does

1. **Polls Gmail inbox** every 30 minutes via IMAP (unseen emails received today)
2. **Detects** a ticket email by one simple rule: it carries a PDF attachment
3. **Parses** the e-ticket PDF with OpenAI (provider-agnostic — no per-provider parsers)
4. **Skips** PDFs that don't look like flight tickets (no trips extracted)
5. **Generates** a PDF voucher using Friendly Wings company templates
6. **Emails** the voucher to the configured recipients
7. **Tracks** processed emails in an H2 database to prevent duplicates, and moves them to a `Processed` folder

## Project Structure

```
src/main/java/com/friendlywings/automation/
├── client/              # OpenAI API client
├── controller/          # REST API endpoints
├── config/              # Application configuration
├── model/               # Booking data models
├── parser/              # FlightTicketPdfParser — the single parser for all providers
├── pdf/                 # PDF generators (Velocity templates + Flying Saucer)
├── repository/          # H2 database entity & repository
├── service/             # Gmail IMAP, OpenAI parsing, orchestration
└── tools/               # PdfParserRunner — parse local PDFs from the command line

src/main/resources/
├── assets/
│   ├── logo.png         # Friendly Wings logo
│   └── signature.png    # Proprietor signature stamp
├── makemytrip/          # Sample e-ticket PDFs for testing
└── application.yml      # Config (Gmail creds, OpenAI key, intervals, etc.)
```

## Build

```bash
mvn clean package -DskipTests
```

Produces: `target/friendly-wings-automation-1.0.0.jar`

## Run

```bash
java -jar target/friendly-wings-automation-1.0.0.jar
```

The service starts on **port 8080**.

## Configuration

Edit `src/main/resources/application.yml` (or pass as env vars):

```yaml
friendlywings:
  gmail:
    username: friendlywings123@gmail.com
    password: ${GMAIL_APP_PASSWORD}   # Gmail App Password (NOT your Gmail password)
    poll-interval-minutes: 30
    folder: INBOX
    move-processed: true              # Move processed emails to "Processed" folder
  openai:
    api-key: ${OPENAI_API_KEY}        # Used to parse e-ticket PDFs
    model: gpt-5.5
  output:
    directory: ./output
```

### Gmail App Password Setup
1. Go to https://myaccount.google.com/apppasswords
2. Enable 2-Step Verification if not already enabled
3. Generate an App Password for "Mail"
4. Use that 16-character password in the config

## How ticket emails are found

- **Automatic**: the scheduler polls the inbox every `poll-interval-minutes` for today's unseen emails. Any email with a PDF attachment is parsed; if OpenAI extracts at least one trip, a voucher is generated and emailed. Everything is recorded in `/api/status` (`SUCCESS` / `SKIPPED` / `PARSE_ERROR`).
- **Manual**: `POST /api/process` scans the inbox now, `POST /api/search?subject=...` processes matching emails on demand.

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/process` | POST | Manually trigger email processing |
| `/api/search?subject=X` | POST | Search inbox by subject and process |
| `/api/status` | GET | List all processed emails |
| `/api/debug/test-pdf-parser` | POST | Upload a PDF (`file` param), get parsed JSON back |
| `/api/demo/flight` | POST | Generate a sample flight PDF (no email needed) |
| `/api/demo/hotel` | POST | Generate a sample hotel PDF (no email needed) |
| `/h2-console` | GET | H2 database console (default creds: sa / empty) |

## Parsing local PDFs without Gmail

```bash
mvn compile dependency:build-classpath -Dmdep.outputFile=target/cp.txt
java -cp "target/classes:$(cat target/cp.txt)" com.friendlywings.automation.tools.PdfParserRunner [pdf-directory]
```

Prints the parsed `FlightItinerary` JSON for every PDF in the directory (default: `src/main/resources/makemytrip`).

## Output

Generated PDFs are saved to `./output/`:
- `FW_FLIGHT_<tripId>.pdf`
- `FW_HOTEL_<bookingRef>.pdf`

## Tech Stack

- Java 17+, Spring Boot 3.x
- OpenAI API (e-ticket PDF parsing)
- Apache PDFBox (PDF text extraction)
- Velocity + Flying Saucer/OpenPDF (voucher PDF generation)
- JavaMail / Angus Mail (Gmail IMAP)
- H2 Database (tracking processed emails)
- Maven
