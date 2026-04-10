# test-app

Test banking application built with Clojure, Ring, and SQLite for testing and learning purposes.

## Features

- User management (create, list, update, delete)
- Account management with persistent balances
- Deposit and withdraw operations
- Transfers in two modes:
  - instant (balance moves immediately)
  - request (receiver owner or admin approves/rejects)
- Account history ledger
- SQLite persistence (data survives server restart)

## Run

From the project root:

    lein run

Server starts at:

    http://localhost:3000

## Database

- Default SQLite file: `data/test-app.sqlite`
- Override path with JVM property:

    lein run -Dtest-app.db.path=target/custom.sqlite

- Or with environment variable:

    TEST_APP_DB_PATH=target/custom.sqlite

## Key API Endpoints

- GET /api/users
- POST /api/users
- GET /api/accounts
- POST /api/accounts
- POST /api/transactions/deposit
- POST /api/transactions/withdraw
- POST /api/transactions/transfer
- GET /api/transactions/pending
- POST /api/transactions/:id/approve
- POST /api/transactions/:id/reject
- GET /api/accounts/:id/history

## Tests

Run all tests:

    lein test

## License

Copyright © 2026

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
https://www.eclipse.org/legal/epl-2.0.
