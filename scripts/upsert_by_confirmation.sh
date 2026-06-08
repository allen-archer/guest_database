curl -u admin:changeme -X POST http://localhost:8080/stays/upsert-by-confirmation \
  -H "Content-Type: application/json" \
  -d '{
    "confirmationCode": "ABC123",
    "primaryGuestName": "Alice Smith",
    "checkIn": "2026-06-01",
    "checkOut": "2026-06-05",
    "invoice": {
      "items": [
        {"type": "Room", "name": "Jade Vine Suite", "quantity": 1, "amount": "150.69", "date": "2026-06-01"},
        {"type": "Room", "name": "Jade Vine Suite", "quantity": 1, "amount": "150.69", "date": "2026-06-02"},
        {"type": "Room", "name": "Jade Vine Suite", "quantity": 1, "amount": "150.69", "date": "2026-06-03"},
        {"type": "Room", "name": "Jade Vine Suite", "quantity": 1, "amount": "150.69", "date": "2026-06-04"}
      ],
      "stateTax": "9.04",
      "countyTax": "3.01"
    }
  }'