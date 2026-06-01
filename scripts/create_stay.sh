curl -u admin:changeme -X POST http://localhost:8080/stays/upsert \
  -H "Content-Type: application/json" \
  -d '[
    {
      "externalId": 1001,
      "confirmationCode": "ABC123",
      "primaryGuestName": "Alice Smith",
      "checkIn": "2026-06-01",
      "checkOut": "2026-06-05",
      "additionalGuestName": "Bob Smith",
      "specialAccommodations": "Ground floor room requested",
      "dietaryRestrictions": "Gluten free",
      "arrivalTime": "3:00 PM",
      "housekeepingNotes": "Do not disturb before 9am",
      "reasonForStay": "Anniversary",
      "invoice": {
        "items": [
          {"type": "Room", "name": "Whole Main House", "quantity": 1, "amount": "150.69", "date": "2026-06-01"},
          {"type": "Mugs", "name": null, "quantity": 1, "amount": "20.42", "date": "2026-06-01"}
        ],
        "stateTax": "0.06",
        "countyTax": "0.01"
      },
      "guest": {
        "externalId": 5001,
        "name": "Alice Smith",
        "notes": "Prefers extra towels",
        "phones": [{"number": "555-100-0001"}],
        "emails": [{"address": "alice@example.com"}],
        "addresses": [{"street": "123 Main St", "city": "Springfield", "state": "IL", "zip": "62701", "country": "US"}]
      }
    }
  ]'