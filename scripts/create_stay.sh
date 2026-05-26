curl -X POST http://localhost:8080/stays \
  -H "Content-Type: application/json" \
  -d '{
    "externalId": 1001,
    "primaryGuestName": "Alice Smith",
    "checkIn": "2026-06-01",
    "checkOut": "2026-06-05",
    "invoice": {
      "items": [
        {"name": "Room", "price": "150.69"},
        {"name": "Parking", "price": "20.42"}
      ],
      "stateTax": "0.06",
      "countyTax": "0.01"
    }
  }'

curl -X POST http://localhost:8080/guests \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alice Smith",
    "externalId": 5001,
    "phones": [{"number": "555-100-0001"}],
    "emails": [{"address": "alice@example.com"}],
    "addresses": [{"street": "123 Main St", "city": "Springfield", "state": "IL", "zip": "62701"}]
  }'
