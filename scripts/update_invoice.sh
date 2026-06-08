curl -u admin:changeme -X POST http://localhost:8080/stays/update-invoice \
  -H "Content-Type: application/json" \
  -d '{
    "confirmationId": "ABC123",
    "invoice": {
      "items": [
        {"type": "Room", "name": "Jade Vine Suite", "quantity": 1, "amount": "150.69", "date": "2026-06-01"},
        {"type": "Room", "name": "Jade Vine Suite", "quantity": 1, "amount": "150.69", "date": "2026-06-02"},
        {"type": "Room", "name": "Jade Vine Suite", "quantity": 1, "amount": "150.69", "date": "2026-06-03"},
        {"type": "Room", "name": "Jade Vine Suite", "quantity": 1, "amount": "150.69", "date": "2026-06-04"},
        {"type": "Mugs", "name": null, "quantity": 1, "amount": "20.42", "date": "2026-06-01"}
      ],
      "stateTax": "9.04",
      "countyTax": "3.01"
    }
  }'
