curl -X POST http://localhost:8080/stays \
  -H "Content-Type: application/json" \
  -d '{
    "externalId": 1002,
    "primaryGuestName": "Bob Jones",
    "checkIn": "2026-07-01",
    "checkOut": "2026-07-05",
    "invoice": {
      "items": [
        {"type": "Room", "name": "Jade Vine Suite", "quantity": 1, "amount": "150.69", "date": "2026-07-01"},
        {"type": "Mugs", "quantity": 1, "amount": "20.42", "date": "2026-07-01"}
      ],
      "stateTax": "0.06",
      "countyTax": "0.01"
    }
  }'
