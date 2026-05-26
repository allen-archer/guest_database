curl -X POST http://localhost:8080/stays \
  -H "Content-Type: application/json" \
  -d '{
    "externalId": 1002,
    "primaryGuestName": "Bob Jones",
    "checkIn": "2026-07-01",
    "checkOut": "2026-07-05",
    "invoice": {
      "items": [
        {"name": "Room", "price": "150.69"},
        {"name": "Parking", "price": "20.42"}
      ],
      "stateTax": "0.06",
      "countyTax": "0.01"
    }
  }'
