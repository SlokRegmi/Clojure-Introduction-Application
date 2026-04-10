# Test Banking Application API Documentation

A professional test banking application built with Clojure that supports account management and payment transactions between users.

## Features

- ✅ Account Management (Create, View, Close accounts)
- ✅ Multiple account types (Checking, Savings)
- ✅ Money Transfers between accounts
- ✅ Deposits and Withdrawals
- ✅ Transaction History
- ✅ Balance Tracking
- ✅ Account Status Management
- ✅ Comprehensive validation and error handling

## Getting Started

### Run the Application

```bash
lein run
```

The server will start on `http://localhost:3000`

### Sample Data

The application comes pre-loaded with sample data:
- 4 accounts for 3 users
- Initial balances ranging from $3,500 to $10,000
- Sample transactions demonstrating transfers, deposits, and withdrawals

## API Endpoints

### Account Management

#### List All Accounts
```
GET /api/accounts
```

Response:
```json
[
  {
    "id": 1,
    "user-id": 1,
    "account-type": "checking",
    "balance": 4500.00,
    "status": "active",
    "created-at": "2026-04-08T15:14:31.000Z",
    "updated-at": "2026-04-08T15:14:31.000Z"
  }
]
```

#### Get User's Accounts
```
GET /api/users/:user-id/accounts
```

#### Get Single Account
```
GET /api/accounts/:id
```

#### Get Account Balance
```
GET /api/accounts/:id/balance
```

Response:
```json
{
  "account-id": 1,
  "balance": 4500.00
}
```

#### Create New Account
```
POST /api/accounts
Content-Type: application/json

{
  "user-id": 1,
  "account-type": "checking",
  "initial-balance": 1000.00
}
```

#### Close Account
```
PUT /api/accounts/:id/close
```

Note: Accounts can only be closed if balance is zero.

### Transaction Management

#### List All Transactions
```
GET /api/transactions
```

#### Get Account Transactions
```
GET /api/accounts/:id/transactions
```

#### Get Single Transaction
```
GET /api/transactions/:id
```

#### Transfer Money
```
POST /api/transactions/transfer
Content-Type: application/json

{
  "from-account-id": 1,
  "to-account-id": 3,
  "amount": 500.00,
  "description": "Payment for services"
}
```

Response:
```json
{
  "success": true,
  "transaction": {
    "id": 1,
    "type": "transfer",
    "from-account": 1,
    "to-account": 3,
    "amount": 500.00,
    "description": "Payment for services",
    "status": "completed",
    "timestamp": "2026-04-08T15:14:31.000Z"
  },
  "from-balance": 4500.00,
  "to-balance": 8000.00
}
```

#### Deposit Money
```
POST /api/transactions/deposit
Content-Type: application/json

{
  "account-id": 1,
  "amount": 1000.00,
  "description": "Salary deposit"
}
```

#### Withdraw Money
```
POST /api/transactions/withdraw
Content-Type: application/json

{
  "account-id": 1,
  "amount": 250.00,
  "description": "ATM withdrawal"
}
```

## User Management

The application also includes user management endpoints:

#### List All Users
```
GET /api/users
```

#### Get Single User
```
GET /api/users/:id
```

#### Create User
```
POST /api/users
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "role": "user"
}
```

#### Update User
```
PUT /api/users/:id
Content-Type: application/json

{
  "name": "John Updated",
  "email": "john.new@example.com"
}
```

#### Delete User
```
DELETE /api/users/:id
```

## Example Usage with cURL

### Create a new account
```bash
curl -X POST http://localhost:3000/api/accounts \
  -H "Content-Type: application/json" \
  -d '{"user-id": 1, "account-type": "savings", "initial-balance": 5000}'
```

### Transfer money between accounts
```bash
curl -X POST http://localhost:3000/api/transactions/transfer \
  -H "Content-Type: application/json" \
  -d '{"from-account-id": 1, "to-account-id": 2, "amount": 250.50, "description": "Loan repayment"}'
```

### Check account balance
```bash
curl http://localhost:3000/api/accounts/1/balance
```

### View transaction history
```bash
curl http://localhost:3000/api/accounts/1/transactions
```

## Validation Rules

- **Transfers**: 
  - Both accounts must exist and be active
  - Amount must be greater than zero
  - Source account must have sufficient funds
  
- **Deposits**: 
  - Account must exist and be active
  - Amount must be greater than zero
  
- **Withdrawals**: 
  - Account must exist and be active
  - Amount must be greater than zero
  - Account must have sufficient funds
  
- **Account Closure**: 
  - Account balance must be zero

## Error Responses

All errors return appropriate HTTP status codes with error messages:

```json
{
  "success": false,
  "error": "Insufficient funds"
}
```

Common status codes:
- `200` - Success
- `201` - Created
- `400` - Bad Request (validation error)
- `404` - Not Found

## Architecture

- **Language**: Clojure 1.12.2
- **Web Framework**: Ring + Compojure
- **Data Storage**: In-memory atoms (for demo purposes)
- **JSON Handling**: Cheshire
- **Date/Time**: clj-time

## Future Enhancements

- Persistent database storage (PostgreSQL, MongoDB)
- User authentication and authorization
- Transaction reversal/cancellation
- Interest calculation for savings accounts
- Account statements and reporting
- Transaction limits and daily caps
- Multi-currency support
- Audit logging
