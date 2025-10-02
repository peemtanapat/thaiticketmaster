-- Initial categories for Ticketmaster application
-- Categories: concerts, shows, sports, exhibitions

INSERT INTO categories (name, description, created_at, updated_at) 
VALUES 
  ('Concerts', 'Live music performances and concerts', NOW(), NOW()),
  ('Shows', 'Theater, comedy shows, and entertainment performances', NOW(), NOW()),
  ('Sports', 'Sporting events and competitions', NOW(), NOW()),
  ('Exhibitions', 'Art exhibitions, trade shows, and expos', NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- Initial events
INSERT INTO events (name, category_id, location, on_sale_datetime, ticket_price, detail, event_status, gate_open, created_at, updated_at)
VALUES 
  -- 1. ONE Fight Night 36
  ('ONE Fight Night 36 : Prajanchai vs. Di Bella ll', 
   (SELECT id FROM categories WHERE name = 'Sports'), 
   'LUMPINEE BOXING STADIUM (RAMINTRA ROAD)', 
   '2025-08-13 19:00:00', 
   10000.00, 
   'Ticket Price: Premium Ringside 10,000 THB (No F&B Service), Ringside 4,500 THB, Cat 1 3,750 THB, Cat 2 2,400 THB, Cat 3 1,200 THB. Early Bird Sale: Wednesday 13 August 2025, 07:00 p.m.', 
   'ON_SALE', 
   '07.00 a.m.', 
   NOW(), 
   NOW()),
  
  -- 2. MARIAH CAREY The Celebration of Mimi
  ('MARIAH CAREY The Celebration of Mimi', 
   (SELECT id FROM categories WHERE name = 'Concerts'), 
   'IMPACT CHALLENGER HALL , MUANG THONG THANI', 
   '2025-07-19 10:00:00', 
   20000.00, 
   'Ticket Price: 20,000 / 15,000 / 12,000 / 9,000 / 7,500 / 6,500 / 5,000 / 3,500 THB. Pre Sale: Friday 18 July 2025, 10:00 a.m. - 11:59 p.m. Public Sale: Saturday 19 July 2025, 10:00', 
   'ON_SALE', 
   '06.00 p.m.', 
   NOW(), 
   NOW()),
  
  -- 3. KABUKI Otokodate Hana No Yoshiwara
  ('KABUKI Otokodate Hana No Yoshiwara By Ichikawa Danjuro XIII in Bangkok 2025', 
   (SELECT id FROM categories WHERE name = 'Shows'), 
   'Muangthai Rachadalai Theatre', 
   '2025-09-06 10:00:00', 
   5000.00, 
   'Ticket Price: 5,000 / 4,000 / 3,000 / 2,000 THB. Public Sale: Saturday 6 September 2025, 10:00', 
   'ON_SALE', 
   'Approx. 1 hr. before show start.', 
   NOW(), 
   NOW());

-- Event show times
INSERT INTO event_show_times (event_id, show_datetime)
VALUES 
  -- ONE Fight Night 36 (show date: Saturday 4 October 2025)
  ((SELECT id FROM events WHERE name = 'ONE Fight Night 36 : Prajanchai vs. Di Bella ll'), '2025-10-04 19:00:00'),
  
  -- MARIAH CAREY (show date: Saturday 11 October 2025)
  ((SELECT id FROM events WHERE name = 'MARIAH CAREY The Celebration of Mimi'), '2025-10-11 18:00:00'),
  
  -- KABUKI (show dates: Saturday 13 December 2025 - Sunday 14 December 2025)
  ((SELECT id FROM events WHERE name = 'KABUKI Otokodate Hana No Yoshiwara By Ichikawa Danjuro XIII in Bangkok 2025'), '2025-12-13 19:00:00'),
  ((SELECT id FROM events WHERE name = 'KABUKI Otokodate Hana No Yoshiwara By Ichikawa Danjuro XIII in Bangkok 2025'), '2025-12-14 19:00:00');
