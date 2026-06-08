curl -u admin:changeme -X POST http://localhost:8080/stays/cancel \
  -H "Content-Type: application/json" \
  -d '[
    {"room": "Jade Vine Suite", "date": "2026-06-01"},
    {"room": "Jade Vine Suite", "date": "2026-06-02"},
    {"room": "Jade Vine Suite", "date": "2026-06-03"},
    {"room": "Jade Vine Suite", "date": "2026-06-04"}
  ]'